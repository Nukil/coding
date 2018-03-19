package com.netposa.poseidon.library.bean;

public class AvailableConnectionBean {
    /**
     * 节点信息
     */
    private ConnectionManagerKey key;
    /**
     * 连接信息
     */
    private ClientStatus cs;

    public AvailableConnectionBean(ConnectionManagerKey key, ClientStatus cs) {
        this.key = key;
        this.cs = cs;
    }

    public ConnectionManagerKey getKey() {
        return key;
    }

    public void setKey(ConnectionManagerKey key) {
        this.key = key;
    }

    public ClientStatus getCs() {
        return cs;
    }

    public void setCs(ClientStatus cs) {
        this.cs = cs;
    }
}
