package com.netposa.poseidon.human.service;

import com.netposa.HbaseUtil;
import com.netposa.poseidon.bean.ResponseResult;
import com.netposa.poseidon.connection.AuxiliayRetirval;
import com.netposa.poseidon.connection.ReverseAuxiliary;
import com.netposa.poseidon.human.bean.*;
import com.netposa.poseidon.human.rpc.inrpc.SearchImgByImgResponse;
import com.netposa.poseidon.human.rpc.outrpc.*;
import com.netposa.poseidon.human.util.Byte2FloatUtil;
import com.netposa.poseidon.human.util.HashAlgorithm;
import com.netposa.poseidon.human.util.HumanFeatureVerifyDg;
import com.netposa.poseidon.human.util.LoadPropers;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class HumanDataAnalyzeServiceHandler implements HumanFeatureDataAnalyzeRpcService.Iface {
    private static Logger logger = Logger.getLogger(HumanFeatureDataAnalyzeRpcService.class);
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private static Properties properties = LoadPropers.getSingleInstance().getProperties("server");
    private static int regionNum = Integer.parseInt(properties.getProperty("hbase.split.region.num", "50").trim());
    private static String tableName = properties.getProperty("hbase.table.name").trim();
    private static int minScore = Integer.parseInt(properties.getProperty("default.score", "90").trim());
    private static int featureSize = Integer.parseInt(properties.getProperty("human.feature.size").trim());
    private static int cacheDays = Integer.parseInt(properties.getProperty("duration.time", "0").trim());
    private static long startTime = 0;
    private static int floatSize = 4;
    private static int featRawDim = Integer.parseInt(properties.getProperty("human.feature.size").trim()) / floatSize;

    private static byte[] FAMILY = "cf".getBytes();
    private static byte[] CAMERAID = "cameraId".getBytes();
    private static byte[] JLBH = "logNum".getBytes();
    private static byte[] FEATURE = "feature".getBytes();
    private static byte[] GATHERTIME = "gatherTime".getBytes();

    private static RpcManagerService rpcManagerService = RpcManagerService.getInstance();
    // 创建线程池，每个线程调用一个server上的rpc进行以图搜图
    private static ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    static {
        try {
            startTime = sdf.parse(properties.getProperty("start.time").trim()).getTime();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    class SearchImgByImgThread implements Callable<SearchImgByImgResponse> {
        // 用于存放
        ConnectionManagerKey key;
        // master-server连接
        ClientStatus cs;
        // master-server以图搜图输入条件
        com.netposa.poseidon.human.rpc.inrpc.SearchImgByImgInputRecord input;
        // 唯一标识uuid
        String uuid;

        SearchImgByImgThread(ConnectionManagerKey key, SearchImgByImgInputRecord inputRecord, String uuid) {
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
                feature.add((double)Byte2FloatUtil.byte2float(srcFeature, i * floatSize));
            }
            this.input = new com.netposa.poseidon.human.rpc.inrpc.SearchImgByImgInputRecord(
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
                    logger.info("id : " + this.uuid + ", " + key.getIp() + ":" + key.getPort() + " execute search image by image, startTime" +
                            input.getStartTime() + ", endtime : " + input.getEndTime() + ", cameraId : " + input.getCameraId() + ", distance : " + input.getDistance() + ", count : " + input.getCount());
                    response = cs.getConnection().searchHummerImgByImg(input, uuid);
                    logger.info("id : " + this.uuid + ", " + key.getIp() + ":" + key.getPort() + " execute search image by image complete, result count is : " + response.getResults().size());
                    rpcManagerService.putConnection(key, cs);
                } catch (TException e) {
                    logger.error(e.getMessage(), e);
                    response.setRCode(com.netposa.poseidon.human.rpc.inrpc.StatusCode.ERROR_SERVICE_EXCEPTION);
                    rpcManagerService.putConnection(key, cs);
                }
            }
            return response;
        }
    }

    @Override
    public List<SearchImgResult> searchHummerImgByImg(SearchImgByImgInputRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        LinkedList<SearchImgResult> resultList = new LinkedList<>();
        logger.info("searchHummerImgByImg id : " + uuid + ", "  + record.toString());
        // 参数校验
        if (record.getStartTime() <= 0) {
            record.setStartTime(startTime);
        }
        if (record.getStartTime() > record.getEndTime() || null == record.getFeature() || record.getFeature().length != featureSize) {
            logger.warn("error param, please check!");
            return resultList;
        }
        if (record.getDistance() <= 0) {
            record.setDistance(minScore);
        }
        long start = System.currentTimeMillis();
        // 优先队列，用于结果集合并
        final Queue<SearchImgResult> results = new PriorityQueue<>(record.getCount(), searchImgResultComparator);
        // 获取集群列表
        Map<ConnectionManagerKey, ConnectionManagerValue> cluster = rpcManagerService.getCluster();
        // 用于获取每个线程的返回值
        Future<SearchImgByImgResponse>[] futures = new Future[cluster.size()];
        int index = 0;
        // 向每个server提交任务
        for (final ConnectionManagerKey key : cluster.keySet()) {
            futures[index++] = cachedThreadPool.submit(new SearchImgByImgThread(key, record, uuid));
        }

        // 从hbase扫冷数据
        byte[] srcFeature = record.getFeature();
        long srcStartTime = record.getStartTime() > startTime ? record.getStartTime() : startTime;
        List<String> srcCamera = new ArrayList<>();
        if (null != record.getCameraId()) {
            String[] cameras = record.getCameraId().split(",");
            srcCamera.addAll(Arrays.asList(cameras));
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
            for (int i = 0 ; i < regionNum; i++) {
                String startKey = String.format("%02d", i);
                List<Result> hbResults = HbaseUtil.getRows(tableName, startKey + startRow, startKey + endRow);
                for (Result hbResult : hbResults) {
                    if (null != hbResult) {
                        byte[] logNum = hbResult.getValue(FAMILY, JLBH);
                        byte[] gatherTime = hbResult.getValue(FAMILY, GATHERTIME);
                        byte[] cameraId = hbResult.getValue(FAMILY, CAMERAID);
                        byte[] feature = hbResult.getValue(FAMILY, FEATURE);
                        if (logNum != null && gatherTime != null && cameraId != null && feature != null) {
                            float score = HumanFeatureVerifyDg.verify(srcFeature, 0, feature, 0);
                            if (score >= srcScore) {
                                if (srcCamera.size() == 0) {
                                    results.add(new SearchImgResult(new String(logNum), new String(cameraId), Long.parseLong(new String(gatherTime)), score));
                                } else if (srcCamera.contains(new String(cameraId))) {
                                    results.add(new SearchImgResult(new String(logNum), new String(cameraId), Long.parseLong(new String(gatherTime)), score));
                                }
                                if (results.size() > record.getCount()) {
                                    results.poll();
                                }
                            }
                        }
                    }
                }
            }
            logger.info("scan " + sdf.format(new Date(cal.getTimeInMillis())) + " data from hbase complete");
        }
        logger.info("scan hbase data cost " + (System.currentTimeMillis() - hbstart) + " ms");

        // 遍历每个线程返回值，放入优先队列
        for (int i = 0; i < index; i++) {
            try {
                SearchImgByImgResponse response = futures[i].get();
                if (response.getRCode() == com.netposa.poseidon.human.rpc.inrpc.StatusCode.OK) {
                    List<com.netposa.poseidon.human.rpc.inrpc.SearchImgResult> list = response.getResults();
                    if (null != list && list.size() > 0) {
                        for (com.netposa.poseidon.human.rpc.inrpc.SearchImgResult sir : list) {
                            results.add(new SearchImgResult(sir.getLogNum(), sir.getCameraId(), sir.getGatherTime(), sir.getScore()));
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
        logger.info("searchHummerImgByImg id : " + uuid + " completed, use time : " + (System.currentTimeMillis() - start) + " ms, result list size is : " + resultList.size());
        return resultList;
    }

    @Override
    public List<SearchImgResult> searchHummerImgByLog(SearchImgByLogInputRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        logger.info("searchHummerImgByLog id : " + uuid + ", "  + record.toString());
        // 日志编号搜图计时
        long start = System.currentTimeMillis();
        // 由于thrift在序列化时不能用null作为返回值，先创建一个返回值对象
        List<SearchImgResult> results = new ArrayList<>();
        // 参数检验
        if (null == record.getDate() || "".equals(record.getDate())) {
            logger.error("date can not be empty");
            return results;
        }
        // 先通过记录编号和采集时间在hbase里找到对应的特征
        SearchFeatureByLogInputRecord searchFeatureByLogInputRecord = new SearchFeatureByLogInputRecord();
        searchFeatureByLogInputRecord.setLogNum(record.getLogNum());
        searchFeatureByLogInputRecord.setGatherTime(Long.parseLong(record.getDate()));
        SearchFeatureByLogResponse response = searchFeatureByLog(searchFeatureByLogInputRecord);
        // 操作成功
        if (response.rCode == StatusCode.OK) {
            // 正确取到了特征
            if (null != response.getFeature()) {
                SearchImgByImgInputRecord searchImgByImgInputRecord = new SearchImgByImgInputRecord();
                searchImgByImgInputRecord.setStartTime(record.getStartTime());
                searchImgByImgInputRecord.setEndTime(record.getEndTime());
                searchImgByImgInputRecord.setFeature(ByteBuffer.wrap(response.getFeature()));
                searchImgByImgInputRecord.setDistance(record.getDistance());
                searchImgByImgInputRecord.setCameraId(record.getCameraId());
                searchImgByImgInputRecord.setCount(record.getCount());
                // 执行以图搜图
                results = searchHummerImgByImg(searchImgByImgInputRecord);
            }
        }
        logger.info("SearchHumanImgByLog complete, id : " + uuid + ", used_time[ms] : " + (System.currentTimeMillis() - start) + ", result list size is : " + results.size());
        return results;
    }

    @Override
    public SearchImgByAssistResponse searchHummerImgByAssist(SearchImgByAssistInputRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        logger.info("logId:" + uuid + ", SearchImgByAssistInputRecord:" + record.toString());

        SearchImgByAssistResponse response = new SearchImgByAssistResponse();
        if ((null == record.resultList || record.resultList.size() == 0) && (null == record.assist || record.assist.size() == 0)) {
            response.setRCode(StatusCode.ERROR_PARAM);
            response.setMessage("logId:" + uuid + ", resultList or assistList param error");
            return response;
        }
        if (record.getStartTime() < startTime) {
            record.setStartTime(startTime);
        }
        if (record.getAssistDistance() <= 0) {
            record.setAssistDistance(minScore);
        }
        if (null != record.exclude && record.exclude.size() > 0 && record.getExcludeDistance() <= 0) {
            record.setExcludeDistance(minScore);
        }
        if(record.exclude == null || record.exclude.size()==0){
            //正向辅助检索
            long startTime = System.currentTimeMillis();
            if (null == record.resultList || record.resultList.size() == 0) {
                //上次计算结果为空
                //辅助目标特征数量为1；
                if (record.getAssist().size() == 1) {
                    SearchImgByImgInputRecord input = new SearchImgByImgInputRecord();
                    input.setFeature(record.getAssist().get(record.getAssist().size() - 1));
                    input.setStartTime(record.getStartTime());
                    input.setEndTime(record.getEndTime());
                    input.setCameraId(record.getCameraID());
                    input.setCount(record.getCount());
                    input.setDistance(record.getAssistDistance());
                    List<SearchImgResult> searchImgRes = searchHummerImgByImg(input);
                    response.setRCode(StatusCode.OK);
                    response.setResult(searchImgRes);
                    response.setMessage("SUCCESS");
                } else {
                    //辅助目标特征数量为n；
                    List<SearchImgByImgInputRecord> inputList = new ArrayList<>();
                    for (int i = 0; i < record.getAssist().size(); ++i) {
                        inputList.add(new SearchImgByImgInputRecord(record.getStartTime(), record.getEndTime(), record.getCameraID(),
                                record.getAssist().get(i), record.getAssistDistance(), record.getCount()));
                    }
                    //进行n次以图搜图
                    List<List<SearchImgResult>> searchImgResList = new ArrayList<>();
                    for (SearchImgByImgInputRecord input : inputList) {
                        List<SearchImgResult> tmpRes = searchHummerImgByImg(input);
                        logger.info("logId:" + uuid + ",execute once searchHummerImgByImg, used_time[ms]:" + (System.currentTimeMillis() - startTime) + ", result_size:" + tmpRes.size());
                        searchImgResList.add(tmpRes);
                    }
                    logger.info("logId:" + uuid + ", searchHummerImgByImg complete , used_time[ms]:" + (System.currentTimeMillis() - startTime) + ", result_size:" + searchImgResList.size());

                    //进行n-1次辅助检索
                    List<com.netposa.poseidon.bean.SearchImgResult> resList = new ArrayList<>();
                    for (int i = 0; i < searchImgResList.get(0).size(); ++i) {
                        resList.add(new com.netposa.poseidon.bean.SearchImgResult(searchImgResList.get(0).get(i).getLogNum(),
                                searchImgResList.get(0).get(i).getCameraId(),
                                searchImgResList.get(0).get(i).getGatherTime(),
                                searchImgResList.get(0).get(i).getScore()));
                    }
                    for (int i = 1; i < searchImgResList.size(); ++i) {
                        List<com.netposa.poseidon.bean.SearchImgResult> assistList = new ArrayList<>();
                        for (int j = 0; j < searchImgResList.get(i).size(); ++j) {
                            assistList.add(new com.netposa.poseidon.bean.SearchImgResult(searchImgResList.get(i).get(j).getLogNum(),
                                    searchImgResList.get(i).get(j).getCameraId(),
                                    searchImgResList.get(i).get(j).getGatherTime(),
                                    searchImgResList.get(i).get(j).getScore()));
                        }
                        ResponseResult tmpResponse = AuxiliayRetirval.auxiliayRetirval(resList, assistList);
                        if (tmpResponse.getRCode() != 2) {
                            response.setRCode(StatusCode.ERROR_OTHER);
                            response.setMessage(tmpResponse.getMessage());
                            break;
                        }
                        resList.clear();
                        resList = new ArrayList<>(tmpResponse.getList());
                    }
                    List<SearchImgResult> result = new ArrayList<>();
                    for (com.netposa.poseidon.bean.SearchImgResult aResList : resList) {
                        result.add(new SearchImgResult(aResList.getRecordId(), aResList.getNumber(), aResList.getGatherTime(), aResList.getScore()));
                    }
                    response.setRCode(StatusCode.OK);
                    response.setMessage("SUCCESS");
                    response.setResult(result);
                }
            } else {
                SearchImgByImgInputRecord input = new SearchImgByImgInputRecord();
                input.setFeature(record.getAssist().get(record.getAssist().size() - 1));
                input.setStartTime(record.getStartTime());
                input.setEndTime(record.getEndTime());
                input.setCameraId(record.getCameraID());
                input.setCount(record.getCount());
                input.setDistance(record.getAssistDistance());

                //进行以图搜图
                List<SearchImgResult> searchImgRes = searchHummerImgByImg(input);
                List<com.netposa.poseidon.bean.SearchImgResult> assistList = new ArrayList<>();
                for (SearchImgResult re : searchImgRes) {
                    assistList.add(new com.netposa.poseidon.bean.SearchImgResult(re.getLogNum(),
                            re.getCameraId(), re.getGatherTime(), re.getScore()));
                }
                long endSearchImgTime = System.currentTimeMillis();
                logger.info("logId:" + uuid + ",execute once searchHummerImgByImg, used_time[ms]:" + (endSearchImgTime - startTime)+ ", result_size:" + assistList.size());

                //解析上次结果，组装数据
                List<SearchImgResult> src = record.getResultList();
                List<com.netposa.poseidon.bean.SearchImgResult> srcList = new ArrayList<>();
                for (int i = 0; i < src.size(); ++i) {
                    srcList.add(new com.netposa.poseidon.bean.SearchImgResult(src.get(i).getLogNum(), src.get(i).getCameraId(), src.get(i).getGatherTime(), src.get(i).getScore()));
                }

                ResponseResult res = AuxiliayRetirval.auxiliayRetirval(srcList, assistList);

                List<com.netposa.poseidon.bean.SearchImgResult> resList = res.getList();
                List<SearchImgResult> result = new ArrayList<>();
                for (com.netposa.poseidon.bean.SearchImgResult re : resList) {
                    result.add(new SearchImgResult(re.getRecordId(), re.getNumber(), re.getGatherTime(), re.getScore()));
                }
                if (res.getRCode() == 2) {
                    response.setRCode(StatusCode.OK);
                } else {
                    response.setRCode(StatusCode.ERROR_OTHER);
                }
                response.setMessage(res.getMessage());
                response.setResult(result);
            }
            logger.info("logId:" + uuid + ", complete multi Img search, used_time[ms]:" + (System.currentTimeMillis() - startTime) + ", result_size:" + response.getResult().size());
        }else{
            //逆向辅助检索
            long start = System.currentTimeMillis();
            if (null == record.resultList || record.resultList.size() == 0) {
                //1张原图 多张逆向辅助图片
                if (record.getAssist().size() == 1 ) {
                    //单张图片只做一次以图搜图
                    SearchImgByImgInputRecord input = new SearchImgByImgInputRecord();
                    input.setFeature(record.getAssist().get(record.getAssist().size() - 1));
                    input.setStartTime(record.getStartTime());
                    input.setEndTime(record.getEndTime());
                    input.setCount(record.getCount());
                    input.setCameraId(record.getCameraID());
                    input.setDistance(record.getAssistDistance());
                    List<SearchImgResult> searchImgRes = searchHummerImgByImg(input);
                    List<com.netposa.poseidon.bean.SearchImgResult> resList = new ArrayList<>();
                    for (int i = 0; i < searchImgRes.size(); ++i) {
                        resList.add(new com.netposa.poseidon.bean.SearchImgResult(searchImgRes.get(i).getLogNum(), searchImgRes.get(i).getCameraId(),
                                searchImgRes.get(i).getGatherTime(), searchImgRes.get(i).getScore()));
                    }
                    //组装排除集合的多图条件
                    List<SearchImgByImgInputRecord> inputList = new ArrayList<>();
                    for (int i = 0; i < record.getExclude().size(); ++i) {
                        inputList.add(new SearchImgByImgInputRecord(record.getStartTime(), record.getEndTime(), record.getCameraID(),
                                record.getExclude().get(i), record.getExcludeDistance(), record.getCount()));
                    }
                    //排除集合的图片遍历以图搜图组装list
                    List<List<SearchImgResult>> searchImgResList = new ArrayList<>();
                    for (SearchImgByImgInputRecord excludeinput : inputList) {
                        List<SearchImgResult> tmpRes = searchHummerImgByImg(excludeinput);
                        logger.info(String.format("execute once search image, feature length is %s, result length is %s", excludeinput.getFeature().length, tmpRes.size()));
                        searchImgResList.add(tmpRes);
                    }
                    for (int i = 0; i < searchImgResList.size(); ++i) {
                        List<com.netposa.poseidon.bean.SearchImgResult> ExcludeList = new ArrayList<>();
                        for (int j = 0; j < searchImgResList.get(i).size(); ++j) {
                            ExcludeList.add(new com.netposa.poseidon.bean.SearchImgResult(searchImgResList.get(i).get(j).getLogNum(), searchImgResList.get(i).get(j).getCameraId(),
                                    searchImgResList.get(i).get(j).getGatherTime(), searchImgResList.get(i).get(j).getScore()));
                        }
                        ResponseResult tmpResponse = ReverseAuxiliary.reverseRetirval(resList, ExcludeList);
                        if (tmpResponse.getRCode() != 2) {
                            response.setRCode(StatusCode.ERROR_OTHER);
                            response.setMessage(tmpResponse.getMessage());
                            break;
                        }
                        resList.clear();
                        resList = new ArrayList<>(tmpResponse.getList());
                    }
                    List<SearchImgResult> result = new ArrayList<>();
                    for (com.netposa.poseidon.bean.SearchImgResult aResList : resList) {
                        result.add(new SearchImgResult(aResList.getRecordId(), aResList.getNumber(), aResList.getGatherTime(), aResList.getScore()));
                    }
                    response.setRCode(StatusCode.OK);
                    response.setResult(result);
                    response.setMessage("SUCCESS");
                } else {
                    //多张图片，多张逆向辅助检索
                    //组装多个正向图片以图搜图条件
                    List<SearchImgByImgInputRecord> inputList = new ArrayList<>();
                    for (int i = 0; i < record.getAssist().size(); ++i) {
                        inputList.add(new SearchImgByImgInputRecord(record.getStartTime(), record.getEndTime(), record.getCameraID(),
                                record.getAssist().get(i), record.getAssistDistance(), record.getCount()));
                    }
                    //进行n次以图搜图
                    List<List<SearchImgResult>> searchImgResList = new ArrayList<>();
                    for (SearchImgByImgInputRecord input : inputList) {
                        List<SearchImgResult> tmpRes = searchHummerImgByImg(input);
                        logger.info(String.format("execute once search image, feature length is %s, result length is %s", input.getFeature().length, tmpRes.size()));
                        searchImgResList.add(tmpRes);
                    }
                    long endSearchImg = System.currentTimeMillis();
                    logger.info(String.format("complete once searchImgByImg, execute time is : %s ms, result size is : %s", (endSearchImg - start), searchImgResList.size()));
                    //进行n-1次辅助检索
                    List<com.netposa.poseidon.bean.SearchImgResult> resList = new ArrayList<>();
                    for (int i = 0; i < searchImgResList.get(0).size(); ++i) {
                        resList.add(new com.netposa.poseidon.bean.SearchImgResult(searchImgResList.get(0).get(i).getLogNum(), searchImgResList.get(0).get(i).getCameraId(),
                                searchImgResList.get(0).get(i).getGatherTime(), searchImgResList.get(0).get(i).getScore()));
                    }
                    //遍历多张排除图片并组装多图检索条件
                    List<SearchImgByImgInputRecord> excludeinputList = new ArrayList<>();
                    for (int i = 0; i < record.getExclude().size(); ++i) {
                        excludeinputList.add(new SearchImgByImgInputRecord(record.getStartTime(), record.getEndTime(), record.getCameraID(),
                                record.getExclude().get(i), record.getExcludeDistance(), record.getCount()));
                    }
                    //排除集合的图片遍历以图搜图，结果组装list
                    List<List<SearchImgResult>> excludesearchImgResList = new ArrayList<>();
                    for (SearchImgByImgInputRecord excludeinput : excludeinputList) {
                        List<SearchImgResult> tmpRes = searchHummerImgByImg(excludeinput);
                        logger.info(String.format("execute once search image, feature length is %s, result length is %s", excludeinput.getFeature().length, tmpRes.size()));
                        excludesearchImgResList.add(tmpRes);
                    }
                    for (int i = 1; i < excludesearchImgResList.size(); ++i) {
                        List<com.netposa.poseidon.bean.SearchImgResult> assistList = new ArrayList<>();
                        for (int j = 0; j < excludesearchImgResList.get(i).size(); ++j) {
                            assistList.add(new com.netposa.poseidon.bean.SearchImgResult(excludesearchImgResList.get(i).get(j).getLogNum(), excludesearchImgResList.get(i).get(j).getCameraId(),
                                    excludesearchImgResList.get(i).get(j).getGatherTime(), excludesearchImgResList.get(i).get(j).getScore()));
                        }
                        ResponseResult tmpResponse = ReverseAuxiliary.reverseRetirval(resList, assistList);
                        if (tmpResponse.getRCode() != 2) {
                            response.setRCode(StatusCode.ERROR_OTHER);
                            response.setMessage(tmpResponse.getMessage());
                            break;
                        }
                        resList.clear();
                        resList = new ArrayList<>(tmpResponse.getList());
                    }
                    List<SearchImgResult> result = new ArrayList<>();
                    for (com.netposa.poseidon.bean.SearchImgResult aResList : resList) {
                        result.add(new SearchImgResult(aResList.getRecordId(), aResList.getNumber(), aResList.getGatherTime(), aResList.getScore()));
                    }
                    response.setRCode(StatusCode.OK);
                    response.setMessage("SUCCESS");
                    response.setResult(result);
                }
            } else {
                SearchImgByImgInputRecord input = new SearchImgByImgInputRecord();
                input.setFeature(record.getExclude().get(record.getExclude().size() - 1));
                input.setStartTime(record.getStartTime());
                input.setEndTime(record.getEndTime());
                input.setCameraId(record.getCameraID());
                input.setCount(record.getCount());
                input.setDistance(record.getExcludeDistance());
                //进行以图搜图
                List<SearchImgResult> searchImgRes = searchHummerImgByImg(input);
                List<com.netposa.poseidon.bean.SearchImgResult> assistList = new ArrayList<>();
                for (SearchImgResult re : searchImgRes) {
                    assistList.add(new com.netposa.poseidon.bean.SearchImgResult(re.getLogNum(),
                            re.getCameraId(), re.getGatherTime(), re.getScore()));
                }
                long endSearchImg = System.currentTimeMillis();
                logger.info(String.format("complete once searchImgByImg, execute time is : %s ms, result size is : %s", (endSearchImg - start), assistList.size()));
                //解析上次结果，组装数据
                List<SearchImgResult> src = record.getResultList();
                List<com.netposa.poseidon.bean.SearchImgResult> srcList = new ArrayList<>();
                for (int i = 0; i < src.size(); ++i) {
                    srcList.add(new com.netposa.poseidon.bean.SearchImgResult(src.get(i).getLogNum(), src.get(i).getCameraId(), src.get(i).getGatherTime(), src.get(i).getScore()));
                }
                ResponseResult res = ReverseAuxiliary.reverseRetirval(srcList, assistList);
                long endAssistSearch = System.currentTimeMillis();
                logger.info(String.format("complete assist search, execute time is : %s ms, result size is : %s", (endAssistSearch - endSearchImg), res.getList().size()));
                List<com.netposa.poseidon.bean.SearchImgResult> resList = res.getList();
                List<SearchImgResult> result = new ArrayList<>();
                for (com.netposa.poseidon.bean.SearchImgResult re : resList) {
                    result.add(new SearchImgResult(re.getRecordId(), re.getNumber(), re.getGatherTime(), re.getScore()));
                }
                if (res.getRCode() == 2) {
                    response.setRCode(StatusCode.OK);
                } else {
                    response.setRCode(StatusCode.ERROR_OTHER);
                }
                response.setMessage(res.getMessage());
                response.setResult(result);
            }
            logger.info(String.format("complete multi Img search, execute time is : %s ms, result size is : %s", (System.currentTimeMillis() - start), response.getResult().size()));

        }

        return response;
    }

    @Override
    public SearchFeatureByLogResponse searchFeatureByLog(SearchFeatureByLogInputRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        logger.info("searchFeatureByLog id : " + uuid + ", "  + record.toString());
        SearchFeatureByLogResponse response = new SearchFeatureByLogResponse();
        // 进行参数校验
        if (null == record.getLogNum() || "".equals(record.getLogNum())) {
            response.setRCode(StatusCode.ERROR_PARAM);
            response.setMessage("input param logNum error, please check!");
            return response;
        }
        // 日志编号搜特征计时
        long start = System.currentTimeMillis();
        // 循环在每个region里搜索
        for (int i = 0; i < regionNum; ++i) {
            // regionNum + 采集时间 + 记录编号 作为rowkey
            String rowKey = String.format("%02d", i) + record.getGatherTime() + record.getLogNum();
            Result res = HbaseUtil.getOneRow(tableName, rowKey);
            // 正确取到特征
            if (null != res && null != res.getValue("cf".getBytes(), "feature".getBytes())) {
                response.setMessage("success!");
                response.setFeature(res.getValue("cf".getBytes(), "feature".getBytes()));
                break;
            }
        }
        if (response.getFeature() == null) {
            response.setMessage("not found!");
        }
        // 标识本次查询正常完成
        response.setRCode(StatusCode.OK);
        logger.info("SearchFeatureByLog id : " + uuid + " complete, used_time[ms]:" + (System.currentTimeMillis() - start));
        return response;
    }

    //匿名Comparator实现
    private static Comparator<SearchImgResult> searchImgResultComparator = new Comparator<SearchImgResult>(){
        @Override
        public int compare(SearchImgResult c1, SearchImgResult c2) {
            return (int)(c1.getScore()- c2.getScore());
        }
    };
}
