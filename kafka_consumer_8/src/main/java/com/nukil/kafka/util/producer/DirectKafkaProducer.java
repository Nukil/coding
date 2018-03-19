package com.nukil.kafka.util.producer;

import com.nukil.kafka.util.bean.SourceMessage;
import com.nukil.kafka.util.inter.ProducerInterface;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class DirectKafkaProducer implements ProducerInterface {
    private Properties properties;
    private boolean debug;
    private Logger logger = Logger.getLogger(DirectKafkaProducer.class);
    private Producer<byte[], byte[]> producer;
    private ErrorMessageBuffer instance = ErrorMessageBuffer.getInstance();
    private int maxErrorMessageListSize;

    public DirectKafkaProducer(Properties properties) {
        this.properties = properties;
        debug = Boolean.parseBoolean(properties.getProperty("enable.kafka.producer.debug", "false"));
        maxErrorMessageListSize = Integer.parseInt(properties.getProperty("max.error.message.size", "10000"));
        setDefaultConfig();
        createProducer();
    }

    @Override
    public void setDefaultConfig() {
        Set keySet = properties.keySet();
        if (!keySet.contains("key.serializer")) {
            properties.setProperty("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        }
        if (!keySet.contains("value.serializer")) {
            properties.setProperty("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        }
    }

    @Override
    public void createProducer() {
        try {
            producer = new KafkaProducer<>(properties);
        } catch (Exception e) {
            logger.error("failed to create kafka producer, sleep 30s and retry, error message is " + e.getMessage(), e);
            try {
                TimeUnit.SECONDS.sleep(30);
            } catch (Exception e1) {
                logger.error(e.getMessage(), e);
            }
            createProducer();
        }
    }

    @Override
    public List<SourceMessage> sendMessage(SourceMessage message, boolean enableConfirm) {
        List<SourceMessage> errorMessageList = new ArrayList<>();
        if (instance.getErrorMessageListSize() > maxErrorMessageListSize) {
            if (debug) {
                logger.warn("==============error message list is already full==============");
            }
            errorMessageList.add(message);
        } else {
            if (enableConfirm) {
                ProducerCallBack callBack = new ProducerCallBack<>(System.currentTimeMillis(), message.getTopic(), message.getKey(), message.getMessage(), debug);
                producer.send(new ProducerRecord<byte[], byte[]>(message.getTopic(), message.getKey().getBytes(), message.getMessage()), callBack);
            } else {
                producer.send(new ProducerRecord<byte[], byte[]>(message.getTopic(), message.getKey().getBytes(), message.getMessage()));
            }
        }
        if (instance.getErrorMessageListSize() > 0) {
            errorMessageList.addAll(instance.getErrorMessageQueue());
        }
        return errorMessageList;
    }

    @Override
    public List<SourceMessage> sendBatchMessage(List<SourceMessage> messages, boolean enableConfirm) {
        List<SourceMessage> errorMessageList = new ArrayList<>();
        if (instance.getErrorMessageListSize() > maxErrorMessageListSize) {
            if (debug) {
                logger.warn("==============error message list is already full==============");
            }
            errorMessageList.addAll(messages);
        } else {
            for (SourceMessage message : messages) {
                List<SourceMessage> errorList = sendMessage(message, enableConfirm);
                if (errorList.size() > 0) {
                    errorMessageList.addAll(errorList);
                }
            }
        }
        return errorMessageList;
    }

    @Override
    public void shutDown() {
        producer.close();
    }
}