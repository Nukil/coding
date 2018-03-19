package com.netposa.poseidon.human.bean;

import com.netposa.poseidon.human.rpc.inrpc.RecentDataBean;
import com.netposa.poseidon.human.rpc.inrpc.SetRecentDataInputRecord;

import java.util.ArrayList;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class ConnectionManagerValue {
    /**
     * rpc 连接池
     */
    private LinkedBlockingDeque<ClientStatus> rpcPool;


    public ConnectionManagerValue(int rpcPoolSize) {
        this.rpcPool = new LinkedBlockingDeque<>(rpcPoolSize);
    }

    public ConnectionManagerValue(LinkedBlockingDeque<ClientStatus> rpcPool) {
        this.rpcPool = rpcPool;
    }

    public ConnectionManagerValue(LinkedBlockingDeque<ClientStatus> rpcPool, SetRecentDataInputRecord record) {
        this.rpcPool = rpcPool;
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