package com.netposa.poseidon.human.bean;

import com.netposa.poseidon.human.rpc.inrpc.HumanFeatureDataAnalyzeRpcService;

public class ClientStatus {
    /**
     * rpc连接对象
     */
    private HumanFeatureDataAnalyzeRpcService.Client connection;
    /**
     * 连接状态
     */
    private ConnectionStatusCode statusCode;

    public ClientStatus(){}

    public ClientStatus(HumanFeatureDataAnalyzeRpcService.Client client, ConnectionStatusCode statusCode) {
        this.connection = client;
        this.statusCode = statusCode;
    }

    public HumanFeatureDataAnalyzeRpcService.Client getConnection() {
        return connection;
    }

    public void setConnection(HumanFeatureDataAnalyzeRpcService.Client connection) {
        this.connection = connection;
    }

    public ConnectionStatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(ConnectionStatusCode statusCode) {
        this.statusCode = statusCode;
    }
}
