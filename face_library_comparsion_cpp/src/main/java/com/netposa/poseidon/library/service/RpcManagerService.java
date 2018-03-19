package com.netposa.poseidon.library.service;

import com.netposa.poseidon.library.bean.*;
import com.netposa.poseidon.library.rpc.inrpc.LibraryDataAnalyzeRpcService;
import com.netposa.poseidon.library.util.LoadPropers;
import com.netposa.poseidon.library.util.RpcUtil;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RpcManagerService extends Thread {
    private LoadPropers instance = LoadPropers.getSingleInstance();
    // 每次获取心跳间隔
    private int heartBeatTimeInterval = Integer.parseInt(instance.getProperties("server").getProperty("heart.beat.time.interval"));
    private int rpcPoolSize = Integer.parseInt(instance.getProperties("server").getProperty("master.server.rpc.pool.max.size", "5"));
    private Logger logger = Logger.getLogger(RpcManagerService.class);

    // 单例模式, 数据只保存一份
    private RpcManagerService() {}
    private static class LazyHolder {
        private static final RpcManagerService instance = new RpcManagerService();
    }
    public static final RpcManagerService getInstance() {
        return LazyHolder.instance;
    }
    private static boolean stopped = false;

    /**
     * 集群节点信息列表
     */
    private Map<ConnectionManagerKey, ConnectionManagerValue> cluster = new HashMap<>();

    @Override
    public void run() {
        while (!stopped) {
            for (Map.Entry<ConnectionManagerKey, ConnectionManagerValue> entry : cluster.entrySet()) {
                for (int i = 0; i < rpcPoolSize; i++) {
                    ClientStatus cs = entry.getValue().getRpcPool().poll();
                    if (null == cs) {
                        // 没有取到连接，可能连接池內没有连接了，尝试新创建一个
                        LibraryDataAnalyzeRpcService.Client client = RpcUtil.getInRpcConnection(entry.getKey().getIp(), entry.getKey().getPort(), entry.getKey().getTimeout());
                        if (null != client) {
                            try {
                                client.ping();
                                entry.getValue().getRpcPool().offer(new ClientStatus(client, ConnectionStatusCode.OK));
                            } catch (TException e) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    }

                    // 上次确认节点工作正常
                    if (null != cs && cs.getStatusCode() == ConnectionStatusCode.OK) {
                        try {
                            cs.getConnection().ping();
                            // 获取心跳后，将连接放回去
                            entry.getValue().getRpcPool().offer(cs);
                        } catch (TException e) {
                            logger.error(String.format("node " + entry.getKey().getIp() +" is disconnected, %s, %s", e.getMessage(), e));
                        }
                    } else if (null != cs && cs.getStatusCode() == ConnectionStatusCode.DISCONNECTED) {
                        // 上次没确认成功，尝试重连
                        LibraryDataAnalyzeRpcService.Client client = RpcUtil.getInRpcConnection(entry.getKey().getIp(), entry.getKey().getPort(), entry.getKey().getTimeout());
                        if (null != client) {
                            try {
                                // 调用rpc心跳接口，确认节点正常
                                client.ping();
                                // 如果正常，修改集群列表信息
                                logger.info("node " + entry.getKey().getIp() + ":" + entry.getKey().getPort() + " is reconnected");
                                entry.getValue().getRpcPool().offer(new ClientStatus(client, ConnectionStatusCode.OK));
                            } catch (TException e) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    }
                }
            }
            logger.info("current cluster avaliable connection number is : " + getAvailableServerNum());
            // 睡眠，等待下次获取心跳
            try {
                TimeUnit.MILLISECONDS.sleep(heartBeatTimeInterval);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 用于获取集群列表
     * @return Map<ConnectionManagerKey, ConnectionManagerValue>
     */
    public Map<ConnectionManagerKey, ConnectionManagerValue> getCluster() {
        return cluster;
    }

    /**
     * 用于更新集群的ConnectionManagerKey和ConnectionManagerValue
     * @param key ConnectionManagerKey
     * @param value ConnectionManagerValue
     */
    public void updateCluster(ConnectionManagerKey key, ConnectionManagerValue value) {
        cluster.put(key, value);
    }

    /**
     * 获取集群内可用的连接数量
     * @return 节点数
     */
    public int getAvailableServerNum() {
        int num = 0;
        for (ConnectionManagerValue value : cluster.values()) {
            for (int i = 0; i < rpcPoolSize; i++) {
                ClientStatus cs = value.getRpcPool().poll();
                if (null != cs) {
                    if (cs.getStatusCode() == ConnectionStatusCode.OK) {
                        num++;
                    }
                    value.getRpcPool().offer(cs);
                }
            }
        }
        return num;
    }

    /**
     * 根据key获取一个可用的连接
     * @param key ConnectionManagerKey
     * @return ClientStatus or null
     */
    public AvailableConnectionBean getAvailableConnection(ConnectionManagerKey key, boolean flag) {
        // 先尝试取自己节点上的rpc
        if (cluster.containsKey(key)) {
            for (int i = 0; i < rpcPoolSize; i++) {
                ClientStatus cs = cluster.get(key).getRpcPool().poll();
                if (null != cs) {
                    if (cs.getStatusCode() == ConnectionStatusCode.OK) {
                        try {
                            cs.getConnection().ping();
                            // 将本节点key信息与连接信息打包返回
                            return new AvailableConnectionBean(key, cs);
                        } catch (TException e) {
                            logger.error(e.getMessage(), e);
                            cs.setStatusCode(ConnectionStatusCode.DISCONNECTED);
                            putConnection(key, cs);
                        }
                    } else {
                        putConnection(key, cs);
                    }
                }
            }
            // 尝试创建一个连接
            LibraryDataAnalyzeRpcService.Client client = RpcUtil.getInRpcConnection(key.getIp(), key.getPort(), key.getTimeout());
            if (null != client) {
                try {
                    client.ping();
                    return new AvailableConnectionBean(key, new ClientStatus(client, ConnectionStatusCode.OK));
                } catch (TException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        if (flag) {
            // 取其他节点的RPC
            for (ConnectionManagerKey key1 : cluster.keySet()) {
                if (!key1.equals(key)) {
                    for (int i = 0; i < rpcPoolSize; i++) {
                        ClientStatus cs = cluster.get(key1).getRpcPool().poll();
                        if (null != cs) {
                            if (cs.getStatusCode() == ConnectionStatusCode.OK) {
                                try {
                                    cs.getConnection().ping();
                                    return new AvailableConnectionBean(key1, cs);
                                } catch (Exception e) {
                                    putConnection(key1, cs);
                                    logger.error(e.getMessage(), e);
                                }
                            } else {
                                putConnection(key1, cs);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 将rpc连接放回连接池
     * @param key ConnectionManagerKey
     * @param cs ClientStatus
     */
    public void putConnection(ConnectionManagerKey key, ClientStatus cs) {
        if (cluster.containsKey(key)) {
            cluster.get(key).getRpcPool().offer(cs);
        }
    }

    /**
     * 停止获取心跳
     */
    public void shutDown() {
        stopped = true;
    }
}
