package com.nukil.kafka.util.inter;

import com.nukil.kafka.util.bean.RowMessage;
import kafka.common.TopicAndPartition;

import java.util.List;
import java.util.Map;

/**
 * Created by Nukil on 2017/12/04
 */
public interface ConsumerInterface {
    /**
     * 增加默认kafka设置
     */
    void setDefaultConfig();

    /**
     * 用于获取Consumer,需要在初始化类的时候传入properties
     */
    void createConsumer();

    /**
     * 拉取Kafka数据
     * @return 消息List<byte>
     */
    List<RowMessage> fetchMessages() throws Exception;

    /**
     * 用于手动提交offsets
     * @return true or false
     */
    boolean commitOffsets(Map<TopicAndPartition, Long> offsets);

    /**
     * 用于停止Kafka拉取线程
     */
    void shutDown() throws Exception;
}