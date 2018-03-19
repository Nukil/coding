package com.netposa.poseidon.face.bean;

import com.netposa.poseidon.face.rpc.inrpc.FaceFeatureDataAnalyzeRpcService;

public class ClientStatus {
    /**
     * rpc连接对象
     */
    private FaceFeatureDataAnalyzeRpcService.Client connection;
    /**
     * 连接状态
     */
    private ConnectionStatusCode statusCode;

    public ClientStatus(){}

    public ClientStatus(FaceFeatureDataAnalyzeRpcService.Client client, ConnectionStatusCode statusCode) {
        this.connection = client;
        this.statusCode = statusCode;
    }

    public FaceFeatureDataAnalyzeRpcService.Client getConnection() {
        return connection;
    }

    public void setConnection(FaceFeatureDataAnalyzeRpcService.Client connection) {
        this.connection = connection;
    }

    public ConnectionStatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(ConnectionStatusCode statusCode) {
        this.statusCode = statusCode;
    }
}
