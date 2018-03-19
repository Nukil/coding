package com.nukil.kafka.util.bean;

public class SourceMessage {
    private String topic;
    private String key;
    private byte[] message;

    public SourceMessage(String topic, String key, byte[] message) {
        this.topic = topic;
        this.key = key;
        this.message = message;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }
}
