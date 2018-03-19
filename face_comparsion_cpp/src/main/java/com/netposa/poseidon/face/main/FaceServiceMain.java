package com.netposa.poseidon.face.main;

import com.netposa.poseidon.face.bean.ClientStatus;
import com.netposa.poseidon.face.bean.ConnectionManagerKey;
import com.netposa.poseidon.face.bean.ConnectionManagerValue;
import com.netposa.poseidon.face.bean.ConnectionStatusCode;
import com.netposa.poseidon.face.init.ThreadExceptionHandler;
import com.netposa.poseidon.face.rpc.inrpc.FaceFeatureDataAnalyzeRpcService;
import com.netposa.poseidon.face.service.FetchMessageService;
import com.netposa.poseidon.face.service.FaceDataAnalyzeService;
import com.netposa.poseidon.face.service.RpcManagerService;
import com.netposa.poseidon.face.util.LoadPropers;
import com.netposa.poseidon.face.util.RpcUtil;
import org.apache.log4j.Logger;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class FaceServiceMain {
    public static void main(String[] args) {
        Logger logger = Logger.getLogger(FaceServiceMain.class);
        LoadPropers instance = LoadPropers.getSingleInstance();
        // 加载服务配置文件
        Properties serverProperties = instance.getProperties("server");
        // 加载kafka配置文件
        Properties kafkaProperties = instance.getProperties("kafka-consumer");
        // rpc超时时间
        int timeout = Integer.parseInt(serverProperties.getProperty("master.server.timeout", "30000"));
        // master与server之间rpc初始化连接池大小
        int initServerPoolSize = Integer.parseInt(serverProperties.getProperty("master.server.rpc.pool.init.size", "2"));
        // master与server之间rpc最大连接池大小
        int maxServerPoolSize = Integer.parseInt(serverProperties.getProperty("master.server.rpc.pool.max.size", "5"));

        // 启动集群心跳监控(master与server之间保持心跳)
        RpcManagerService rpcManagerService = RpcManagerService.getInstance();
        String clusterList = serverProperties.getProperty("cluster.list");
        String[] ipPorts = clusterList.split(",");
        for (String ipPort : ipPorts) {
            String[] split = ipPort.split(":");
            String ip = split[0];
            int port = Integer.parseInt(split[1]);
            int hashStartValue = Integer.parseInt(split[2]);
            int hashEndValue = Integer.parseInt(split[3]);
            // 循环创建rpc连接
            ConnectionManagerKey key = new ConnectionManagerKey(ip, port, timeout, hashStartValue, hashEndValue);
            ConnectionManagerValue value = new ConnectionManagerValue(maxServerPoolSize);
            for (int i = 0; i < initServerPoolSize; i++) {
                ConnectionStatusCode code;
                FaceFeatureDataAnalyzeRpcService.Client client = RpcUtil.getInRpcConnection(ip, port, timeout);
                if (null != client) {
                    try {
                        client.ping();
                        logger.info("node " + ip + ":" + port + " is connected");
                        code = ConnectionStatusCode.OK;
                        value.putRpcPool(new ClientStatus(client, code));
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                } else {
                    logger.warn("node " + ip + ":" + port + " is disconnected");
                }
            }
            rpcManagerService.updateCluster(key, value);
        }
        rpcManagerService.setUncaughtExceptionHandler(new ThreadExceptionHandler());
        new Thread(rpcManagerService).start();

        // 启动kafka数据拉取线程
        FetchMessageService fetchMsgThread = new FetchMessageService(serverProperties, kafkaProperties);
        fetchMsgThread.setUncaughtExceptionHandler(new ThreadExceptionHandler());
        new Thread(fetchMsgThread).start();

        // 启动对外rpc
        int rpcPort = Integer.parseInt(serverProperties.getProperty("rpc.port", "30055").trim());
        Thread thread = new Thread(new FaceDataAnalyzeService(rpcPort), "FaceFeatureDataAnalyzeThread");
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler(new ThreadExceptionHandler());
        thread.start();

        while (true) {
            try {
                TimeUnit.SECONDS.sleep(3600);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}