package com.netposa.poseidon.library.main;

import com.netposa.poseidon.library.bean.CacheBean;
import com.netposa.poseidon.library.init.LoadPropers;
import com.netposa.poseidon.library.rpc.*;
import com.netposa.poseidon.library.service.BigLibraryComparisonService;
import com.netposa.poseidon.library.service.DataOperateService;
import com.netposa.poseidon.library.util.FaceFeatureVerify;
import com.netposa.poseidon.library.util.FaceFeatureVerifyDg;
import com.netposa.poseidon.library.util.HashAlgorithm;
import com.netposa.poseidon.library.util.HbaseUtil;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//import com.netposa.poseidon.library.service.BigLibraryComparisonJob;

public class LibraryDataAnalyzeRpcServiceHandler implements LibraryDataAnalyzeRpcService.Iface {
    private static final String IGNITE_NAME = LoadPropers.getProperties().getProperty("ignite.name");
    private static final Logger log = LoggerFactory.getLogger(LibraryDataAnalyzeRpcServiceHandler.class);
    private static boolean DEBUG = Boolean.parseBoolean(LoadPropers.getProperties().getProperty("service.process.debug.enable", "true").trim());// 调试日志开关
    private static CacheConfiguration<String, CacheBean> cacheCfg = new CacheConfiguration<>();
    private static final String META_TABLE = LoadPropers.getProperties().getProperty("hbase.meta.data.table.name", "meta_data");
    private static int regionNum = Integer.parseInt(LoadPropers.getProperties().getProperty("hbase.split.region.num", "50").trim());
    private static  String formatStr = "%0" + String.valueOf(regionNum - 1).length() + "d" ;
    static {
        try {
            cacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
            cacheCfg.setCacheMode(CacheMode.PARTITIONED);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public LibraryOperationResponse libraryOperate(LibraryOperation request) throws TException {
        LibraryOperationResponse response = new LibraryOperationResponse(StatusCode.ERROR_OTHER, "Unknown error occurred!");
        String uuid = UUID.randomUUID().toString();
        if (DEBUG) {
            log.info("[logId:" + uuid + "]libraryOperate is :" + request.toString());
        }
//        int a = 0;//用于判断库
        // System.out.println(request.getLibraryType());

        if (request == null || request.getLibraryId() == null || (request.getLibraryType() != 1 && request.getLibraryType() != 2)) {
            log.error(String.format("Args error,[%s]", request));
            response = new LibraryOperationResponse(StatusCode.ERROR_PARAM, String.format("Args error,[%s]", request));
            return response;
        }
        //库操作逻辑
        long startTime = System.currentTimeMillis();
        boolean flag = false;
        try {
            switch (request.getOperationType()) {
                case INSERT:
                    if (!HbaseUtil.tableIsExists(request.getLibraryId())) {
                        if (!HbaseUtil.tableIsExists(META_TABLE)) {
                            HbaseUtil.createTable(META_TABLE, new String[]{"cf"}, null);
                        }
                        Put put = new Put(request.getLibraryId().getBytes());
                        put.addColumn("cf".getBytes(), "type".getBytes(), String.valueOf(request.getLibraryType()).getBytes());
                        flag = HbaseUtil.save(put, META_TABLE);
                        if (!flag) {
                            log.error(String.format("table %s information save to meta table failed!", request.getLibraryId()));
                        }
//                        for(int i = 0; i < regionNum; i++){
//                            String str = String.format(formatStr, i);
//                            cacheCfg.setName(request.getLibraryId() + "_" + str);
//                            Ignition.ignite(IGNITE_NAME).getOrCreateCache(cacheCfg);
//                        }
                        flag = HbaseUtil.createTable(request.getLibraryId(), new String[]{"cf"}, HashAlgorithm.getSplitKeys(null));
                    } else {
                        log.error(String.format("table %s already exists!", request.getLibraryId()));
                        response = new LibraryOperationResponse(StatusCode.ERROR_OTHER, String.format("table %s already exists!", request.getLibraryId()));
                    }
                    break;
                case DELETE:
                    if (!HbaseUtil.tableIsExists(META_TABLE) || !HbaseUtil.tableIsExists(request.getLibraryId())) {
                        log.error(String.format("table %s is not exists!", request.getLibraryId()));
                        response = new LibraryOperationResponse(StatusCode.ERROR_NOT_EXIST_LIBRARY, String.format("table %s is not exists in hbase!", request.getLibraryId()));
                        return response;
                    } else {
                        Delete delete = new Delete(request.getLibraryId().getBytes());
                        HbaseUtil.delete(delete, META_TABLE);
                    }
                    for(int i = 0; i < regionNum; i++){
                        String str = String.format(formatStr, i);
                        Ignition.ignite(IGNITE_NAME).destroyCache(request.getLibraryId() + "_" + str);
                    }
                    if (HbaseUtil.tableIsExists(request.getLibraryId())) {
                        flag = HbaseUtil.deleteTable(request.getLibraryId());
                    } else {
                        log.error(String.format("table %s is not exists in cache!", request.getLibraryId()));
                        response = new LibraryOperationResponse(StatusCode.ERROR_NOT_EXIST_LIBRARY, String.format("table %s is not exists in cache!", request.getLibraryId()));
                        return response;
                    }
                    break;
                default:
                    response = new LibraryOperationResponse(StatusCode.ERROR_OTHER, "OperationType must be insert or delete!");
            }
            if (flag) {
                response = new LibraryOperationResponse(StatusCode.OK, "SUCCESS");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response = new LibraryOperationResponse(StatusCode.ERROR_OTHER, e.getMessage());
        }
        if (DEBUG) {

            log.info("[logId:" + uuid + "]library " + request.getOperationType() + "used time " + (System.currentTimeMillis() - startTime) + ",result is " + response);
        }
        return response;
    }

    @Override
    public StorageResponse dataOperate(StorageRequest request) throws TException {
        String uuid = UUID.randomUUID().toString();
        if (request != null && DEBUG) {
            log.info("[logId:" + uuid + "]dataOperate count is :" + request.getStorageInfos().size() + request.getStorageInfos());
        }
        if (request == null || request.getStorageInfos() == null || request.getStorageInfos().size() == 0) {
            log.error(String.format("Task [%s] dataOperate args error,[%s]", uuid, request));
            return new StorageResponse(StatusCode.ERROR_PARAM, String.format("Task [%s] dataOperate args error,[%s]", uuid, request));
        }
        for (StorageInfo re : request.getStorageInfos()) {
            if (re.getId() == null || re.getLibraryId() == null) {
                log.error(String.format("Args error,[%s]", request));
                return new StorageResponse(StatusCode.ERROR_PARAM, String.format("Task [%s] dataOperate args [LibraryId or Id] is can not null error,[%s]", uuid, re));
            }
            if (request.getType().equals(OperationType.INSERT)) {
                for (ImgInfo img : re.getImgInfos()) {
                    if (img.getFeature() == null || img.getFeature().length < 1) {
                        log.error(String.format("Args error,[%s]", request));
                        return new StorageResponse(StatusCode.ERROR_PARAM, String.format("Task [%s] dataOperate args [ImgInfo] is can not null error,[%s]", uuid, re));
                    }
                }
            }
        }
        //数据作逻辑
        long startTime = System.currentTimeMillis();
        try {
            StorageResponse storageResponse = new DataOperateService().data_Operate(request);
            log.info(String.format("Task [%s]data " + request.getType() + " Operate finish,used time:%dms!", uuid, System.currentTimeMillis() - startTime));
            return storageResponse;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new StorageResponse(StatusCode.ERROR_OTHER, e.getMessage());
        }
    }

    @Override
    public QueryResponse dataQuery(QueryRequest request) throws TException {
        String uuid = UUID.randomUUID().toString();
        if (DEBUG) {
            log.info("[logId:" + uuid + "]dataQuery is :" + request.toString());
        }
        //参数判断
        if (request == null || request.getLibraryIds() == null || request.getLibraryIds().size() == 0 || request.getSimilarity() < 0 || request.getRCount() <= 0) {
            log.error(String.format("Args error,[%s]", request));
            return new QueryResponse(StatusCode.ERROR_PARAM, String.format("Task [%s] dataQuery args error,[%s]", uuid, request), null);
        }
        for(String tableN : request.getLibraryIds()){
            if(!HbaseUtil.tableIsExists(tableN)){
                return new QueryResponse(StatusCode.ERROR_NOT_EXIST_LIBRARY , String.format("Task [%s] dataQuery error,table [%s] is not exists!", uuid, tableN), null);
            }
        }
        //大库比对逻辑
        long startTime = System.currentTimeMillis();
        try {
            List<RecordInfo> recordInfos = BigLibraryComparisonService.executeQuery(request);
            log.info(String.format("Query task [%s]  finish,used time:%dms!", uuid, System.currentTimeMillis() - startTime));
            return new QueryResponse(StatusCode.OK, "SUCCESS", recordInfos);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new QueryResponse(StatusCode.ERROR_OTHER, e.getMessage(), null);
        }
    }

    @Override
    public Map<String, QueryResponse> dataBatchQuery(List<QueryRequest> queryRequests) throws TException {
        String uuid = UUID.randomUUID().toString();
        Map<String, QueryResponse> result = new HashMap<>();
        //参数判断
        if (DEBUG) {
            log.info("[logId:" + uuid + "]dataQuery is :" + queryRequests.toString());
        }
        if (queryRequests == null || queryRequests.size() == 0) {
            log.error(String.format("Task [%s] dataBatchQuery args error,[%s]", uuid, queryRequests));
            QueryResponse queryResponse = new QueryResponse(StatusCode.ERROR_PARAM, "请求参数非法!", null);
            result.put(uuid, queryResponse);
            return result;
        }
        //小库比对逻辑
        long startTime = System.currentTimeMillis();
        try {
            for(QueryRequest request :queryRequests){
                List<RecordInfo> recordInfos = BigLibraryComparisonService.executeQuery(request);
                result.put(request.getRequestId(),new QueryResponse(StatusCode.OK, "SUCCESS", recordInfos));
            }
            log.info(String.format("Query task [%s]  finish,used time:%dms!", uuid, System.currentTimeMillis() - startTime));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    @Override
    public double featureVerify(FeatureVerifyRecord record) throws TException {
        String igniteName = "Netposa";
        IgniteCache<String, String> igniteCache = Ignition.ignite(igniteName).cache(igniteName + "_LIBRARY_GLOBAL");
        String algorithm = igniteCache.get("ALGORITHM");
        int versionSize = Integer.parseInt(igniteCache.get("VERSIONSIZE"));
        double score;
        long startTime = System.currentTimeMillis();
        if (record == null || record.getFeature1() != null && record.getFeature2() != null) {
            log.info(String.format("feature1.length is[%d],feature2.length is [%d]", record.getFeature1().length, record.getFeature2().length));
        } else {
            log.error("feature is null,exit!");
            return -1;
        }
        if ("netposa".equalsIgnoreCase(algorithm)) {
            score = FaceFeatureVerify.verify(record.getFeature1(), versionSize, record.getFeature2(), versionSize);
        } else {
            score = FaceFeatureVerifyDg.verify(record.getFeature1(), versionSize, record.getFeature2(), versionSize);
        }
        if (DEBUG) {
            log.info("face feature verify score is [" + score + "],used time " + (System.currentTimeMillis() - startTime) + " ms!");
        }
        return score;
    }

}
