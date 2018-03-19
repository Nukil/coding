package com.netposa.main;

import com.netposa.bean.CacheBean;
import com.netposa.bean.CacheFaceFeature;
import com.netposa.init.LoadPropers;
import com.netposa.rpc.*;
import com.netposa.service.SearchHumanImgByImgService;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

public class HumanFeatureDataAnalyzeRpcServiceHandler implements HumanFeatureDataAnalyzeRpcService.Iface {
    private static final Logger log = LoggerFactory.getLogger(HumanFeatureDataAnalyzeRpcServiceHandler.class);
    private static int MIN_SCORE = Integer.parseInt(LoadPropers.getProperties().getProperty("default.score", "90").trim());// 相似度阈值
    private static boolean DEBUG = Boolean.parseBoolean(LoadPropers.getProperties().getProperty("service.process.debug.enable", "true").trim());// 调试日志开关
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
    private static final int HUMAN_FEATURE_VERSION_SIZE = Integer.parseInt(LoadPropers.getProperties().getProperty("human.version.size", "32").trim());
    private static final int HUMAN_FEATURE_SIZE = Integer.parseInt(LoadPropers.getProperties().getProperty("human.feature.size", "512").trim());
    private static CacheConfiguration<String, CacheBean> cacheCfg = new CacheConfiguration<>();
    private static final String IGNITE_NAME = LoadPropers.getProperties().getProperty("ignite.name");

    private static long STARTTIME = 0;

    static {
        try {
            STARTTIME = sdf1.parse(LoadPropers.getProperties().getProperty("start.time", "2017-01-01").trim()).getTime();
            cacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public List<SearchImgResult> searchHummerImgByImg(SearchImgByImgInputRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        if (DEBUG) {
            log.info("[logId:" + uuid + "]searchHummerImgByImg InputRecord is :" + record.toString());
        }
        List<SearchImgResult> results = new ArrayList<>();
        if (record == null || record.getFeature() == null) {
            log.error("searchHummerImgByImg record info is null,return");
            return results;
        }

        if (record.getFeature().length > HUMAN_FEATURE_SIZE) {
            byte[] feature = new byte[HUMAN_FEATURE_SIZE];
            System.arraycopy(record.getFeature(), HUMAN_FEATURE_VERSION_SIZE, feature, 0, HUMAN_FEATURE_SIZE);
            record.setFeature(feature);
        } else if (record.getFeature().length < HUMAN_FEATURE_SIZE) {
            log.error("record.feature.length is too short,require:[" + HUMAN_FEATURE_SIZE + "],find:[" + record.getFeature().length + "]return");
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
            calendar.setTimeInMillis(record.getStartTime() / 86400000 * 86400000);// 取整数天
            while (calendar.getTimeInMillis() <= record.getEndTime()) {
                dates.add(IGNITE_NAME + "_HUMAN_" + sdf.format(calendar.getTime()));
                calendar.add(Calendar.DATE, 1);
            }
            if (dates != null && dates.size() > 0) {
                results = SearchHumanImgByImgService.executeQuery(record, dates);
                if (DEBUG) {
                    log.info("[logId:" + uuid + "]searchHummerImgByImg used time " + (System.currentTimeMillis() - startTime) + "ms!results count is :" + results.size());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return results;
    }

    @Override
    public List<SearchImgResult>  searchHummerImgByLog(SearchImgByLogInputRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        if (DEBUG) {
            log.info("[logId:" + uuid + "]searchHummerImgByLog is :" + record.toString());
        }
        List<SearchImgResult> results = new ArrayList<>();
        if (record == null || record.getLogNum() == null || record.getDate() == null) {
            log.error("searchHummerImgByLog record info is null,return");
            return results;
        }
        SearchImgByImgInputRecord input = new SearchImgByImgInputRecord();
        IgniteCache<String, CacheFaceFeature> cache = Ignition.ignite(IGNITE_NAME).cache(IGNITE_NAME + "_HUMAN_" + record.getDate());
        if (cache != null && cache.containsKey(record.getLogNum())) {
            input.setFeature(cache.get(record.getLogNum()).getFeature());
        } else {
            log.error("logNum[" + record.getLogNum() + "]  is not found in the cache,return");
            return results;
        }
        input.setStartTime(record.getStartTime());
        input.setEndTime(record.getEndTime());
        input.setCameraId(record.getCameraId());
        input.setCount(record.getCount());
        input.setDistance(record.getDistance());
        results = searchHummerImgByImg(input);
        if (DEBUG) {
            log.info("[logId:" + uuid + "]searchHummerImgByLog used time " + (System.currentTimeMillis() - startTime)+ "ms!results count is :" + results.size());
        }
        return results;
    }
}
