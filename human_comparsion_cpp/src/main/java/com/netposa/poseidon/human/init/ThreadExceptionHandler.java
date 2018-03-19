package com.netposa.poseidon.human.init;

import org.apache.log4j.Logger;

public class ThreadExceptionHandler implements Thread.UncaughtExceptionHandler {
    private Logger logger = Logger.getLogger(this.getClass());
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        logger.error(String.format("thread => %d, error msg is => %s, %s", t.getId(), e.getMessage(), e));
        Thread newThread = new Thread(t, t.getName());
        newThread.setUncaughtExceptionHandler(this);
        newThread.start();
    }
}
