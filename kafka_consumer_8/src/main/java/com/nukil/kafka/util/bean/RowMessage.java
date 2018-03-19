package com.nukil.kafka.util.bean;

import kafka.common.TopicAndPartition;

public class RowMessage {
    private TopicAndPartition topicAndPartition;
    private byte[] message;
    private long offset;
    public RowMessage(TopicAndPartition topicAndPartition, byte[] message, long offset) {
        this.topicAndPartition = topicAndPartition;
        this.message = message;
        this.offset = offset;
    }
    public TopicAndPartition topicAndPartition() {
        return this.topicAndPartition;
    }
    public byte[] message() {
        return this.message;
    }
    public long offset() {
        return this.offset;
    }
}