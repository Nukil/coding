package com.netposa.poseidon.face.util;

import org.apache.log4j.Logger;
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
    public static com.netposa.poseidon.face.rpc.inrpc.FaceFeatureDataAnalyzeRpcService.Client getInRpcConnection(String host, int port, int timeout) {
        com.netposa.poseidon.face.rpc.inrpc.FaceFeatureDataAnalyzeRpcService.Client client = null;
        TTransport transport = new TSocket(host, port, timeout);
        transport = new TFramedTransport(transport);
        TProtocol protocol = new TCompactProtocol(transport);
        try {
            transport.open();
            client = new com.netposa.poseidon.face.rpc.inrpc.FaceFeatureDataAnalyzeRpcService.Client(protocol);
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
    public static com.netposa.poseidon.face.rpc.outrpc.FaceFeatureDataAnalyzeRpcService.Client getOutRpcConnection(String host, int port, int timeout) {
        com.netposa.poseidon.face.rpc.outrpc.FaceFeatureDataAnalyzeRpcService.Client client = null;
        TTransport transport = new TSocket(host, port, timeout);
        transport = new TFramedTransport(transport);
        TProtocol protocol = new TCompactProtocol(transport);
        try {
            transport.open();
            client = new com.netposa.poseidon.face.rpc.outrpc.FaceFeatureDataAnalyzeRpcService.Client(protocol);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return client;
    }
}
