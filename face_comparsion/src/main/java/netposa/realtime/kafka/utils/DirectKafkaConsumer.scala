package netposa.realtime.kafka.utils

import java.util
import java.util.concurrent.TimeUnit
import java.util.{Collections, Properties}

import kafka.common.TopicAndPartition
import kafka.utils.Logging
import org.apache.kafka.clients.consumer.{KafkaConsumer, OffsetAndMetadata, OffsetCommitCallback}
import org.apache.kafka.common.TopicPartition

import scala.collection.{JavaConversions, mutable}
import scala.collection.JavaConversions._
import scala.collection.mutable.{Buffer, ListBuffer}
import java.util.{List => JList}

class DirectKafkaConsumer(val props: Properties,val topic: String) extends Logging {

  val kafka =getConsumer

  private def getConsumer(): KafkaConsumer[Array[Byte], Array[Byte]] = {
    try {
      createConsumer()
    } catch {
      case e: Exception =>
        error(s"create kafka consumer failed! sleep 30s and try again. info is ${e.getLocalizedMessage}",e)
        TimeUnit.SECONDS.sleep(30.toLong)
        getConsumer()
    }
  }

  private def createConsumer():KafkaConsumer[Array[Byte],Array[Byte]]={
    val kafka_prop =new Properties()
    props.stringPropertyNames().foreach(key => {
      if (key.startsWith("kafka.")) {
        val key_p = key.stripPrefix("kafka.")
        val va = props.getProperty(key)
         key_p  match {
           case "auto.offset.reset"=>
              va.toLowerCase match {
                case "smallest" => kafka_prop.setProperty("auto.offset.reset", "earliest")
                case "largest" => kafka_prop.setProperty("auto.offset.reset", "latest")
                case _ => kafka_prop.put(key_p, va)
              }
           case "metadata.broker.list" =>kafka_prop.setProperty("bootstrap.servers", va)
           case _ =>   kafka_prop.put(key_p, va)
         }
      }
    })
    kafka_prop.setProperty("group.id",kafka_prop.getProperty(String.format("%s.group.id",topic)))
    //设置brokerServer(kafka)ip地址
    setDefultProps(kafka_prop,"enable.auto.commit", "false");
    // setDefultProps(props,"auto.offset.reset", "earliest")
    setDefultProps(kafka_prop,"key.deserializer","org.apache.kafka.common.serialization.ByteArrayDeserializer")
    setDefultProps(kafka_prop,"value.deserializer"," org.apache.kafka.common.serialization.ByteArrayDeserializer")
    setDefultProps(kafka_prop,"max.poll.records", "1000")
    println(kafka_prop)
    //topic msg
    val kafkaConsumer = new KafkaConsumer[Array[Byte],Array[Byte]](kafka_prop)
    kafkaConsumer.subscribe(JavaConversions.asJavaCollection(mutable.Buffer(kafka_prop.getProperty(topic + ".service.receive.topic.name"))))
    kafkaConsumer
  }


  def fetchMessages():Buffer[RowMessage]={
    val buffer = Buffer[RowMessage]()
    val allPartis =kafka.poll(props.getProperty("poll.timeout.ms","3000").toLong)
    allPartis.partitions().foreach(par=>{
      allPartis.records(par).foreach(v=>{
        buffer +=RowMessage(TopicAndPartition(par.topic(),par.partition()),v.value(),v.offset())
      })
    })
//    info(s"====This  batch  has fetched  Message  size is  [${buffer.size}]====")
    buffer
  }

  /**
    * 关闭消息接收服务
    */
  def shutdown() {
    kafka.close()
  }

  def commitOffsets(offsets: Map[TopicAndPartition, Long]):Unit={
    offsets.foreach(v=>{
      val offset= Collections.singletonMap(new TopicPartition(v._1.topic,v._1.partition),new OffsetAndMetadata(v._2+1))
      kafka.commitAsync(offset,new OffsetCommitCallback {
        override def onComplete(offsets: util.Map[TopicPartition, OffsetAndMetadata], exception: Exception) = {
          if(exception !=null) {
            error(s"commit offset failed! info is ${exception.getLocalizedMessage}")
          }
          debug(s"commit offset is ${offset}")
        }
      })
    })
  }

  private def setDefultProps(pro:Properties,key:String,value:String):Properties={
    if(!pro.containsKey(key)){
      pro.put(key,value)
    }
    props
  }

  private def initProps(): Map[String, String] = {
    val arr = new ListBuffer[(String, String)]()
    val propNames = {
      import JavaConversions._
      Collections.list(props.propertyNames).map(_.toString).sorted
    }
    propNames.foreach(key => {
      info("load properties key is [%s], value is [%s]".format(key, props.getProperty(key)))
      val row = (key, props.getProperty(key))
      arr += row
    })
      arr.toMap
  }
}
case class RowMessage(val tp:TopicAndPartition, val message:Array[Byte], val offset:Long) {
}