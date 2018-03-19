package com.netposa.poseidon.face.init

import org.slf4j.{Logger, LoggerFactory}


/**
  * 线程处理过程中的异常处理工具
  */
class ThreadExceptionHandler extends Thread.UncaughtExceptionHandler {
    val LOG: Logger = LoggerFactory.getLogger("ThreadExceptionHandler")
    override def uncaughtException(t: Thread, e: Throwable) {
        LOG.error("thread=>%d,error msg is=>%s".format(t.getId, e.getMessage), e)
        val newThread = new Thread(t, t.getName)
        newThread.setUncaughtExceptionHandler(this)
        newThread.start()
    }
}
