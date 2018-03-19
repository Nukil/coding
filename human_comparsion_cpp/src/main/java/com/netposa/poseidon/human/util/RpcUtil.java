package com.netposa.poseidon.human.util;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

public class RpcUtil {
    private static Logger logger = Logger.getLogger(RpcUtil.class);

    /**
     * 用于获取Master与Server之间的rpc连接
     * @param host ip
     * @param port port
     * @param timeout 超时时间 ms
     * @return com.netposa.poseidon.human.rpc.inrpc.HumanFeatureDataAnalyzeRpcService.Client
     */
    public static com.netposa.poseidon.human.rpc.inrpc.HumanFeatureDataAnalyzeRpcService.Client getInRpcConnection(String host, int port, int timeout) {
        com.netposa.poseidon.human.rpc.inrpc.HumanFeatureDataAnalyzeRpcService.Client client = null;
        TTransport transport = new TSocket(host, port, timeout);
        transport = new TFramedTransport(transport);
        TProtocol protocol = new TCompactProtocol(transport);
        try {
            transport.open();
            client = new com.netposa.poseidon.human.rpc.inrpc.HumanFeatureDataAnalyzeRpcService.Client(protocol);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return client;
    }

    /**
     * 用于获取对外Rpc连接
     * @param host ip
     * @param port port
     * @param timeout 超时时间 ms
     * @return com.netposa.poseidon.human.rpc.outrpc.HumanFeatureDataAnalyzeRpcService.Client
     */
    public static com.netposa.poseidon.human.rpc.outrpc.HumanFeatureDataAnalyzeRpcService.Client getOutRpcConnection(String host, int port, int timeout) {
        com.netposa.poseidon.human.rpc.outrpc.HumanFeatureDataAnalyzeRpcService.Client client = null;
        TTransport transport = new TSocket(host, port, timeout);
        transport = new TFramedTransport(transport);
        TProtocol protocol = new TCompactProtocol(transport);
        try {
            transport.open();
            client = new com.netposa.poseidon.human.rpc.outrpc.HumanFeatureDataAnalyzeRpcService.Client(protocol);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return client;
    }
}
