package netposa.realtime.kafka.utils

import kafka.utils.Logging

/**
 * 线程处理过程中的异常处理工具
 */
class ThreadExceptionHandler extends Logging with Thread.UncaughtExceptionHandler{
  
  override def uncaughtException(t:Thread, e:Throwable){
    error("thread=>%d,error msg is=>%s".format(t.getId(),e.getMessage()), e)
  }

}