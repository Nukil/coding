package com.netposa.poseidon.library.bean;

import com.netposa.poseidon.library.rpc.inrpc.LibraryDataAnalyzeRpcService;

public class ClientStatus {
    /**
     * rpc连接对象
     */
    private LibraryDataAnalyzeRpcService.Client connection;
    /**
     * 连接状态
     */
    private ConnectionStatusCode statusCode;

    public ClientStatus(){}

    public ClientStatus(LibraryDataAnalyzeRpcService.Client client, ConnectionStatusCode statusCode) {
        this.connection = client;
        this.statusCode = statusCode;
    }

    public LibraryDataAnalyzeRpcService.Client getConnection() {
        return connection;
    }

    public void setConnection(LibraryDataAnalyzeRpcService.Client connection) {
        this.connection = connection;
    }

    public ConnectionStatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(ConnectionStatusCode statusCode) {
        this.statusCode = statusCode;
    }
}
