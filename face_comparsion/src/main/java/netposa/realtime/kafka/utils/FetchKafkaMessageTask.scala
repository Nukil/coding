package netposa.realtime.kafka.utils

import java.util.concurrent.{ForkJoinTask, RecursiveTask}

import kafka.api.FetchRequestBuilder
import kafka.common.{ErrorMapping, TopicAndPartition}
import kafka.consumer.SimpleConsumer
import kafka.message.MessageAndOffset
import kafka.utils.Logging
import org.apache.kafka.common.utils.Utils

import scala.collection.JavaConversions._
import scala.collection.mutable.{Buffer, ListBuffer}

/**
 * 读取KAFKA消息的线程实现
 */
class FetchKafkaMessageTask(
    val partitions:Array[Partition],val partition:Partition) 
        extends RecursiveTask[Buffer[RowMessage]] with Logging {
  
  override def compute():Buffer[RowMessage]={
    partition match {
      case null => {
        //开始进行并行处理,这里相当于一个递归
        val buffer = new ListBuffer[RowMessage]()
        val tasks = partitions.map(part => new FetchKafkaMessageTask(null,part))
        val results = ForkJoinTask.invokeAll(tasks.toList)
        results.foreach(result => {
          val tmp = result.join()
          if (tmp != null) {
            buffer ++= tmp
          }
        })
        buffer.toBuffer
      }
      case _ => {
        //直接执行当前的partition
        val messages = fetchBatch
        if (null == messages || messages.size < 1) {
          return null
        }
        val buffer = new ListBuffer[RowMessage]()
        val tp = new TopicAndPartition(partition.topic,partition.partition)
        val itor = messages.iterator
        while(itor.hasNext) {
          val item = itor.next
          val message = Utils.readBytes(item.message.payload)
          buffer += new RowMessage(tp,message,item.offset)
          partition.current_offset = item.nextOffset
        }
        buffer.toBuffer
      }
    }
  }
  
  
  
  private def fetchBatch: Buffer[MessageAndOffset] = {
    val consumer = connectPartitionLeader
    if (null == consumer) {
      warn("consumer is null, skip this fetch message by topic [%s] and partition [%d]"
          .format(partition.topic,partition.partition))
      return null
    }
    val req = new FetchRequestBuilder()
        .addFetch(partition.topic, partition.partition,
            partition.current_offset, 
            partition.kc.config.fetchMessageMaxBytes)
        .build()
    val resp = consumer.fetch(req)
    if (!resp.hasError) {
      // kafka may return a batch that starts before the requested offset
      val messages = resp.messageSet(partition.topic, partition.partition)
          .iterator
          .dropWhile(_.offset < partition.current_offset).toBuffer
      
      if (partition.kc.debug) {
        info("get message size is [%d] by topic [%s] partition [%d]"
            .format(messages.size,partition.topic, partition.partition))
      }
      return messages
    }else {
      try {
        val err = resp.errorCode(partition.topic, partition.partition)
        handleFetchErr(err)
      }catch {
        case e:Throwable => {
          warn("get topic [%s] and partition [%d] consumer leader error, msg is %s"
              .format(partition.topic, partition.partition,e.getMessage()),e)
        }
      }//end try catch
      return null
    }//end if else
  }
  
  
  private def connectPartitionLeader():SimpleConsumer={
    if (partition.consumer != null) {
      if (partition.kc.debug) {
        info("get this cache partition consumer by topic [%s] and partition [%d]"
            .format(partition.topic,partition.partition))
      }
      return partition.consumer
    }

    partition.attemp = if (partition.max_attemp < 1)
        {1} else {partition.max_attemp}
    while(partition.attemp > 0) {
      try {
        partition.consumer = connectLeader
        partition.attemp = 0
        if (partition.kc.debug) {
          info("get this new connect partition consumer by topic [%s] and partition [%d]"
              .format(partition.topic,partition.partition))
        }
      }catch {
        case e:Throwable => {
          partition.attemp -= 1
          warn("get topic [%s] and partition [%d] consumer leader error, retry is [%d] msg is %s"
              .format(partition.topic,partition.partition, partition.attemp, e.getMessage()),e)
          Thread.sleep(partition.kc.config.refreshLeaderBackoffMs)
        }
      }
    }
    partition.consumer
  }
  
  // The idea is to use the provided preferred host, except on task retry atttempts,
  // to minimize number of kafka metadata requests
  private def connectLeader: SimpleConsumer = {
    if (partition.attemp > 0) {
      partition.kc.findLeader(partition.topic, partition.partition).right.map(
        hp => {
          partition.broker_host = hp._1
          partition.broker_port = hp._2
          partition.kc.connect(hp._1, hp._2)
        }
      ).fold(
          errs => throw new Exception(
            s"Couldn't connect to leader for topic ${partition.topic} ${partition.partition}: " +
              errs.mkString("\n")),
          consumer => consumer
      )
    } else {
      partition.kc.connect(partition.broker_host, partition.broker_port)
    }
  }

  private def handleFetchErr(err: Short) {
    if (err == ErrorMapping.LeaderNotAvailableCode ||
          err == ErrorMapping.NotLeaderForPartitionCode) {
      try {
        partition.consumer.close
      }catch {
        case e:Throwable => {
          warn("close topic [%s] and partition [%d] old consumer error, msg is %s"
              .format(partition.topic, partition.partition,e.getMessage()),e)
        }
      }
      partition.consumer = null
      
      error(s"Lost leader for topic ${partition.topic} partition ${partition.partition}, " +
            s" sleeping for ${partition.kc.config.refreshLeaderBackoffMs}ms")
      //Thread.sleep(partition.kc.config.refreshLeaderBackoffMs)
    }
    // Let normal rdd retry sort out reconnect attempts
    throw ErrorMapping.exceptionFor(err)
  }

}