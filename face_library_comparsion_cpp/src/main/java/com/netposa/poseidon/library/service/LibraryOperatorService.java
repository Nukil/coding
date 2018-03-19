package com.netposa.poseidon.library.service;

import com.netposa.HbaseUtil;
import com.netposa.poseidon.library.bean.*;
import com.netposa.poseidon.library.rpc.outrpc.LibraryOperation;
import com.netposa.poseidon.library.rpc.outrpc.LibraryOperationResponse;
import com.netposa.poseidon.library.rpc.outrpc.OperationType;
import com.netposa.poseidon.library.rpc.outrpc.StatusCode;
import com.netposa.poseidon.library.util.HashAlgorithm;
import com.netposa.poseidon.library.util.LoadPropers;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LibraryOperatorService {
    // 创建线程池，每个线程调用一个server上的rpc进行库操作
    private static ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    private static Logger logger = Logger.getLogger(LibraryOperatorService.class);
    private static final Properties properties = LoadPropers.getSingleInstance().getProperties("server");
    private static RpcManagerService rpcManagerService = RpcManagerService.getInstance();
    private static TableManagerService tableManagerService = TableManagerService.getInstance();
    private static final String META_TABLE = properties.getProperty("hbase.meta.data.table.name", "meta_data");

    public static LibraryOperationResponse execute(LibraryOperation request, String uuid) {
        LibraryOperationResponse response = new LibraryOperationResponse(StatusCode.ERROR_OTHER, "Unknown error occurred!");
        switch (request.getOperationType()) {
            case INSERT:
                if (tableManagerService.getTable().containsKey(request.getLibraryId())) {
                    logger.error("tables is already exist");
                    response.setMessage("tables is already exist");
                    return response;
                }
                break;
            case DELETE:
                if (!tableManagerService.getTable().containsKey(request.getLibraryId())) {
                    logger.error("tables is not exist");
                    response.setMessage("tables is not exist");
                    return response;
                }
                // 把表状态置成待删除
                Map<String, TableStatusCode> map = tableManagerService.getTable().get(request.getLibraryId());
                for (Map.Entry<String, TableStatusCode> entry : map.entrySet()) {
                    tableManagerService.updateTable(request.getLibraryId(), entry.getKey(), TableStatusCode.WAITTING_DELETED);
                }
                break;
            default:
                return new LibraryOperationResponse(StatusCode.ERROR_OTHER, "OperationType must be insert or delete!");
        }

        LibraryOperationResponse response1 = operateHbaseTable(request);
        if (response1.getRCode() != StatusCode.OK) {
            return new LibraryOperationResponse(StatusCode.ERROR_OTHER, "fail to create hbase table");
        }

        // 获取集群列表
        Map<ConnectionManagerKey, ConnectionManagerValue> cluster = rpcManagerService.getCluster();
        // 用于获取每个线程的返回值
        Future<com.netposa.poseidon.library.rpc.inrpc.LibraryOperationResponse>[] futures = new Future[cluster.size()];
        int index = 0;
        // 向每个server提交任务
        for (final ConnectionManagerKey key : cluster.keySet()) {
            futures[index++] = cachedThreadPool.submit(new LibraryOperatorService().new LibraryOperateThread(key, request, uuid));
        }
        // 遍历每个线程返回值，放入优先队列
        boolean flag = true;
        for (int i = 0; i < index; i++) {
            try {
                com.netposa.poseidon.library.rpc.inrpc.LibraryOperationResponse operationResponse = futures[i].get();
                flag &= (operationResponse.getRCode() == com.netposa.poseidon.library.rpc.inrpc.StatusCode.OK);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        if (flag) {
            response.setRCode(StatusCode.OK);
            response.setMessage("operate successfully");
            if (request.getOperationType() == OperationType.INSERT) {
                for (ConnectionManagerKey key : rpcManagerService.getCluster().keySet()) {
                    tableManagerService.updateTable(request.getLibraryId(), key.getIp(), TableStatusCode.IN_USING);
                }
            }
            if (request.getOperationType() == OperationType.DELETE) {
                tableManagerService.deleteTable(request.getLibraryId());
            }
        }

        return response;
    }

    private static LibraryOperationResponse operateHbaseTable(LibraryOperation request) {
        LibraryOperationResponse response = new LibraryOperationResponse(StatusCode.ERROR_OTHER, "Unknown error occurred!");
        if (request == null || request.getLibraryId() == null || (request.getLibraryType() != 1 && request.getLibraryType() != 2)) {
            logger.error(String.format("Args error,[%s]", request));
            response = new LibraryOperationResponse(StatusCode.ERROR_PARAM, String.format("Args error,[%s]", request));
            return response;
        }
        //库操作逻辑
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
                            logger.error(String.format("table %s information save to meta table failed!", request.getLibraryId()));
                        }
                        flag = HbaseUtil.createTable(request.getLibraryId(), new String[]{"cf"}, HashAlgorithm.getSplitKeys());
                    } else {
                        logger.error(String.format("table %s already exists!", request.getLibraryId()));
                        response = new LibraryOperationResponse(StatusCode.ERROR_OTHER, String.format("table %s already exists!", request.getLibraryId()));
                    }
                    break;
                case DELETE:
                    if (!HbaseUtil.tableIsExists(META_TABLE) || !HbaseUtil.tableIsExists(request.getLibraryId())) {
                        logger.error(String.format("table %s is not exists!", request.getLibraryId()));
                        response = new LibraryOperationResponse(StatusCode.ERROR_NOT_EXIST_LIBRARY, String.format("table %s is not exists in hbase!", request.getLibraryId()));
                        return response;
                    } else {
                        Delete delete = new Delete(request.getLibraryId().getBytes());
                        HbaseUtil.delete(delete, META_TABLE);
                    }

                    if (HbaseUtil.tableIsExists(request.getLibraryId())) {
                        flag = HbaseUtil.deleteTable(request.getLibraryId());
                    } else {
                        logger.error(String.format("table %s is not exists in cache!", request.getLibraryId()));
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
            logger.error(e.getMessage(), e);
            response = new LibraryOperationResponse(StatusCode.ERROR_OTHER, e.getMessage());
        }
        return response;
    }

    private class LibraryOperateThread implements Callable<com.netposa.poseidon.library.rpc.inrpc.LibraryOperationResponse> {
        // 用于存放
        ConnectionManagerKey key;
        // master-server连接
        ClientStatus cs;
        // master-server以图搜图输入条件
        com.netposa.poseidon.library.rpc.inrpc.LibraryOperation input;
        // 唯一标识uuid
        String uuid;

        LibraryOperateThread(ConnectionManagerKey key, LibraryOperation inputRecord, String uuid) {
            this.key = key;
            // 获取一个可用的连接
            AvailableConnectionBean acb = rpcManagerService.getAvailableConnection(key, false);
            if (null != acb) {
                this.cs = acb.getCs();
            }
            switch (inputRecord.getOperationType()) {
                case INSERT:
                    this.input = new com.netposa.poseidon.library.rpc.inrpc.LibraryOperation(
                            com.netposa.poseidon.library.rpc.inrpc.OperationType.INSERT,
                            inputRecord.getLibraryId(),
                            inputRecord.getLibraryType());
                    break;
                case DELETE:
                    this.input = new com.netposa.poseidon.library.rpc.inrpc.LibraryOperation(
                            com.netposa.poseidon.library.rpc.inrpc.OperationType.DELETE,
                            inputRecord.getLibraryId(),
                            inputRecord.getLibraryType());
            }
            this.uuid = uuid;
        }
        @Override
        public com.netposa.poseidon.library.rpc.inrpc.LibraryOperationResponse call() throws Exception {
            com.netposa.poseidon.library.rpc.inrpc.LibraryOperationResponse response = new com.netposa.poseidon.library.rpc.inrpc.LibraryOperationResponse();
            // 如果连接状态正常，执行master-server以图搜图
            if (null != cs && cs.getStatusCode() == ConnectionStatusCode.OK) {
                try {
                    logger.info("id : " + this.uuid + ", " + key.getIp() + ":" + key.getPort() + " execute library operate, operation type : "
                            + input.getOperationType() + ", library id : " + input.getLibraryId() + ", library type : " + input.getLibraryType());
                    response = cs.getConnection().libraryOperate(input, uuid);
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

}
