package com.netposa.poseidon.library.bean;

public class ConnectionManagerKey {
    /**
     * ip
     */
    private String ip;
    /**
     * port
     */
    private int port;
    /**
     * timeout
     */
    private int timeout;
    /**
     * hash值取值左值
     */
    private int startHashValue;
    /**
     * hash值取值右值
     */
    private int endHashValue;

    public ConnectionManagerKey(String ip, int port, int timeout, int startHashValue, int endHashValue) {
        this.ip = ip;
        this.port = port;
        this.timeout = timeout;
        this.startHashValue = startHashValue;
        this.endHashValue = endHashValue;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getStartHashValue() {
        return startHashValue;
    }

    public void setStartHashValue(int startHashValue) {
        this.startHashValue = startHashValue;
    }

    public int getEndHashValue() {
        return endHashValue;
    }

    public void setEndHashValue(int endHashValue) {
        this.endHashValue = endHashValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConnectionManagerKey key = (ConnectionManagerKey) o;
        if (this.ip != null ? !this.ip.equals(key.ip) : key.ip != null) {
            return false;
        }
        if (this.port != key.port) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return this.ip != null ? (this.ip.hashCode() + this.port) : 0;
    }
}
