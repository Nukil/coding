package com.netposa.poseidon.face.bean;

import com.netposa.poseidon.face.rpc.inrpc.SetRecentDataInputRecord;

import java.util.concurrent.LinkedBlockingDeque;

public class ConnectionManagerValue {
    /**
     * rpc 连接池
     */
    private LinkedBlockingDeque<ClientStatus> rpcPool;


    public ConnectionManagerValue(int rpcPoolSize) {
        this.rpcPool = new LinkedBlockingDeque<>(rpcPoolSize);
    }

    public LinkedBlockingDeque<ClientStatus> getRpcPool() {
        return rpcPool;
    }

    public void setRpcPool(LinkedBlockingDeque<ClientStatus> rpcPool) {
        this.rpcPool = rpcPool;
    }

    public void putRpcPool(ClientStatus cs) {
        this.rpcPool.add(cs);
    }
}