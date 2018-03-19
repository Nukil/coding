package com.nukil.kafka.util.inter;

import com.nukil.kafka.util.bean.SourceMessage;

import java.util.List;

public interface ProducerInterface {
    /**
     * 增加默认kafka设置
     */
    void setDefaultConfig();

    /**
     * 用于获取Producer,需要在初始化类的时候传入properties
     */
    void createProducer();

    /**
     * 发送一条数据
     * @param message 消息体
     * @param enableConfirm 是否需要确认消息发送成功
     * @return null or errorMessageList
     */
    List<SourceMessage> sendMessage(SourceMessage message, boolean enableConfirm);

    /**
     * 批量数据发送接口
     * @param messages 待发送数据集合
     * @return null or errorMessageList
     */
    List<SourceMessage> sendBatchMessage(List<SourceMessage> messages, boolean enableConfirm);

    /**
     * 用于停止producer
     */
    void shutDown();
}
