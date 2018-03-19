package com.nukil.kafka.util.consumer;

import com.nukil.kafka.util.bean.RowMessage;
import com.nukil.kafka.util.inter.ConsumerInterface;
import kafka.common.TopicAndPartition;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by Nukil on 2017/12/04
 */
public class DirectKafkaConsumer implements ConsumerInterface {
    private Properties properties;
    private boolean debug;
    private KafkaConsumer consumer;
    private Logger logger = Logger.getLogger(DirectKafkaConsumer.class);

    public DirectKafkaConsumer(Properties properties) {
        this.properties = properties;
        setDefaultConfig();
        this.debug = Boolean.parseBoolean(this.properties.getProperty("enable.kafka.consumer.debug", "false").trim());
        createConsumer();
    }

    @Override
    public void createConsumer() {
        try {
            consumer = new KafkaConsumer(properties);
            consumer.subscribe(Collections.singletonList(properties.getProperty("service.receive.topic.name")));
        } catch (Exception e) {
            logger.error("failed to create kafka consumer, sleep 30s and retry, error message is " + e.getMessage(), e);
            try {
                Thread.sleep(30000);
            } catch (Exception e1) {
                logger.error(e.getMessage(), e1);
            }
            createConsumer();
        }
    }

    @Override
    public List<RowMessage> fetchMessages() {
        List<RowMessage> messages = new ArrayList<>();
        ConsumerRecords<byte[], byte[]> records = consumer.poll(Long.parseLong(properties.getProperty("poll.timeout.ms", "3000")));
        for (ConsumerRecord<byte[], byte[]> record : records) {
            messages.add(new RowMessage(new TopicAndPartition(record.topic(), record.partition()), record.value(), record.offset()));
        }
        return messages;
    }

    @Override
    public boolean commitOffsets(Map<TopicAndPartition, Long> offsets) {
        for (TopicAndPartition key : offsets.keySet()) {
            final Map<TopicPartition, OffsetAndMetadata> offset = new HashMap<>();
            offset.put(new TopicPartition(key.topic(), key.partition()), new OffsetAndMetadata(offsets.get(key) + 1));
            consumer.commitAsync(offset, new OffsetCommitCallback() {
                @Override
                public void onComplete(Map<TopicPartition, OffsetAndMetadata> map, Exception e) {
                    if (null != e) {
                        logger.error(e.getLocalizedMessage(), e);
                    }
                    if (debug) {
                        logger.info("submit offset : " + offset);
                    }
                }
            });
        }
        return true;
    }

    @Override
    public void shutDown() {
        if (null != consumer) {
            logger.info("try to shutdown consumer......");
            consumer.close();
        }
    }

    @Override
    public void setDefaultConfig() {
        Set keySet = properties.keySet();
        if (!keySet.contains("enable.auto.commit")) {
            properties.setProperty("enable.auto.commit", "false");
        }
        if (!keySet.contains("value.deserializer")) {
            properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        }
        if (!keySet.contains("key.deserializer")) {
            properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        }
    }
}