package com.netposa.poseidon.library.service;

import com.netposa.poseidon.library.bean.*;
import com.netposa.poseidon.library.rpc.outrpc.*;
import com.netposa.poseidon.library.util.Byte2FloatUtil;
import com.netposa.poseidon.library.util.HashAlgorithm;
import com.netposa.poseidon.library.util.LoadPropers;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import java.util.*;
import java.util.concurrent.TimeUnit;


public class DataOperationService {
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

    public static StorageResponse execute(StorageRequest request, String uuid) {
        StorageResponse response = new StorageResponse();
        boolean flag;
        // 先判断参数的正确性
        for (StorageInfo info : request.getStorageInfos()) {
            if (info.getId() == null || info.getLibraryId() == null) {
                logger.error(String.format("Args error,[%s]", request));
                return new StorageResponse(StatusCode.ERROR_PARAM, String.format("Task [%s] dataOperate args [LibraryId or Id] is can not null error,[%s]", uuid, info));
            }
            if (request.getType().equals(OperationType.INSERT)) {
                for (ImgInfo img : info.getImgInfos()) {
                    if (img.getFeature() == null || img.getFeature().length < 1) {
                        logger.error(String.format("Args error,[%s]", request));
                        return new StorageResponse(StatusCode.ERROR_PARAM, String.format("Task [%s] dataOperate args [ImgInfo] is can not null error,[%s]", uuid, info));
                    }
                }
            }
            if (!tableManagerService.getTable().containsKey(info.getLibraryId())) {
                logger.error(String.format("table is not exist,[%s]", request));
                return new StorageResponse(StatusCode.ERROR_PARAM, String.format("Task [%s] dataOperate args [LibraryId] is not exist,[%s]", uuid, info.getId()));
            } else {
                flag = true;
                for (TableStatusCode code : tableManagerService.getTable().get(info.getLibraryId()).values()) {
                    flag &= code == TableStatusCode.IN_USING;
                }
                if (!flag) {
                    return new StorageResponse(StatusCode.ERROR_PARAM, String.format("Task [%s] dataOperate args [LibraryId] is not in using,[%s]", uuid, info.getId()));
                }
            }
        }

        if (request.getStorageInfos().size() <= 0) {
            logger.warn("data list is empty");
        } else {
            //对数据进行分组
            Map<ConnectionManagerKey, com.netposa.poseidon.library.rpc.inrpc.StorageRequest> dataMap = splitDataList(request);
            Iterator entries = dataMap.entrySet().iterator();
            // 找到当前可用的server
            while (entries.hasNext()) {
                Map.Entry entry = (Map.Entry)entries.next();
                com.netposa.poseidon.library.rpc.inrpc.StorageRequest record = (com.netposa.poseidon.library.rpc.inrpc.StorageRequest)entry.getValue();
                if (null == record || null == record.getStorageInfos() || record.getStorageInfos().size() <= 0) {
                    entries.remove();
                    continue;
                }
                flag = true;
                while (flag) {
                    // 先尝试获取一个对应server的连接, 有可能拿到别的节点的连接
                    AvailableConnectionBean connectionBean = rpcManagerService.getAvailableConnection((ConnectionManagerKey) entry.getKey(), true);
                    if (null != connectionBean) {
                        ConnectionManagerKey key = connectionBean.getKey();
                        ClientStatus cs = connectionBean.getCs();
                        if (cs.getStatusCode() == ConnectionStatusCode.OK) {
                            try {
                                com.netposa.poseidon.library.rpc.inrpc.StorageResponse storageResponse = cs.getConnection().dataOperate(record, uuid);
                                if (storageResponse.getRCode() == com.netposa.poseidon.library.rpc.inrpc.StatusCode.OK) {
                                    entries.remove();
                                } else {
                                    logger.error("id : " + uuid + ", save data failed : " + storageResponse.getMessage());
                                }
                                if (debug) {
                                    logger.info(String.format("id : %s, data was sent to %s : %s", uuid, key.getIp(), key.getPort()));
                                }
                                rpcManagerService.putConnection(key, cs);
                            } catch (TException e) {
                                logger.error(String.format("id : %s, send data to server error %s, %s", uuid, e.getMessage(), e));
                                cs.setStatusCode(ConnectionStatusCode.DISCONNECTED);
                                rpcManagerService.putConnection(key, cs);
                            }
                        }
                    }
                    flag = dataMap.containsKey((ConnectionManagerKey)entry.getKey());
                    if (flag) {
                        logger.warn("id : " + uuid + " ,send data failed, server may all dead, retry after 3s");
                        try {
                            TimeUnit.SECONDS.sleep(3);
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            }
        }
        response.setRCode(StatusCode.OK);
        response.setMessage("storage data successfully");
        return response;
    }

    /**
     * 对数据进行切分
     * @param request 输入图片信息集合
     * @return 切分好的数据map
     */
    private static Map<ConnectionManagerKey, com.netposa.poseidon.library.rpc.inrpc.StorageRequest> splitDataList(StorageRequest request) {
        Set<ConnectionManagerKey> clusterKeySet = rpcManagerService.getCluster().keySet();
        Map<ConnectionManagerKey, com.netposa.poseidon.library.rpc.inrpc.StorageRequest> map = new HashMap<>();
        List<StorageInfo> list = request.getStorageInfos();

        for (StorageInfo storageInfo : list) {
            int hashValue = HashAlgorithm.BKDRHash(storageInfo.getId()) % HashAlgorithm.BKDRHashValue;
            // 根据hash值将数据分配到Map的对应位置
            for (ConnectionManagerKey key : clusterKeySet) {
                if (hashValue >= key.getStartHashValue() && hashValue <= key.getEndHashValue()) {
                    if (map.containsKey(key)) {
                        map.get(key).addToStorageInfos(new com.netposa.poseidon.library.rpc.inrpc.StorageInfo(
                                storageInfo.getLibraryId(),
                                storageInfo.getId(),
                                out2InImgInfo(storageInfo.getImgInfos()),
                                storageInfo.getExt()));
                    } else {
                        switch (request.getType()) {
                            case DELETE:
                                map.put(key, new com.netposa.poseidon.library.rpc.inrpc.StorageRequest(new ArrayList<com.netposa.poseidon.library.rpc.inrpc.StorageInfo>(), com.netposa.poseidon.library.rpc.inrpc.OperationType.DELETE));
                                break;
                            case INSERT:
                                map.put(key, new com.netposa.poseidon.library.rpc.inrpc.StorageRequest(new ArrayList<com.netposa.poseidon.library.rpc.inrpc.StorageInfo>(), com.netposa.poseidon.library.rpc.inrpc.OperationType.INSERT));
                                break;
                            case UPDATE:
                                map.put(key, new com.netposa.poseidon.library.rpc.inrpc.StorageRequest(new ArrayList<com.netposa.poseidon.library.rpc.inrpc.StorageInfo>(), com.netposa.poseidon.library.rpc.inrpc.OperationType.UPDATE));
                                break;
                        }
                        map.get(key).addToStorageInfos(new com.netposa.poseidon.library.rpc.inrpc.StorageInfo(
                                storageInfo.getLibraryId(),
                                storageInfo.getId(),
                                out2InImgInfo(storageInfo.getImgInfos()),
                                storageInfo.getExt()));
                    }
                }
            }
        }
        return map;
    }

    private static List<com.netposa.poseidon.library.rpc.inrpc.ImgInfo> out2InImgInfo(List<ImgInfo> list) {
        List<com.netposa.poseidon.library.rpc.inrpc.ImgInfo> rList = new ArrayList<>();
        for (ImgInfo img : list) {
            if (null == img.getFeature() || img.getFeature().length != (faceFeatureSize + faceTailSize + faceVersionSize)) {
                rList .add(new com.netposa.poseidon.library.rpc.inrpc.ImgInfo(img.getImgId(), null));
                continue;
            }
            List<Double> feature = new ArrayList<>();
            for (int i = 0; i < featRawDim; i++) {
                feature.add((double)Byte2FloatUtil.byte2float(img.getFeature(), faceVersionSize + i * floatSize));
            }
            rList.add(new com.netposa.poseidon.library.rpc.inrpc.ImgInfo(img.getImgId(), feature));
        }
        return rList;
    }

}
