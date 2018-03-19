package com.nukil.kafka.util.producer;
import com.nukil.kafka.util.bean.SourceMessage;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.log4j.Logger;

public class ProducerCallBack<K, V> implements Callback {
    private final Logger logger = Logger.getLogger(ProducerCallBack.class);
    private ErrorMessageBuffer instance = ErrorMessageBuffer.getInstance();
    private final long start;
    private final String topic;
    private final String key;
    private final byte[] value;
    private final boolean debug;

    ProducerCallBack(long start, String topic, String key, byte[] value, boolean debug) {
        this.start = start;
        this.topic = topic;
        this.key = key;
        this.value = value;
        this.debug = debug;
    }

    /**
     * 生产者成功发送消息，收到Kafka服务端发来的ACK确认消息后，会调用此回调函数
     *
     * @param recordMetadata 生产者发送的消息的元数据，如果发送过程中出现异常，此参数为null
     * @param exception 发送过程中出现的异常，如果发送成功，则此参数为null
     */
    @Override
    public void onCompletion(RecordMetadata recordMetadata, Exception exception) {
        if (debug) {
            if (recordMetadata != null) {
                logger.info(String.format("message key : %s, value : %s, sent to topic : %s, partition : %s, offset : %s, use time : %s ms",
                        key, new String(value), recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset(), System.currentTimeMillis()-start));
            }
        }
        if (null != exception) {
            logger.error(String.format("fail to send message, key : %s, value : %s", key, new String(value)));
        }
        if (null == recordMetadata) {
            instance.addErrorMessageQueue(new SourceMessage(topic, key, value));
        }
    }
}
