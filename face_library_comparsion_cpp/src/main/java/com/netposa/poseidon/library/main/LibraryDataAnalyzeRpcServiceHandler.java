package com.netposa.poseidon.library.main;

import com.netposa.HbaseUtil;
import com.netposa.poseidon.library.rpc.outrpc.*;
import com.netposa.poseidon.library.service.DataOperateService;
import com.netposa.poseidon.library.service.DataOperationService;
import com.netposa.poseidon.library.service.DataQueryService;
import com.netposa.poseidon.library.service.LibraryOperatorService;
import com.netposa.poseidon.library.util.FaceFeatureVerify;
import com.netposa.poseidon.library.util.FaceFeatureVerifyDg;
import com.netposa.poseidon.library.util.HashAlgorithm;
import com.netposa.poseidon.library.util.LoadPropers;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class LibraryDataAnalyzeRpcServiceHandler implements LibraryDataAnalyzeRpcService.Iface {
    private static final Properties properties = LoadPropers.getSingleInstance().getProperties("server");
    private static final Logger logger = LoggerFactory.getLogger(LibraryDataAnalyzeRpcServiceHandler.class);
    private static boolean DEBUG = Boolean.parseBoolean(properties.getProperty("service.process.debug.enable", "true").trim());// 调试日志开关
    private static final String META_TABLE = properties.getProperty("hbase.meta.data.table.name", "meta_data");
    private static String arithmeticType = properties.getProperty("arithmetic.type");
    private static int FACE_FEATURE_SIZE = Integer.parseInt(properties.getProperty("face.feature.size").trim());
    private static int FACE_FEATURE_VERSION_SIZE = Integer.parseInt(properties.getProperty("face.version.size").trim());
    private static int FACE_FEATURE_TAIL_SIZE = Integer.parseInt(properties.getProperty("face.tail.size").trim());
    private static byte[] arithmeticLabel;
    private static boolean enableCheck = false;

    static {
        // 设置算法版本校验位
        if (null != arithmeticType && "netposa".equals(arithmeticType)) {
            enableCheck = true;
            String arithmeticVersion = properties.getProperty("face.arithmetic.version");
            if ("1.3".equals(arithmeticVersion)) {
                arithmeticLabel = new byte[]{1, 0, 3, 0};
            } else if ("1.5".equals(arithmeticVersion)) {
                arithmeticLabel = new byte[]{1, 0, 5, 0};
            }
        } else {
            enableCheck = false;
        }
    }

    @Override
    public LibraryOperationResponse libraryOperate(LibraryOperation request) throws TException {
        LibraryOperationResponse response;
        String uuid = UUID.randomUUID().toString();
        if (DEBUG) {
            logger.info("[logId:" + uuid + "]libraryOperate is :" + request.toString());
        }
        if (request == null || request.getLibraryId() == null || request.getLibraryType() == 0) {
            logger.error(String.format("Args error,[%s]", request));
            response = new LibraryOperationResponse(StatusCode.ERROR_PARAM, String.format("Args error,[%s]", request));
            return response;
        }
        long startTime = System.currentTimeMillis();
        response = LibraryOperatorService.execute(request, uuid);
        if (DEBUG) {
            logger.info("[logId:" + uuid + "] library " + request.getOperationType() + " used time " + (System.currentTimeMillis() - startTime) + " ,result is " + response);
        }
        return response;
    }

    @Override
    public StorageResponse dataOperate(StorageRequest request) throws TException {
        String uuid = UUID.randomUUID().toString();
        if (request != null && DEBUG) {
            logger.info("[logId:" + uuid + "] dataOperate count is :" + request.getStorageInfos().size() + request.getStorageInfos());
        }
        if (request == null || request.getStorageInfos() == null || request.getStorageInfos().size() == 0) {
            logger.error(String.format("Task [%s] dataOperate args error,[%s]", uuid, request));
            return new StorageResponse(StatusCode.ERROR_PARAM, String.format("Task [%s] dataOperate args error,[%s]", uuid, request));
        }

        //数据作逻辑
        long startTime = System.currentTimeMillis();
        try {
            StorageResponse storageResponse = new DataOperateService().data_Operate(request);
            if (storageResponse.getRCode() == StatusCode.OK) {
                storageResponse = DataOperationService.execute(request, uuid);
                logger.info(String.format("Task [%s] dataOperate finish,used time:%dms!", uuid, System.currentTimeMillis() - startTime));
            }
            return storageResponse;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new StorageResponse(StatusCode.ERROR_OTHER, e.getMessage());
        }
    }

    @Override
    public QueryResponse dataQuery(QueryRequest request) throws TException {
        String uuid = UUID.randomUUID().toString();
        if (DEBUG) {
            logger.info("[logId:" + uuid + "] dataQuery is :" + request.toString());
        }
        //大库比对逻辑
        long startTime = System.currentTimeMillis();
        try {
            QueryResponse response = DataQueryService.execute(request, uuid);
            logger.info(String.format("Query task [%s] finish,used time:%dms!", uuid, System.currentTimeMillis() - startTime));
            return response;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new QueryResponse(StatusCode.ERROR_OTHER, e.getMessage(), null);
        }
    }

    @Override
    public Map<String, QueryResponse> dataBatchQuery(List<QueryRequest> queryRequests) throws TException {
        String uuid = UUID.randomUUID().toString();
        Map<String, QueryResponse> result = new HashMap<>();
        //参数判断
        if (DEBUG) {
            logger.info("[logId:" + uuid + "]dataQuery is :" + queryRequests.toString());
        }
        if (queryRequests == null || queryRequests.size() == 0) {
            logger.error(String.format("Task [%s] dataBatchQuery args error,[%s]", uuid, queryRequests));
            QueryResponse queryResponse = new QueryResponse(StatusCode.ERROR_PARAM, "请求参数非法!", null);
            result.put(uuid, queryResponse);
            return result;
        }
        //小库比对逻辑
        long startTime = System.currentTimeMillis();
        try {
            for(QueryRequest request :queryRequests){
                QueryResponse response = DataQueryService.execute(request, uuid);
                result.put(request.getRequestId(), response);
            }
            logger.info(String.format("Query task [%s]  finish,used time:%dms!", uuid, System.currentTimeMillis() - startTime));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }

    @Override
    public double featureVerify(FeatureVerifyRecord record) throws TException {
        String uuid = UUID.randomUUID().toString();
        logger.info("featureVerify id : " + uuid + ", input record : " + record.toString());
        double score = 0.0f;
        long start = System.currentTimeMillis();
        if (null == record.getFeature1() || null == record.getFeature2()
                || record.getFeature1().length != record.getFeature2().length || record.getFeature1().length != (FACE_FEATURE_SIZE + FACE_FEATURE_TAIL_SIZE + FACE_FEATURE_VERSION_SIZE)) {
            logger.error("featureVerify id : " + uuid + " error feature, please check");
            return 0.0f;
        }
        if (enableCheck) {
            byte[] version1 = new byte[4];
            byte[] version2 = new byte[4];
            System.arraycopy(record.getFeature1(), 4, version1, 0, 4);
            System.arraycopy(record.getFeature2(), 4, version2, 0, 4);
            if (!Arrays.equals(version1, version2) || !Arrays.equals(version1, arithmeticLabel)) {
                logger.error("featureVerify id : " + uuid + "error feature version, please check");
                return 0.0f;
            }
        }
        if ("netposa".equalsIgnoreCase(arithmeticType)) {
            score = FaceFeatureVerify.verify(record.getFeature1(), FACE_FEATURE_VERSION_SIZE, record.getFeature2(), FACE_FEATURE_VERSION_SIZE);
        } else {
            score = FaceFeatureVerifyDg.verify(record.getFeature1(), FACE_FEATURE_VERSION_SIZE, record.getFeature2(), FACE_FEATURE_VERSION_SIZE);
        }
        logger.info("featureVerify id : " + uuid + " completed, result is : " + score + ", use time : " + (System.currentTimeMillis() - start) + " ms");
        return score;
    }
}
