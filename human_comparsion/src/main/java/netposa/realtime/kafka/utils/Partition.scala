package netposa.realtime.kafka.utils

import kafka.consumer.SimpleConsumer

class Partition(val topic:String, val partition:Int, 
    val max_attemp:Int, val kc:DirectKafkaCluster) {
  var attemp = max_attemp
  var commit_offset:Long = 0
  var current_offset:Long = 0
  var broker_host:String = null
  var broker_port:Int = 0
  var consumer:SimpleConsumer=null
  
  override def toString():String={
    s"topic=[$topic],partition=[$partition],commit_offset=[$commit_offset]," +
       s"current_offset=[$current_offset],host:port=[${broker_host}:${broker_port}]"
  }
}