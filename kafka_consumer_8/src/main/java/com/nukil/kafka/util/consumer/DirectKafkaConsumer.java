package com.nukil.kafka.util.consumer;

import com.nukil.kafka.util.bean.RowMessage;
import com.nukil.kafka.util.inter.ConsumerInterface;
import kafka.common.TopicAndPartition;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Nukil on 2017/12/04
 */
public class DirectKafkaConsumer implements Runnable, ConsumerInterface {
    private static MessageBuffer buffer = MessageBuffer.getInstance();
    private Properties properties;
    private boolean debug;
    private ConsumerConnector consumer;
    private Logger logger = Logger.getLogger(DirectKafkaConsumer.class);
    private ExecutorService executor;
    private int threadNum;

    public DirectKafkaConsumer(Properties properties) {
        this.properties = properties;
        setDefaultConfig();
        this.debug = Boolean.parseBoolean(this.properties.getProperty("enable.kafka.consumer.debug", "false").trim());
        this.threadNum = Integer.parseInt(this.properties.getProperty("kafka.fetch.thread.number", "8").trim());
        createConsumer();
    }

    @Override
    public void run() {
        String topicName = properties.getProperty("service.receive.topic.name");
        Map<String, Integer> topicCountMap = new HashMap<>();
        topicCountMap.put(topicName, threadNum);
        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer.createMessageStreams(topicCountMap);
        List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topicName);
        executor = Executors.newFixedThreadPool(threadNum);
        int number = 0;
        for (KafkaStream<byte[], byte[]> stream : streams) {
            if (debug) {
                logger.info(String.format("thread number %d start......", number));
            }
            executor.submit(new ConsumerThread(number, stream, Integer.parseInt(properties.getProperty("max.poll.records")), debug));
            number ++;
        }
    }

    @Override
    public void createConsumer() {
        try {
            ConsumerConfig config = new ConsumerConfig(properties);
            consumer = kafka.consumer.Consumer.createJavaConsumerConnector(config);
        } catch (Exception e) {
            logger.error("failed to create kafka consumer, sleep 30s and retry, error message is " + e.getMessage(), e);
            try {
                TimeUnit.SECONDS.sleep(30);
            } catch (Exception e1) {
                logger.error(e.getMessage(), e1);
            }
            createConsumer();
        }
    }

    @Override
    public List<RowMessage> fetchMessages() throws Exception {
        return buffer.getMessages();
    }

    @Override
    public boolean commitOffsets(Map<TopicAndPartition, Long> offsets) {
        consumer.commitOffsets();
        return true;
    }

    @Override
    public void shutDown() {
        if (null != consumer) {
            logger.info("try to shutdown consumer......");
            consumer.shutdown();
        }
        if (null != executor) {
            logger.info("try to shutdown executor.......");
            executor.shutdown();
        }
    }

    @Override
    public void setDefaultConfig() {
        Set keySet = properties.keySet();
        if (!keySet.contains("auto.commit.enable")) {
            properties.setProperty("auto.commit.enable", "false");
        }
    }
}