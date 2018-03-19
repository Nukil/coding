package com.netposa.poseidon.face.service;

import com.netposa.HbaseUtil;
import com.netposa.poseidon.face.bean.*;
import com.netposa.poseidon.face.rpc.outrpc.SearchImgByImgInputRecord;
import com.netposa.poseidon.face.rpc.inrpc.SearchImgByImgResponse;
import com.netposa.poseidon.face.rpc.outrpc.SearchImgResult;
import com.netposa.poseidon.face.util.Byte2FloatUtil;
import com.netposa.poseidon.face.util.FaceFeatureVerify;
import com.netposa.poseidon.face.util.LoadPropers;
import org.apache.hadoop.hbase.client.Result;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SearchFaceImgByImgService {
    private static Logger logger = Logger.getLogger(SearchFaceImgByImgService.class);
    private static Properties properties = LoadPropers.getSingleInstance().getProperties("server");
    // 创建线程池，每个线程调用一个server上的rpc进行以图搜图
    private static ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    private static RpcManagerService rpcManagerService = RpcManagerService.getInstance();
    private static int floatSize = 4;
    private static int featRawDim = Integer.parseInt(properties.getProperty("face.feature.size").trim()) / floatSize;
    private static long startTime = 0;
    private static int cacheDays = Integer.parseInt(properties.getProperty("duration.time", "0").trim());
    private static String tableName = properties.getProperty("hbase.table.name").trim();
    private static int regionNum = Integer.parseInt(properties.getProperty("hbase.split.region.num", "50").trim());
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private static boolean debug = Boolean.parseBoolean(properties.getProperty("service.process.debug.enable", "false"));

    private static byte[] FAMILY = "cf".getBytes();
    private static byte[] CAMERAID = "cameraId".getBytes();
    private static byte[] JLBH = "logNum".getBytes();
    private static byte[] FEATURE = "feature".getBytes();
    private static byte[] GATHERTIME = "gatherTime".getBytes();

    static {
        try {
            startTime = sdf.parse(properties.getProperty("start.time").trim()).getTime();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private class SearchImgByImgThread implements Callable<SearchImgByImgResponse> {
        // 用于存放
        ConnectionManagerKey key;
        // master-server连接
        ClientStatus cs;
        // master-server以图搜图输入条件
        com.netposa.poseidon.face.rpc.inrpc.SearchImgByImgInputRecord input;
        // 唯一标识uuid
        String uuid;

        SearchImgByImgThread(ConnectionManagerKey key, com.netposa.poseidon.face.rpc.outrpc.SearchImgByImgInputRecord inputRecord, String uuid) {
            List<Double> feature = new ArrayList<>();
            byte[] srcFeature = inputRecord.getFeature();
            this.key = key;
            // 获取一个可用的连接
            AvailableConnectionBean acb = rpcManagerService.getAvailableConnection(key, false);
            if (null != acb) {
                this.cs = acb.getCs();
            }
            // 将原特征转换为float数组
            for (int i = 0; i < featRawDim; i++) {
                feature.add((double) Byte2FloatUtil.byte2float(srcFeature, i * floatSize));
            }
            this.input = new com.netposa.poseidon.face.rpc.inrpc.SearchImgByImgInputRecord(
                    inputRecord.getStartTime(),
                    inputRecord.getEndTime(),
                    inputRecord.getCameraId(),
                    feature,
                    inputRecord.getDistance(),
                    inputRecord.getCount()
            );
            this.uuid = uuid;
        }
        @Override
        public SearchImgByImgResponse call() throws Exception {
            SearchImgByImgResponse response = new SearchImgByImgResponse();
            // 如果连接状态正常，执行master-server以图搜图
            if (null != cs && cs.getStatusCode() == ConnectionStatusCode.OK) {
                try {
                    logger.info("id : " + this.uuid + ", " + key.getIp() + ":" + key.getPort() + " execute search image by image, starttime : "
                    + input.getStartTime() + ", endtime : " + input.getEndTime() + ", cameraId : " + input.getCameraId() + ", distance : " + input.getDistance() + ", count : " + input.getCount());
                    response = cs.getConnection().searchFaceImgByImg(input, uuid);
                    logger.info("id : " + this.uuid + ", " + key.getIp() + ":" + key.getPort() + " execute search image by image complete, result count is : " + response.getResults().size());
                    rpcManagerService.putConnection(key, cs);
                } catch (TException e) {
                    logger.error(e.getMessage(), e);
                    response.setRCode(com.netposa.poseidon.face.rpc.inrpc.StatusCode.ERROR_SERVICE_EXCEPTION);
                    rpcManagerService.putConnection(key, cs);
                }
            }
            return response;
        }
    }

    public static List<SearchImgResult> executeQuery(SearchImgByImgInputRecord record, String uuid) {
        LinkedList<com.netposa.poseidon.face.rpc.outrpc.SearchImgResult> resultList = new LinkedList<>();
        // 优先队列，用于结果集合并
        final Queue<com.netposa.poseidon.face.rpc.outrpc.SearchImgResult> results = new PriorityQueue<>(record.getCount(), searchImgResultComparator);
        // 获取集群列表
        Map<ConnectionManagerKey, ConnectionManagerValue> cluster = rpcManagerService.getCluster();
        // 用于获取每个线程的返回值
        Future<SearchImgByImgResponse>[] futures = new Future[cluster.size()];
        int index = 0;
        // 向每个server提交任务
        for (final ConnectionManagerKey key : cluster.keySet()) {
            futures[index++] = cachedThreadPool.submit(new SearchFaceImgByImgService().new SearchImgByImgThread(key, record, uuid));
        }

        // 从hbase扫冷数据
        byte[] srcFeature = record.getFeature();
        long srcStartTime = record.getStartTime() > startTime ? record.getStartTime() : startTime;
        List<String> srcCamera = new ArrayList<>();
        if (null != record.getCameraId()) {
            logger.info("camera list is  :" + record.getCameraId());
            String[] cameras = record.getCameraId().split(",");
            for (String camera : cameras) {
                if (null != camera && !"".equals(camera)) {
                    srcCamera.add(camera);
                }
            }
        }
        float srcScore = record.getDistance();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, 1);
        cal.add(Calendar.DATE, (-1) * cacheDays);
        while (cal.getTimeInMillis() - record.getEndTime() > 86400000) {
            cal.add(Calendar.DATE, -1);
        }
        long hbstart = System.currentTimeMillis();
        while (cal.getTimeInMillis() > srcStartTime) {
            String endRow = "" + cal.getTimeInMillis();
            cal.add(Calendar.DATE, -1);
            String startRow = "" + cal.getTimeInMillis();
            int hbaseTotal = 0;
            for (int i = 0 ; i < regionNum; i++) {
                String startKey = String.format("%02d", i);
                List<Result> hbResults = HbaseUtil.getRows(tableName, startKey + startRow, startKey + endRow);
                hbaseTotal += hbResults.size();
                for (Result hbResult : hbResults) {
                    if (null != hbResult) {
                        byte[] logNum = hbResult.getValue(FAMILY, JLBH);
                        byte[] gatherTime = hbResult.getValue(FAMILY, GATHERTIME);
                        byte[] cameraId = hbResult.getValue(FAMILY, CAMERAID);
                        byte[] feature = hbResult.getValue(FAMILY, FEATURE);
                        if (logNum != null && gatherTime != null && cameraId != null && feature != null) {
                            float score = FaceFeatureVerify.verify(srcFeature, 0, feature, 0);
                            if (score >= srcScore) {
                                if (srcCamera.size() == 0) {
                                    results.add(new com.netposa.poseidon.face.rpc.outrpc.SearchImgResult(new String(logNum), new String(cameraId), Long.parseLong(new String(gatherTime)), score));
                                } else if (srcCamera.contains(new String(cameraId))) {
                                    results.add(new com.netposa.poseidon.face.rpc.outrpc.SearchImgResult(new String(logNum), new String(cameraId), Long.parseLong(new String(gatherTime)), score));
                                }
                                if (results.size() > record.getCount()) {
                                    results.poll();
                                }
                            }
                        }
                    }
                }
            }
            logger.info("scan " + sdf.format(new Date(cal.getTimeInMillis())) + " data from hbase complete, scan total : " + hbaseTotal + "result list size is : " + results.size());
        }
        logger.info("scan hbase data cost " + (System.currentTimeMillis() - hbstart) + " ms");

        // 遍历每个线程返回值，放入优先队列
        for (int i = 0; i < index; i++) {
            try {
                SearchImgByImgResponse response = futures[i].get();
                if (response.getRCode() == com.netposa.poseidon.face.rpc.inrpc.StatusCode.OK) {
                    List<com.netposa.poseidon.face.rpc.inrpc.SearchImgResult> list = response.getResults();
                    if (null != list && list.size() > 0) {
                        for (com.netposa.poseidon.face.rpc.inrpc.SearchImgResult sir : list) {
                            results.add(new com.netposa.poseidon.face.rpc.outrpc.SearchImgResult(sir.getLogNum(), sir.getCameraId(), sir.getGatherTime(), sir.getScore()));
                            if (results.size() > record.getCount()) {
                                results.poll();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        while (!results.isEmpty()) {
            resultList.addFirst(results.poll());
        }

        return resultList;
    }

    //匿名Comparator实现
    private static Comparator<com.netposa.poseidon.face.rpc.outrpc.SearchImgResult> searchImgResultComparator = new Comparator<com.netposa.poseidon.face.rpc.outrpc.SearchImgResult>() {
        @Override
        public int compare(com.netposa.poseidon.face.rpc.outrpc.SearchImgResult c1, com.netposa.poseidon.face.rpc.outrpc.SearchImgResult c2) {
            return (int)(c1.getScore()- c2.getScore());
        }
    };
}
