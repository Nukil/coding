package com.netposa.poseidon.face.service;

import com.netposa.HbaseUtil;
import com.netposa.poseidon.bean.ResponseResult;
import com.netposa.poseidon.connection.AuxiliayRetirval;
import com.netposa.poseidon.connection.ReverseAuxiliary;
import com.netposa.poseidon.face.bean.*;
import com.netposa.poseidon.face.rpc.inrpc.SearchImgByImgResponse;
import com.netposa.poseidon.face.rpc.outrpc.*;
import com.netposa.poseidon.face.util.Byte2FloatUtil;
import com.netposa.poseidon.face.util.FaceFeatureVerify;
import com.netposa.poseidon.face.util.LoadPropers;
import org.apache.hadoop.hbase.client.Result;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class FaceDataAnalyzeServiceHandler implements FaceFeatureDataAnalyzeRpcService.Iface {
    private static Logger logger = Logger.getLogger(FaceFeatureDataAnalyzeRpcService.class);
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private static Properties properties = LoadPropers.getSingleInstance().getProperties("server");
    private static int regionNum = Integer.parseInt(properties.getProperty("hbase.split.region.num", "50").trim());
    private static String tableName = properties.getProperty("hbase.table.name").trim();
    private static int minScore = Integer.parseInt(properties.getProperty("default.score", "90").trim());
    private static boolean debug = Boolean.parseBoolean(properties.getProperty("service.process.debug.enable", "false"));
    private static long startTime = 0;
    private static int FACE_FEATURE_SIZE = Integer.parseInt(properties.getProperty("face.feature.size").trim());
    private static int FACE_FEATURE_VERSION_SIZE = Integer.parseInt(properties.getProperty("face.version.size").trim());
    private static int FACE_FEATURE_TAIL_SIZE = Integer.parseInt(properties.getProperty("face.tail.size").trim());


    //获取cpu核数
    private static int PROCESSOR_NUM = Runtime.getRuntime().availableProcessors();
    //根据核数创建线程池
    private static ExecutorService POOLS = Executors.newFixedThreadPool(PROCESSOR_NUM);

    static {
        try {
            startTime = sdf.parse(properties.getProperty("start.time").trim()).getTime();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public List<CollisionAyalyzeResult> collisionAyalyze(CollisionAyalyzeInputRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        if (debug) {
            logger.info("[logId:" + uuid + "]CollisionAyalyzeInputRecord is :" + record.toString());
        }
        List<CollisionAyalyzeResult> resultList = new ArrayList<>();
        if (record == null) {
            logger.error("collisionAyalyze record is null!");
            return resultList;
        }
        List<RegionRecord> regionRecords = record.getRegionRecords();
        if (regionRecords == null || regionRecords.size() == 0) {
            logger.error("collisionAyalyze record.regionRecords is null!");
            return resultList;
        }
        if (record.getDistence() == 0) {
            record.setDistence(minScore);
        }
        final int distance = record.getDistence();
        long start = System.currentTimeMillis();

        final Map<String, List<String>> matrix = new ConcurrentHashMap<String, List<String>>();
        final Map<String, List<String>> matrix1 = new ConcurrentHashMap<String, List<String>>();
        final List<List<FaceFeature>> list = new ArrayList<List<FaceFeature>>();
        for (int i = 0; i < regionRecords.size(); i++) {
            List<FaceFeature> faceFeatures = CollisionAyalyzeService.findFaceByRegionRecord(regionRecords.get(i), POOLS);
            if (faceFeatures != null && faceFeatures.size() != 0) {
                logger.info("region " + i + " face_data count is :" + faceFeatures.size());
                list.add(faceFeatures);
            }
        }
        int regionSize = list.size();
        if(regionSize > 8){
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < list.size() - 1; i++) {
                final List<FaceFeature> partOne = list.get(i);
                logger.info("srcPartOne size is : " + partOne.size());
                for (int x = i + 1; x < list.size(); x++) {
                    final List<FaceFeature> partTwo = list.get(i);
                    logger.info("srcPartTwo size is : " + partTwo.size());
                    Future<Boolean> future = POOLS.submit(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            for (FaceFeature f1 : partOne) {
                                List<String> temp1 = matrix.get(f1.getJlbh()) == null ? new ArrayList<String>() : matrix.get(f1.getJlbh());
                                List<String> temp2 = matrix.get(f1.getJlbh()) == null ? new ArrayList<String>() : matrix.get(f1.getJlbh());
                                for (FaceFeature f2 : partTwo) {
                                    float score = FaceFeatureVerify.verify(f1.getFeature(), 0, f2.getFeature(), 0);
                                    if (score >= distance) {
                                        temp1.add(f2.getJlbh());
                                    }
                                }
                                for (FaceFeature f2 : partOne) {
                                    float score = FaceFeatureVerify.verify(f1.getFeature(), 0, f2.getFeature(), 0);
                                    if (score >= distance) {
                                        temp2.add(f2.getJlbh());
                                    }
                                }
                                if (temp1.size() > 0) {
                                    matrix.put(f1.getJlbh(), temp1);
                                }
                                if (temp2.size() > 0) {
                                    matrix1.put(f1.getJlbh(), temp2);
                                }
                            }
                            return true;
                        }
                    });
                    futures.add(future);
                }
            }
            for (Future<Boolean> future : futures) {
                try {
                    Boolean queue = future.get();
                    if (!queue) {
                        logger.warn("collisionAyalyze result is lackly!");
                    }
                } catch (InterruptedException e) {
                    logger.warn(e.getMessage(), e);
                } catch (ExecutionException e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }else{
            for (int i = 0; i < list.size() - 1; i++) {
                List<Future<Boolean>> futures = new ArrayList<>();
                final List<FaceFeature> srcPartOne = list.get(i);
                logger.info("srcPartOne size is : " + srcPartOne.size());
                int split = srcPartOne.size() / PROCESSOR_NUM;
                for (int m = 0; m < PROCESSOR_NUM; m++) {
                    List<FaceFeature> partOne_tmp;
                    if (m == (PROCESSOR_NUM - 1)) {
                        partOne_tmp = srcPartOne.subList(m * split, srcPartOne.size());
                        logger.info("partOne_tmp start and end is : " + m * split + "," + srcPartOne.size() + "size is :" + partOne_tmp.size());
                    } else {
                        partOne_tmp = srcPartOne.subList(m * split, (m + 1) * split);
                        logger.info("partOne_tmp start and end is : " + (m * split) + "," + ((m + 1) * split) + "size is :" + partOne_tmp.size());
                    }
                    final List<FaceFeature> partOne = partOne_tmp;
                    for (int x = i + 1; x < list.size(); x++) {
                        final List<FaceFeature> partTwo = list.get(x);
                        Future<Boolean> future = POOLS.submit(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                for (FaceFeature f1 : partOne) {
                                    List<String> temp1 = matrix.get(f1.getJlbh()) == null ? new ArrayList<String>() : matrix.get(f1.getJlbh());
                                    List<String> temp2 = matrix.get(f1.getJlbh()) == null ? new ArrayList<String>() : matrix.get(f1.getJlbh());
                                    for (FaceFeature f2 : partTwo) {
                                        float score = FaceFeatureVerify.verify(f1.getFeature(), 0, f2.getFeature(), 0);
                                        if (score >= distance) {
                                            temp1.add(f2.getJlbh());
                                        }
                                    }
                                    for (FaceFeature f2 : srcPartOne) {
                                        float score = FaceFeatureVerify.verify(f1.getFeature(), 0, f2.getFeature(), 0);
                                        if (score >= distance) {
                                            temp2.add(f2.getJlbh());
                                        }
                                    }
                                    if (temp1.size() > 0) {
                                        matrix.put(f1.getJlbh(), temp1);
                                    }
                                    if (temp2.size() > 0) {
                                        matrix1.put(f1.getJlbh(), temp2);
                                    }
                                }
                                return true;
                            }
                        });
                        futures.add(future);
                    }
                }
                for (Future<Boolean> future : futures) {
                    try {
                        Boolean queue = future.get();
                        if (!queue) {
                            logger.warn("collisionAyalyze result is lackly!");
                        }
                    } catch (InterruptedException e) {
                        logger.warn(e.getMessage(), e);
                    } catch (ExecutionException e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        }
        for (int i = 2; i <= regionSize; i++) {
            List<List<String>> tempList;
            if (i == 2) {
                tempList = CollisionAyalyzeService.getSrcMap(matrix);
            } else {
                tempList = CollisionAyalyzeService.convert(matrix, resultList.get(resultList.size() - 1).getLogNums(), i, record.getRegionCount());
            }
            resultList.add(new CollisionAyalyzeResult(tempList, i));
        }
        List<CollisionAyalyzeResult> removeList = new ArrayList<>();
        for (CollisionAyalyzeResult res : resultList) {
            if (res.getRegionCount() < record.getRegionCount()) {
                removeList.add(res);
            }
        }
        resultList.removeAll(removeList);
        long end = System.currentTimeMillis();
        logger.info("ayalyze used time :" + (end - start) + "ms!");
        for (CollisionAyalyzeResult res : resultList) {
            if (res.getLogNums() == null || res.getLogNums().size() == 0) {
                continue;
            }
            // 去重
            List<List<String>> tmpLogNums = new ArrayList<>();
            for (List<String> src : res.getLogNums()) {
                boolean flag = true;
                for (List<String> tmp : tmpLogNums) {
                    for (int i = 0; i < src.size(); i++) {
                        if (matrix1.containsKey(tmp.get(i)) && matrix1.get(tmp.get(i)).contains(src.get(i))) {
                            flag = false;
                            break;
                        }
                    }
                    if (!flag) {
                        break;
                    }
                }
                if (flag) {
                    tmpLogNums.add(src);
                }
            }
            res.setLogNums(tmpLogNums);
            // 将过多的数据删除
            if (res.getLogNums().size() > 100) {
                res.setLogNums(res.getLogNums().subList(0, 100));
            }
        }
        if (debug) {
            logger.info("delete disable data used time :" + (System.currentTimeMillis() - end) + "ms!");
            logger.info("[logId:" + uuid + "]collisionAyalyze used time " + (System.currentTimeMillis() - startTime) + "ms!");
            logger.info("[logId:" + uuid + "]CollisionAyalyzeResult size is :" + resultList.size());
        }
        return resultList;
    }

    @Override
    public List<AccompanyAyalyzeResult> accompanyAyalyze(AccompanyAyalyzeInputRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        List<AccompanyAyalyzeResult> result = new ArrayList<>();
        if (debug) {
            logger.info("[logId:" + uuid + "]AccompanyAyalyzeInputRecord is :" + record.toString());
        }
        if (record == null || record.getAccompanyCount() < 1 || record.getAccompanyTime() == 0
                || record.getEndTime() == 0 || record.getStartTime() == 0
                || record.getFeature() == null || record.getCameraId() == null) {
            logger.error("please checked your accompanyAyalyze request args!");
            return result;
        }
        if (record.getStartTime() < startTime) {
            record.setStartTime(startTime);
        }
        if (record.getDistence() == 0) {
            record.setDistence(minScore);
        }
        if (record.getFeature().length > FACE_FEATURE_SIZE) {
            byte[] feature = new byte[FACE_FEATURE_SIZE];
            System.arraycopy(record.getFeature(), FACE_FEATURE_VERSION_SIZE, feature, 0, FACE_FEATURE_SIZE);
            record.setFeature(feature);
        } else if (record.getFeature().length < FACE_FEATURE_SIZE) {
            logger.error("record.feature.length is too short,require:[" + FACE_FEATURE_SIZE + "],find:[" + record.getFeature().length + "]return");
            return result;
        }
        result = AccompanyAyalyzeService.executeQuery(record, POOLS);
        if (debug) {
            logger.info("[logId:" + uuid + "]accompanyAyalyze used time " + (System.currentTimeMillis() - startTime) + "ms!");
            logger.info("[logId:" + uuid + "]AccompanyAyalyzeResults size is :" + result.size());
        }
        return result;
    }

    @Override
    public List<AccompanyAyalyzeResult> frequentPass(FrequentPassInputRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        if (debug) {
            logger.info("[logId:" + uuid + "]FrequentPassInputRecord is :" + record.toString());
        }
        List<AccompanyAyalyzeResult> list = new ArrayList<AccompanyAyalyzeResult>();
        if (record.getCameraId() == null) {
            logger.error("frequentPass cameraId is null,please checked your request args!");
            if (debug) {
                logger.info("[logId:" + uuid + "]frequentPass used time " + (System.currentTimeMillis() - startTime) + "ms!");
                logger.info("[logId:" + uuid + "]FrequentPassInputResults size is :" + list.size());
            }
            return list;
        }
        if (record.getDistence() == 0) {
            record.setDistence(minScore);
        }
        if(record.getStartTime() < startTime) {
            record.setStartTime(startTime);
        }
        ArrayList<ArrayList<String>> result = FrequentPassService.executeQuery(record, POOLS);
        for (ArrayList<String> arrayList : result) {
            if (arrayList.size() >= record.getPassCount()) {
                list.add(new AccompanyAyalyzeResult(arrayList, arrayList.size()));
            }
        }
        if (debug) {
            logger.info("[logId:" + uuid + "]frequentPass used time " + (System.currentTimeMillis() - startTime) + "ms!");
            logger.info("[logId:" + uuid + "]FrequentPassInputResults size is :" + list.size());
        }
        return list;
    }

    @Override
    public List<FaceTrackingResult> faceTracking(SearchImgByImgInputRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        if (debug) {
            logger.info("[logId:" + uuid + "]FaceTrackingRecord is :" + record.toString());
        }
        List<FaceTrackingResult> resultList = new ArrayList<>();
        if (record == null || record.getFeature() == null || record.getCameraId() == null) {
            logger.error("faceTracking record info is null,return");
            if (debug) {
                logger.info("[logId:" + uuid + "]faceTracking used time " + (System.currentTimeMillis() - startTime) + "ms!");
                logger.info("[logId:" + uuid + "]FaceTrackingResult is :" + resultList.toString());
            }
            return resultList;
        }
        if (record.getStartTime() < startTime) {
            record.setStartTime(startTime);
        }
        if (record.getDistance() == 0) {
            record.setDistance(minScore);
        }
        List<SearchImgResult> results = searchFaceImgByImg(record);
        logger.info(results.toString());
        resultList = FaceTrackingService.getResult(results);
        if (debug) {
            logger.info("[logId:" + uuid + "]faceTracking used time " + (System.currentTimeMillis() - startTime) + "ms!");
            logger.info("[logId:" + uuid + "]FaceTrackingResult size is :" + resultList.size());
        }
        return resultList;
    }

    @Override
    public List<AccompanyAyalyzeResult> searchHumanByTrack(SearchHumanByTrackInputRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        if (debug) {
            logger.info("[logId:" + uuid + "]SearchHumanByTrackInputRecord is :" + record.toString());
        }
        List<AccompanyAyalyzeResult> list = new ArrayList<AccompanyAyalyzeResult>();
        if (record.getCameraId() == null) {
            logger.error("SearchHumanByTrackInputRecord cameraId is null,please checked your request args!");
            if (debug) {
                logger.info("[logId:" + uuid + "]SearchHumanByTrackInputRecord used time " + (System.currentTimeMillis() - startTime) + "ms!");
                logger.info("[logId:" + uuid + "]SearchHumanByTrackInputRecord size is :" + list.size());
            }
            return list;
        }
        if (record.getDistence() == 0) {
            record.setDistence(minScore);
        }
        if(record.getStartTime() < startTime) {
            record.setStartTime(startTime);
        }
        ArrayList<ArrayList<String>> result = SearchHumanByTrack.executeQuery(record, POOLS);
        for (ArrayList<String> arrayList : result) {
            list.add(new AccompanyAyalyzeResult(arrayList, arrayList.size()));
        }
        if (debug) {
            logger.info("[logId:" + uuid + "]SearchHumanByTrackInputRecord used time " + (System.currentTimeMillis() - startTime) + "ms!");
            logger.info("[logId:" + uuid + "]SearchHumanByTrackInputRecord size is :" + list.size());
        }
        return list;
    }

    @Override
    public List<SearchImgResult> searchFaceImgByImg(SearchImgByImgInputRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        List<SearchImgResult> resultList = new LinkedList<>();
        logger.info("searchFaceImgByImg id : " + uuid + ", "  + record.toString());
        // 参数校验
        if (record.getStartTime() <= 0) {
            record.setStartTime(startTime);
        }
        if (record.getStartTime() > record.getEndTime() || null == record.getFeature()) {
            logger.warn("error param, please check!");
            return resultList;
        }
        if (record.getFeature().length < FACE_FEATURE_SIZE) {
            logger.error("error feature size : " + record.getFeature().length);
        }

        if (record.getFeature().length > FACE_FEATURE_SIZE) {
            if (record.getFeature().length == FACE_FEATURE_SIZE + FACE_FEATURE_VERSION_SIZE + FACE_FEATURE_TAIL_SIZE) {
                logger.warn("feature is not pure feature, will cut pure feature from it, feature size is : " + record.getFeature().length);
                byte[] feature = new byte[1024];
                System.arraycopy(record.getFeature(), FACE_FEATURE_VERSION_SIZE, feature, 0, 1024);
                record.setFeature(feature);
            } else {
                logger.error("error feature, size is : " + record.getFeature().length);
            }
        }

        if (record.getDistance() <= 0) {
            record.setDistance(minScore);
        }
        long start = System.currentTimeMillis();
        resultList = SearchFaceImgByImgService.executeQuery(record, uuid);
        logger.info("searchFaceImgByImg id : " + uuid + " completed, use time : " + (System.currentTimeMillis() - start) + " ms, result list size is : " + resultList.size());
        return resultList;
    }

    @Override
    public List<SearchImgResult> searchFaceImgByLog(SearchImgByLogInputRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        logger.info("searchFaceImgByLog id : " + uuid + ", "  + record.toString());
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
                results = searchFaceImgByImg(searchImgByImgInputRecord);
            }
        }
        logger.info("SearchFaceImgByLog complete, id : " + uuid + ", used_time[ms] : " + (System.currentTimeMillis() - start) + ", result list size is : " + results.size());
        return results;
    }

    @Override
    public double featureVerify(FeatureVerifyRecord record) throws TException {
        // 该功能已经移到人脸库服务里
        return 0;
    }

    @Override
    public SearchImgByAssistResponse searchFaceImgByAssist(SearchImgByAssistInputRecord record) throws TException {
        if (debug) {
            logger.info("searchFaceImgByAssist input record is " + record.toString());
        }
        SearchImgByAssistResponse response = new SearchImgByAssistResponse();
        if ((null == record.resultList || record.resultList.size() == 0)
                && (null == record.assist || record.assist.size() == 0)) {
            response.setRCode(StatusCode.ERROR_PARAM);
            response.setMessage(String.format("param error resultList size is %d, assistList size is %d", record.getResultList().size(), record.getAssist().size()));
            return response;
        }
        if (record.getStartTime() > record.getEndTime()) {
            response.setRCode(StatusCode.ERROR_PARAM);
            response.setMessage(String.format("param error, starttime can not large than endtime resultList, starttime is %d, endtime is %d",
                    record.getStartTime(), record.getEndTime()));
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
            long start = System.currentTimeMillis();
            if (null == record.resultList || record.resultList.size() == 0) {
                //单张图片只做一次以图搜图
                if (record.getAssist().size() == 1) {
                    SearchImgByImgInputRecord input = new SearchImgByImgInputRecord();
                    input.setFeature(record.getAssist().get(record.getAssist().size() - 1));
                    input.setStartTime(record.getStartTime());
                    input.setEndTime(record.getEndTime());
                    input.setCameraId(record.getCameraID());
                    input.setCount(record.getCount());
                    input.setDistance(record.getAssistDistance());
                    List<SearchImgResult> searchImgRes = searchFaceImgByImg(input);
                    response.setRCode(StatusCode.OK);
                    response.setResult(searchImgRes);
                    response.setMessage("SUCCESS");
                } else { //多张图片分别作以图搜图再进行辅助检索
                    //组装多个以图搜图条件
                    List<SearchImgByImgInputRecord> inputList = new ArrayList<>();
                    for (int i = 0; i < record.getAssist().size(); ++i) {
                        inputList.add(new SearchImgByImgInputRecord(record.getStartTime(), record.getEndTime(), record.getCameraID(),
                                record.getAssist().get(i), record.getAssistDistance(), record.getCount()));
                    }
                    //进行n次以图搜图
                    List<List<SearchImgResult>> searchImgResList = new ArrayList<>();
                    for (SearchImgByImgInputRecord input : inputList) {
                        List<SearchImgResult> tmpRes = searchFaceImgByImg(input);
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
                    for (int i = 1; i < searchImgResList.size(); ++i) {
                        List<com.netposa.poseidon.bean.SearchImgResult> assistList = new ArrayList<>();
                        for (int j = 0; j < searchImgResList.get(i).size(); ++j) {
                            assistList.add(new com.netposa.poseidon.bean.SearchImgResult(searchImgResList.get(i).get(j).getLogNum(), searchImgResList.get(i).get(j).getCameraId(),
                                    searchImgResList.get(i).get(j).getGatherTime(), searchImgResList.get(i).get(j).getScore()));
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
                List<SearchImgResult> searchImgRes = searchFaceImgByImg(input);
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
                ResponseResult res = AuxiliayRetirval.auxiliayRetirval(srcList, assistList);
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
        }else {
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
                    List<SearchImgResult> searchImgRes = searchFaceImgByImg(input);
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
                        List<SearchImgResult> tmpRes = searchFaceImgByImg(excludeinput);
                        logger.info(String.format("execute once search imgage, feature length is %s, result length is %s", excludeinput.getFeature().length, tmpRes.size()));
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
                        List<SearchImgResult> tmpRes = searchFaceImgByImg(input);
                        logger.info(String.format("execute once search imgage, feature length is %s, result length is %s", input.getFeature().length, tmpRes.size()));
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
                        List<SearchImgResult> tmpRes = searchFaceImgByImg(excludeinput);
                        logger.info(String.format("execute once search imgage, feature length is %s, result length is %s", excludeinput.getFeature().length, tmpRes.size()));
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
                List<SearchImgResult> searchImgRes = searchFaceImgByImg(input);
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

}
