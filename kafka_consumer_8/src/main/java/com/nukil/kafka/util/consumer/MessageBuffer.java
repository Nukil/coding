package com.nukil.kafka.util.consumer;
import com.nukil.kafka.util.bean.RowMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class MessageBuffer {

    private static BlockingQueue<RowMessage> messages = new LinkedBlockingDeque<>();

    private MessageBuffer() {
    }

    private static MessageBuffer instance = null;

    public static MessageBuffer getInstance() {
        if (null == instance) {
            synchronized (MessageBuffer.class) {
                if (null == instance) {
                    instance = new MessageBuffer();
                }
            }
        }
        return instance;
    }

    public List<RowMessage> getMessages() throws Exception {
        List<RowMessage> message = new ArrayList<>();
        int messageSize = messages.size();
        if (messageSize > 0) {
            for (int i = 0; i < messageSize; ++i) {
                message.add(messages.take());
            }
        }
        return message;
    }

    public int getMessagesSize() {
        return messages.size();
    }

    void putMessages(RowMessage message) {
        messages.add(message);
    }
}
