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
     * 发送一条消息
     * @param message 消息体
     * @param enableConfirm 是否需要确认消息发送成功
     * @return 发送失败消息列表
     */
    List<SourceMessage> sendMessage(SourceMessage message, boolean enableConfirm);

    /**
     * 发送批量数据
     * @param messages 消息列表
     * @param enableConfirm 是覅需要确认消息发送成功
     * @return 发送失败消息列表
     */
    List<SourceMessage> sendBatchMessage(List<SourceMessage> messages, boolean enableConfirm);

    /**
     * 用于停止producer
     */
    void shutDown();
}
