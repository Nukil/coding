package com.nukil.kafka.util.consumer;

import com.nukil.kafka.util.bean.RowMessage;
import kafka.common.TopicAndPartition;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.message.MessageAndMetadata;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;

public class ConsumerThread implements Runnable {
    private boolean debug;
    private int threadNum;
    private int maxMessagesSize;
    private KafkaStream<byte[], byte[]> stream;
    private static MessageBuffer instace = MessageBuffer.getInstance();
    private static Logger logger = Logger.getLogger(ConsumerThread.class);

    ConsumerThread(int threadNum, KafkaStream<byte[], byte[]> stream, int maxMessagesSize, boolean debug) {
        this.debug = debug;
        this.threadNum = threadNum;
        this.stream = stream;
        this.maxMessagesSize = maxMessagesSize;
    }

    @Override
    public void run() {
        ConsumerIterator<byte[], byte[]> iter = stream.iterator();
        while (iter.hasNext()) {
            if (debug) {
                logger.info(String.format("stream message, thread number %d fetch message", threadNum));
            }
            while (instace.getMessagesSize() >= maxMessagesSize) {
                if (debug) {
                    logger.info(String.format("thread %d, message queue is already full, please fetch message first, message queue size is %d", threadNum, instace.getMessagesSize()));
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            MessageAndMetadata<byte[], byte[]> message = iter.next();
            instace.putMessages(new RowMessage(new TopicAndPartition(message.topic(), message.partition()), message.message(), message.offset()));
        }
    }
}
