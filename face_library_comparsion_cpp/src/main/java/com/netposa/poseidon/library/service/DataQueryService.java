package com.netposa.poseidon.library.service;

import com.netposa.HbaseUtil;
import com.netposa.poseidon.library.bean.*;
import com.netposa.poseidon.library.rpc.outrpc.*;
import com.netposa.poseidon.library.util.Byte2FloatUtil;
import com.netposa.poseidon.library.util.LoadPropers;
import com.netposa.poseidon.library.util.SearchImgResultFromHbase;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DataQueryService {
    // 创建线程池，每个线程调用一个server上的rpc进行数据操作
    private static ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    private static Logger logger = Logger.getLogger(LibraryOperatorService.class);
    private static final Properties properties = LoadPropers.getSingleInstance().getProperties("server");
    private static RpcManagerService rpcManagerService = RpcManagerService.getInstance();
    private static TableManagerService tableManagerService = TableManagerService.getInstance();
    private static int faceVersionSize = Integer.parseInt(properties.getProperty("face.version.size"));
    private static int faceFeatureSize = Integer.parseInt(properties.getProperty("face.feature.size"));
    private static int faceTailSize = Integer.parseInt(properties.getProperty("face.tail.size"));
    private static int floatSize = 4;
    private static int featRawDim = faceFeatureSize / floatSize;
    private static boolean debug = Boolean.parseBoolean(properties.getProperty("service.process.debug.enable", "false"));

    public static QueryResponse execute(QueryRequest request, String uuid) {
        LinkedList<com.netposa.poseidon.library.rpc.outrpc.RecordInfo> resultList = new LinkedList<>();
        // 优先队列，用于结果集合并
        final PriorityQueue<RecordInfo> results = new PriorityQueue<>(request.getRCount(), dataQueryComparator);
        //参数判断
        if (request == null || request.getLibraryIds() == null || request.getLibraryIds().size() == 0 || request.getSimilarity() < 0 || request.getRCount() <= 0) {
            logger.error(String.format("Args error,[%s]", request));
            return new QueryResponse(StatusCode.ERROR_PARAM, String.format("Task [%s] dataQuery args error,[%s]", uuid, request), null);
        }
        if (request.getFeature().length != (faceFeatureSize + faceTailSize + faceVersionSize)) {
            logger.error(String.format("Args error,[%s]", request));
            return new QueryResponse(StatusCode.ERROR_PARAM, String.format("Task [%s] error feature length,[%s]", uuid, request.getFeature().length), null);
        }
        for (String tableN : request.getLibraryIds()) {
            if (!HbaseUtil.tableIsExists(tableN)) {
                return new QueryResponse(StatusCode.ERROR_NOT_EXIST_LIBRARY, String.format("Task [%s] dataQuery error,table [%s] is not exists!", uuid, tableN), null);
            } else if (!tableManagerService.getTable().containsKey(tableN)) {
                return new QueryResponse(StatusCode.ERROR_NOT_EXIST_LIBRARY, String.format("Task [%s] dataQuery error,table [%s] is not exists!", uuid, tableN), null);
            } else {
                boolean flag = true;
                for (TableStatusCode code : tableManagerService.getTable().get(tableN).values()) {
                    flag &= code == TableStatusCode.IN_USING;
                }
                if (!flag) {
                    return new QueryResponse(StatusCode.ERROR_NOT_EXIST_LIBRARY, String.format("Task [%s] dataQuery error,table [%s] is not in using!", uuid, tableN), null);
                }
            }
        }

        // 获取集群列表
        Map<ConnectionManagerKey, ConnectionManagerValue> cluster = rpcManagerService.getCluster();
        // 用于获取每个线程的返回值
        Future<com.netposa.poseidon.library.rpc.inrpc.QueryResponse>[] futures = new Future[cluster.size()];
        int index = 0;
        // 向每个server提交任务
        for (final ConnectionManagerKey key : cluster.keySet()) {
            futures[index++] = cachedThreadPool.submit(new DataQueryService().new DataQueryThread(key, request, uuid));
        }
        // 遍历每个线程返回值，放入优先队列
        for (int i = 0; i < index; i++) {
            try {
                com.netposa.poseidon.library.rpc.inrpc.QueryResponse queryResponse = futures[i].get();
                if (queryResponse.getRCode() == com.netposa.poseidon.library.rpc.inrpc.StatusCode.OK) {
                    List<com.netposa.poseidon.library.rpc.inrpc.RecordInfo> info = queryResponse.getRecordInfos();
                    if (null != info && info.size() > 0) {
                        for (com.netposa.poseidon.library.rpc.inrpc.RecordInfo ri : info) {
                            double highestScore = ri.getHighestScore();
                            results.add(new RecordInfo(ri.getId(), inImgScore2OutImgScore(ri.getImgScore()), ri.getExt(), highestScore, ri.getHighestScoreImgId()));
                            if (results.size() > request.getRCount()) {
                                results.poll();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        PriorityQueue<RecordInfo> priorityQueue = SearchImgResultFromHbase.execute(request, results);
        while (!priorityQueue.isEmpty()) {
            resultList.addFirst(priorityQueue.poll());
        }
        return new QueryResponse(StatusCode.OK, "success", resultList);
    }

    private class DataQueryThread implements Callable<com.netposa.poseidon.library.rpc.inrpc.QueryResponse> {
        // 用于存放
        ConnectionManagerKey key;
        // master-server连接
        ClientStatus cs;
        // master-server以图搜图输入条件
        com.netposa.poseidon.library.rpc.inrpc.QueryRequest input;
        // 唯一标识uuid
        String uuid;

        DataQueryThread(ConnectionManagerKey key, QueryRequest inputRecord, String uuid) {
            this.key = key;
            // 获取一个可用的连接
            AvailableConnectionBean acb = rpcManagerService.getAvailableConnection(key, false);
            if (null != acb) {
                this.cs = acb.getCs();
            }
            // 特征转换
            List<Double> featureFloat = new ArrayList<>();
            for (int i = 0; i < featRawDim; i++) {
                featureFloat.add((double) Byte2FloatUtil.byte2float(inputRecord.getFeature(), faceVersionSize + i * floatSize));
            }
            this.input = new com.netposa.poseidon.library.rpc.inrpc.QueryRequest(
                    inputRecord.getLibraryIds(),
                    inputRecord.getSimilarity(),
                    inputRecord.getRCount(),
                    featureFloat,
                    inputRecord.getRequestId());
            this.uuid = uuid;
        }

        @Override
        public com.netposa.poseidon.library.rpc.inrpc.QueryResponse call() throws Exception {
            com.netposa.poseidon.library.rpc.inrpc.QueryResponse response = new com.netposa.poseidon.library.rpc.inrpc.QueryResponse();
            // 如果连接状态正常，执行master-server以图搜图
            if (null != cs && cs.getStatusCode() == ConnectionStatusCode.OK) {
                try {
                    logger.info("id : " + this.uuid + ", " + key.getIp() + ":" + key.getPort() + " execute data query");
                    response = cs.getConnection().dataQuery(input, uuid);
                    logger.info("id : " + this.uuid + ", " + key.getIp() + ":" + key.getPort() + " execute data query complete, result size is : " + response.getRecordInfos().size());
                    rpcManagerService.putConnection(key, cs);
                } catch (TException e) {
                    logger.error(e.getMessage(), e);
                    response.setRCode(com.netposa.poseidon.library.rpc.inrpc.StatusCode.ERROR_OTHER);
                    rpcManagerService.putConnection(key, cs);
                }
            }
            return response;
        }

    }

    private static List<ImgScore> inImgScore2OutImgScore(List<com.netposa.poseidon.library.rpc.inrpc.ImgScore> list) {
        List<ImgScore> result = new ArrayList<>();
        for (com.netposa.poseidon.library.rpc.inrpc.ImgScore score : list) {
            result.add(new ImgScore(score.getImgId(), score.getScore()));
        }
        return result;
    }

    //匿名Comparator实现
    private static Comparator<RecordInfo> dataQueryComparator = new Comparator<RecordInfo>() {
        @Override
        public int compare(RecordInfo c1, RecordInfo c2) {
            if (c1.getHighestScore() > c2.getHighestScore()) {
                return 1;
            } else if (c1.getHighestScore() < c2.getHighestScore()) {
                return -1;
            } else {
                return 0;
            }
        }
    };
}
