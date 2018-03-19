package com.netposa.poseidon.face.main;

import com.netposa.poseidon.bean.ResponseResult;
import com.netposa.poseidon.connection.*;
import com.netposa.poseidon.face.bean.CacheBean;
import com.netposa.poseidon.face.bean.CacheFaceFeature;
import com.netposa.poseidon.face.bean.FaceFeature;
import com.netposa.poseidon.face.init.LoadPropers;
import com.netposa.poseidon.face.rpc.*;
import com.netposa.poseidon.face.service.*;
import com.netposa.poseidon.face.util.FaceFeatureVerify;
import com.netposa.poseidon.face.util.FaceFeatureVerifyDg;
import com.netposa.poseidon.face.util.HbaseUtil;
import org.apache.hadoop.hbase.client.Result;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class FaceFeatureDataAnalyzeRpcServiceHandler implements FaceFeatureDataAnalyzeRpcService.Iface {
    private static final Logger LOG = LoggerFactory.getLogger(FaceFeatureDataAnalyzeRpcServiceHandler.class);
    private static int MIN_SCORE = Integer.parseInt(LoadPropers.getProperties().getProperty("default.score", "90").trim());// 相似度阈值
    private static boolean DEBUG = Boolean.parseBoolean(LoadPropers.getProperties().getProperty("service.process.debug.enable", "true").trim());// 调试日志开关
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
    private static final int FACE_FEATURE_VERSION_SIZE = Integer.parseInt(LoadPropers.getProperties().getProperty("face.version.size", "12").trim());
    private static final int FACE_FEATURE_SIZE = Integer.parseInt(LoadPropers.getProperties().getProperty("face.feature.size", "512").trim());
    private static CacheConfiguration<String, CacheBean> cacheCfg = new CacheConfiguration<>();
    private static final String ALGORITHM = LoadPropers.getProperties().getProperty("algorithm.type", "netposa");
    private static final String IGNITE_NAME = LoadPropers.getProperties().getProperty("ignite.name");
    //获取cpu核数
    private static int PROCESSOR_NUM = Runtime.getRuntime().availableProcessors();
    private static int REGION_NUM = Integer.parseInt(LoadPropers.getProperties().getProperty("hbase.split.region.num", "50"));
    //根据核数创建线程池
    private static ExecutorService POOLS = Executors.newFixedThreadPool(PROCESSOR_NUM);
    private static long STARTTIME = 0;

    static {
        try {
            STARTTIME = sdf1.parse(LoadPropers.getProperties().getProperty("start.time", "2017-01-01").trim()).getTime();
            cacheCfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public List<SearchImgResult>  searchFaceImgByImg(SearchImgByImgInputRecord record) throws TException {
        long startTime = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString();
        if (DEBUG) {
            LOG.info("[logId:" + uuid + "]SearchFaceImgByImgInputRecord is :" + record.toString());
        }
        List<SearchImgResult> results = new ArrayList<>();

        if (record == null || record.getFeature() == null) {
            LOG.error("searchFaceImgByImg record info is null,return");
            return results;
        }
        if (record.getFeature().length > FACE_FEATURE_SIZE) {
            byte[] feature = new byte[FACE_FEATURE_SIZE];
            System.arraycopy(record.getFeature(), FACE_FEATURE_VERSION_SIZE, feature, 0, FACE_FEATURE_SIZE);
            record.setFeature(feature);
        } else if (record.getFeature().length < FACE_FEATURE_SIZE){
            LOG.error("record.feature.length is  too short,require:[" + FACE_FEATURE_SIZE + "],find:[" + record.getFeature().length + "]return");
            return results;
        }
        if (record.getStartTime() < STARTTIME) {
            record.setStartTime(STARTTIME);
        }
        if (record.getDistance() == 0) {
            record.setDistance(MIN_SCORE);
        }

        try {
            List<String> dates = new ArrayList<>();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(record.getStartTime());
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            while (calendar.getTimeInMillis() <= record.getEndTime()) {
                dates.add(String.format(IGNITE_NAME + "_FACE_%s", sdf.format(calendar.getTime())));
                calendar.add(Calendar.DATE, 1);
            }
            if (dates != null && dates.size() > 0) {
//                long startTime1 = System.currentTimeMillis();
                results = SearchFaceImgByImgService.executeQuery(record, dates);
                if (DEBUG) {
//                    LOG.info("receive record and before executeQuery used time "+ (System.currentTimeMillis() - startTime));
                    LOG.info("[logId:" + uuid + "]SearchFaceImgByImg used time " + (System.currentTimeMillis() - startTime) + "ms!results count is :" + results.size());
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return results;
    }

    @Override
    public List<SearchImgResult>  searchFaceImgByLog(SearchImgByLogInputRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        if (DEBUG) {
            LOG.info("[logId:" + uuid + "]SearchFaceImgByLogInputRecord is :" + record.toString());
        }
        List<SearchImgResult> results = new ArrayList<>();
        if (record == null || record.getLogNum() == null || record.getDate() == null) {
            LOG.error("searchFaceImgByLog record info is null,return");
            return results;
        }
        SearchImgByImgInputRecord input = new SearchImgByImgInputRecord();
        IgniteCache<String, CacheFaceFeature> cache = Ignition.ignite(IGNITE_NAME).cache(IGNITE_NAME + "_FACE_" + sdf.format(new Date(Long.parseLong(record.getDate()))));
        if (cache != null && cache.containsKey(record.getLogNum())) {
            LOG.error("logNum[" + record.getLogNum() + "]  found in the cache");
            input.setFeature(cache.get(record.getLogNum()).getFeature());
        } else {
            LOG.error("logNum[" + record.getLogNum() + "]  is not found in the cache, now to search hbase");
            SearchFeatureByLogInputRecord searchInput = new SearchFeatureByLogInputRecord();
            searchInput.setLogNum(record.getLogNum());
            searchInput.setGatherTime(Long.parseLong(record.getDate()));
            SearchFeatureByLogResponse response = searchFeatureByLog(searchInput);
            if (response.rCode == StatusCode.OK) {
                if (null != response.getFeature()) {
                    input.setFeature(response.getFeature());
                } else {
                    LOG.error("logNum[" + record.getLogNum() + "]  is not found in the hbase");
                    return results;
                }
            }
        }
        input.setStartTime(record.getStartTime());
        input.setEndTime(record.getEndTime());
        input.setCameraId(record.getCameraId());
        input.setCount(record.getCount());
        input.setDistance(record.getDistance());
        results = searchFaceImgByImg(input);
        if (DEBUG) {
            LOG.info("[logId:" + uuid + "]SearchFaceImgByLog used time " + (System.currentTimeMillis() - startTime));
        }
        return results;
    }

    @Override
    public SearchImgByAssistResponse searchFaceImgByAssist(SearchImgByAssistInputRecord record) throws TException {
        if (DEBUG) {
            LOG.info("searchFaceImgByAssist input record is " + record.toString());
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
        if (record.getStartTime() < STARTTIME) {
            record.setStartTime(STARTTIME);
        }
        if (record.getAssistDistance() <= 0) {
            record.setAssistDistance(MIN_SCORE);
        }
        if (null != record.exclude && record.exclude.size() > 0 && record.getExcludeDistance() <= 0) {
            record.setExcludeDistance(MIN_SCORE);
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
                        LOG.info(String.format("execute once search image, feature length is %s, result length is %s", input.getFeature().length, tmpRes.size()));
                        searchImgResList.add(tmpRes);
                    }
                    long endSearchImg = System.currentTimeMillis();
                    LOG.info(String.format("complete once searchImgByImg, execute time is : %s ms, result size is : %s", (endSearchImg - start), searchImgResList.size()));
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
                LOG.info(String.format("complete once searchImgByImg, execute time is : %s ms, result size is : %s", (endSearchImg - start), assistList.size()));
                //解析上次结果，组装数据
                List<SearchImgResult> src = record.getResultList();
                List<com.netposa.poseidon.bean.SearchImgResult> srcList = new ArrayList<>();
                for (int i = 0; i < src.size(); ++i) {
                    srcList.add(new com.netposa.poseidon.bean.SearchImgResult(src.get(i).getLogNum(), src.get(i).getCameraId(), src.get(i).getGatherTime(), src.get(i).getScore()));
                }
                ResponseResult res = AuxiliayRetirval.auxiliayRetirval(srcList, assistList);
                long endAssistSearch = System.currentTimeMillis();
                LOG.info(String.format("complete assist search, execute time is : %s ms, result size is : %s", (endAssistSearch - endSearchImg), res.getList().size()));
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
            LOG.info(String.format("complete multi Img search, execute time is : %s ms, result size is : %s", (System.currentTimeMillis() - start), response.getResult().size()));
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
                        LOG.info(String.format("execute once search imgage, feature length is %s, result length is %s", excludeinput.getFeature().length, tmpRes.size()));
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
                        LOG.info(String.format("execute once search imgage, feature length is %s, result length is %s", input.getFeature().length, tmpRes.size()));
                        searchImgResList.add(tmpRes);
                    }
                    long endSearchImg = System.currentTimeMillis();
                    LOG.info(String.format("complete once searchImgByImg, execute time is : %s ms, result size is : %s", (endSearchImg - start), searchImgResList.size()));
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
                        LOG.info(String.format("execute once search imgage, feature length is %s, result length is %s", excludeinput.getFeature().length, tmpRes.size()));
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
                LOG.info(String.format("complete once searchImgByImg, execute time is : %s ms, result size is : %s", (endSearchImg - start), assistList.size()));
                //解析上次结果，组装数据
                List<SearchImgResult> src = record.getResultList();
                List<com.netposa.poseidon.bean.SearchImgResult> srcList = new ArrayList<>();
                for (int i = 0; i < src.size(); ++i) {
                    srcList.add(new com.netposa.poseidon.bean.SearchImgResult(src.get(i).getLogNum(), src.get(i).getCameraId(), src.get(i).getGatherTime(), src.get(i).getScore()));
                }
                ResponseResult res = ReverseAuxiliary.reverseRetirval(srcList, assistList);
                long endAssistSearch = System.currentTimeMillis();
                LOG.info(String.format("complete assist search, execute time is : %s ms, result size is : %s", (endAssistSearch - endSearchImg), res.getList().size()));
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
            LOG.info(String.format("complete multi Img search, execute time is : %s ms, result size is : %s", (System.currentTimeMillis() - start), response.getResult().size()));
        }
        return response;
    }

    @Override
    public SearchFeatureByLogResponse searchFeatureByLog(SearchFeatureByLogInputRecord record) throws TException {
        IgniteCache<String, Properties> cache = Ignition.ignite(IGNITE_NAME).cache(IGNITE_NAME + "_FACE_GLOBAL");
        Properties prop = cache.get("SERVER");
        SearchFeatureByLogResponse response = new SearchFeatureByLogResponse();
        if (null == record.getLogNum() || "".equals(record.getLogNum())) {
            response.setRCode(StatusCode.ERROR_PARAM);
            response.setMessage("input param logNum error, please check!");
            return response;
        }
        long start = System.currentTimeMillis();
        for (int i = 0; i < REGION_NUM; ++i) {
            String rowKey = String.format("%02d", i) + record.getGatherTime() + record.getLogNum();
            Result res = HbaseUtil.getOneRow(prop.getProperty("face.table.name"), rowKey);
            if (null != res && null !=  res.getValue("cf".getBytes(), "feature".getBytes())) {
                response.setRCode(StatusCode.OK);
                response.setFeature(res.getValue("cf".getBytes(), "feature".getBytes()));
                break;
            }
        }
        LOG.info(String.format("SearchFeatureByLog complete, execute time is %s ms", (System.currentTimeMillis() - start)));
        return response;
    }

    @Override
    public double featureVerify(FeatureVerifyRecord record) throws TException {
        double score;
        long startTime = System.currentTimeMillis();
        if (record == null || record.getFeature1() != null && record.getFeature2() != null) {
            LOG.info(String.format("feature1.length is[%d],feature2.length is [%d]", record.getFeature1().length, record.getFeature2().length));
        } else {
            LOG.error("feature is null,exit!");
            return -1;
        }
        if ("netposa".equalsIgnoreCase(ALGORITHM)) {
            score = FaceFeatureVerify.verify(record.getFeature1(), FACE_FEATURE_VERSION_SIZE, record.getFeature2(), FACE_FEATURE_VERSION_SIZE);
        } else {
            score = FaceFeatureVerifyDg.verify(record.getFeature1(), FACE_FEATURE_VERSION_SIZE, record.getFeature2(), FACE_FEATURE_VERSION_SIZE);
        }
        if (DEBUG) {
            LOG.info("face feature verify score is [" + score + "],used time " + (System.currentTimeMillis() - startTime) + " ms!");
        }
        return score;
    }

    @Override
    public List<CollisionAyalyzeResult> collisionAyalyze(CollisionAyalyzeInputRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        if (DEBUG) {
            LOG.info("[logId:" + uuid + "]CollisionAyalyzeInputRecord is :" + record.toString());
        }
        List<CollisionAyalyzeResult> resultList = new ArrayList<>();
        if (record == null) {
            LOG.error("collisionAyalyze record is null!");
            return resultList;
        }
        List<RegionRecord> regionRecords = record.getRegionRecords();
        if (regionRecords == null || regionRecords.size() == 0) {
            LOG.error("collisionAyalyze record.regionRecords is null!");
            return resultList;
        }
        if (record.getDistence() == 0) {
            record.setDistence(MIN_SCORE);
        }
        final int distance = record.getDistence();
        long start = System.currentTimeMillis();

        final Map<String, List<String>> matrix = new ConcurrentHashMap<String, List<String>>();
        final Map<String, List<String>> matrix1 = new ConcurrentHashMap<String, List<String>>();
        final List<List<FaceFeature>> list = new ArrayList<List<FaceFeature>>();
        for (int i = 0; i < regionRecords.size(); i++) {
            List<FaceFeature> faceFeatures = CollisionAyalyzeService.findFaceByRegionRecord(regionRecords.get(i), POOLS);
            if (faceFeatures != null && faceFeatures.size() != 0) {
                LOG.info("region " + i + " face_data count is :" + faceFeatures.size());
                list.add(faceFeatures);
            }
        }
        int regionSize = list.size();
        if(regionSize > 8){
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < list.size() - 1; i++) {
                final List<FaceFeature> partOne = list.get(i);
                LOG.info("srcPartOne size is : " + partOne.size());
                for (int x = i + 1; x < list.size(); x++) {
                    final List<FaceFeature> partTwo = list.get(i);
                    LOG.info("srcPartTwo size is : " + partTwo.size());
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
                        LOG.warn("collisionAyalyze result is lackly!");
                    }
                } catch (InterruptedException e) {
                    LOG.warn(e.getMessage(), e);
                } catch (ExecutionException e) {
                    LOG.warn(e.getMessage(), e);
                }
            }
        }else{
            for (int i = 0; i < list.size() - 1; i++) {
                List<Future<Boolean>> futures = new ArrayList<>();
                final List<FaceFeature> srcPartOne = list.get(i);
                LOG.info("srcPartOne size is : " + srcPartOne.size());
                int split = srcPartOne.size() / PROCESSOR_NUM;
                for (int m = 0; m < PROCESSOR_NUM; m++) {
                    List<FaceFeature> partOne_tmp;
                    if (m == (PROCESSOR_NUM - 1)) {
                        partOne_tmp = srcPartOne.subList(m * split, srcPartOne.size());
                        LOG.info("partOne_tmp start and end is : " + m * split + "," + srcPartOne.size() + "size is :" + partOne_tmp.size());
                    } else {
                        partOne_tmp = srcPartOne.subList(m * split, (m + 1) * split);
                        LOG.info("partOne_tmp start and end is : " + (m * split) + "," + ((m + 1) * split) + "size is :" + partOne_tmp.size());
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
                            LOG.warn("collisionAyalyze result is lackly!");
                        }
                    } catch (InterruptedException e) {
                        LOG.warn(e.getMessage(), e);
                    } catch (ExecutionException e) {
                        LOG.warn(e.getMessage(), e);
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
        LOG.info("ayalyze used time :" + (end - start) + "ms!");
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
        if (DEBUG) {
            LOG.info("delete disable data used time :" + (System.currentTimeMillis() - end) + "ms!");
            LOG.info("[logId:" + uuid + "]collisionAyalyze used time " + (System.currentTimeMillis() - startTime) + "ms!");
            LOG.info("[logId:" + uuid + "]CollisionAyalyzeResult size is :" + resultList.size());
        }
        return resultList;
    }

    @Override
    public List<AccompanyAyalyzeResult> accompanyAyalyze(AccompanyAyalyzeInputRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        List<AccompanyAyalyzeResult> result = new ArrayList<>();
        if (DEBUG) {
            LOG.info("[logId:" + uuid + "]AccompanyAyalyzeInputRecord is :" + record.toString());
        }
        if (record == null || record.getAccompanyCount() < 1 || record.getAccompanyTime() == 0
                || record.getEndTime() == 0 || record.getStartTime() == 0
                || record.getFeature() == null || record.getCameraId() == null) {
            LOG.error("please checked your accompanyAyalyze request args!");
            return result;
        }
        if (record.getStartTime() < STARTTIME) {
            record.setStartTime(STARTTIME);
        }
        if (record.getDistence() == 0) {
            record.setDistence(MIN_SCORE);
        }
        if (record.getFeature().length > FACE_FEATURE_SIZE) {
            byte[] feature = new byte[FACE_FEATURE_SIZE];
            System.arraycopy(record.getFeature(), FACE_FEATURE_VERSION_SIZE, feature, 0, FACE_FEATURE_SIZE);
            record.setFeature(feature);
        } else if (record.getFeature().length < FACE_FEATURE_SIZE) {
            LOG.error("record.feature.length is too short,require:[" + FACE_FEATURE_SIZE + "],find:[" + record.getFeature().length + "]return");
            return result;
        }
        result = AccompanyAyalyzeService.executeQuery(record, POOLS);
        if (DEBUG) {
            LOG.info("[logId:" + uuid + "]accompanyAyalyze used time " + (System.currentTimeMillis() - startTime) + "ms!");
            LOG.info("[logId:" + uuid + "]AccompanyAyalyzeResults size is :" + result.size());
        }
        return result;
    }

    @Override
    public List<AccompanyAyalyzeResult> frequentPass(FrequentPassInputRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        if (DEBUG) {
            LOG.info("[logId:" + uuid + "]FrequentPassInputRecord is :" + record.toString());
        }
        List<AccompanyAyalyzeResult> list = new ArrayList<AccompanyAyalyzeResult>();
        if (record.getCameraId() == null) {
            LOG.error("frequentPass cameraId is null,please checked your request args!");
            if (DEBUG) {
                LOG.info("[logId:" + uuid + "]frequentPass used time " + (System.currentTimeMillis() - startTime) + "ms!");
                LOG.info("[logId:" + uuid + "]FrequentPassInputResults size is :" + list.size());
            }
            return list;
        }
        if (record.getDistence() == 0) {
            record.setDistence(MIN_SCORE);
        }
        if(record.getStartTime() < STARTTIME) {
            record.setStartTime(STARTTIME);
        }
        ArrayList<ArrayList<String>> result = FrequentPassService.executeQuery(record, POOLS);
        for (ArrayList<String> arrayList : result) {
            if (arrayList.size() >= record.getPassCount()) {
                list.add(new AccompanyAyalyzeResult(arrayList, arrayList.size()));
            }
        }
        if (DEBUG) {
            LOG.info("[logId:" + uuid + "]frequentPass used time " + (System.currentTimeMillis() - startTime) + "ms!");
            LOG.info("[logId:" + uuid + "]FrequentPassInputResults size is :" + list.size());
        }
        return list;
    }

    @Override
    public List<FaceTrackingResult> faceTracking(SearchImgByImgInputRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        if (DEBUG) {
            LOG.info("[logId:" + uuid + "]FaceTrackingRecord is :" + record.toString());
        }
        List<FaceTrackingResult> resultList = new ArrayList<>();
        if (record == null || record.getFeature() == null || record.getCameraId() == null) {
            LOG.error("faceTracking record info is null,return");
            if (DEBUG) {
                LOG.info("[logId:" + uuid + "]faceTracking used time " + (System.currentTimeMillis() - startTime) + "ms!");
                LOG.info("[logId:" + uuid + "]FaceTrackingResult is :" + resultList.toString());
            }
            return resultList;
        }
        if (record.getStartTime() < STARTTIME) {
            record.setStartTime(STARTTIME);
        }
        if (record.getDistance() == 0) {
            record.setDistance(MIN_SCORE);
        }
        List<SearchImgResult> results = searchFaceImgByImg(record);
        LOG.info(results.toString());
        resultList = FaceTrackingService.getResult(results);
        if (DEBUG) {
            LOG.info("[logId:" + uuid + "]faceTracking used time " + (System.currentTimeMillis() - startTime) + "ms!");
            LOG.info("[logId:" + uuid + "]FaceTrackingResult size is :" + resultList.size());
        }
        return resultList;
    }

    @Override
    public List<AccompanyAyalyzeResult> searchHumanByTrack(SearchHumanByTrackInputRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        if (DEBUG) {
            LOG.info("[logId:" + uuid + "]SearchHumanByTrackInputRecord is :" + record.toString());
        }
        List<AccompanyAyalyzeResult> list = new ArrayList<AccompanyAyalyzeResult>();
        if (record.getCameraId() == null) {
            LOG.error("SearchHumanByTrackInputRecord cameraId is null,please checked your request args!");
            if (DEBUG) {
                LOG.info("[logId:" + uuid + "]SearchHumanByTrackInputRecord used time " + (System.currentTimeMillis() - startTime) + "ms!");
                LOG.info("[logId:" + uuid + "]SearchHumanByTrackInputRecord size is :" + list.size());
            }
            return list;
        }
        if (record.getDistence() == 0) {
            record.setDistence(MIN_SCORE);
        }
        if(record.getStartTime() < STARTTIME) {
            record.setStartTime(STARTTIME);
        }
        ArrayList<ArrayList<String>> result = SearchHumanByTrack.executeQuery(record, POOLS);
        for (ArrayList<String> arrayList : result) {
            list.add(new AccompanyAyalyzeResult(arrayList, arrayList.size()));
        }
        if (DEBUG) {
            LOG.info("[logId:" + uuid + "]SearchHumanByTrackInputRecord used time " + (System.currentTimeMillis() - startTime) + "ms!");
            LOG.info("[logId:" + uuid + "]SearchHumanByTrackInputRecord size is :" + list.size());
        }
        return list;
    }
}
