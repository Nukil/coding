package com.nukil.kafka.util.producer;


import com.nukil.kafka.util.bean.SourceMessage;

import java.util.ArrayList;
import java.util.List;

public class ErrorMessageBuffer {
    private static List<SourceMessage> errorMessageQueue = new ArrayList<>();
    private ErrorMessageBuffer() {}
    private static ErrorMessageBuffer instance = null;
    public static ErrorMessageBuffer getInstance() {
        if (null == instance) {
            synchronized (ErrorMessageBuffer.class) {
                if (null == instance) {
                    instance = new ErrorMessageBuffer();
                }
            }
        }
        return instance;
    }

    public List<SourceMessage> getErrorMessageQueue() {
        List<SourceMessage> errorMessages = new ArrayList<>();
        if (null != errorMessageQueue && errorMessageQueue.size() > 0) {
            synchronized (errorMessages) {
                errorMessages.addAll(errorMessageQueue);
                errorMessageQueue.clear();
            }
        }
        return errorMessages;
    }

    public void addErrorMessageQueue(SourceMessage errorMessage) {
        errorMessageQueue.add(errorMessage);
    }

    public int getErrorMessageListSize() {
        return errorMessageQueue.size();
    }
}
