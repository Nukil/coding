package com.netposa.poseidon.fakecardetect.service

import java.io.InputStream
import java.util.Properties

import com.netposa.poseidon.fakecardetect.bean.{InputMsg, ResultRecord}
import com.netposa.poseidon.fakecardetect.main.FakeCarDetectMainServer
import com.netposa.poseidon.fakecardetect.main.FakeCarDetectMainServer.{broadcastList, iniRDD, kafka_Proper, propConf}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.{Seconds, State, StateSpec, StreamingContext}
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

object MessageHandler {
  val LOG = LoggerFactory.getLogger(FakeCarDetectMainServer.getClass)
  def msgHandler(streams:InputDStream[ConsumerRecord[Array[Byte], Array[Byte]]]): Unit ={
    val allRecords=streams.mapPartitions(res=>{
      res.map(data=>(new DataParserService).dataParser(data.value()))})
      .map(res=>{
        val key= res.getRcgHphm()+"-"+res.getHpys()+"-"+res.getHeadRear()
        //println(key)
        val value= res
      //  println(res.toString)
        (key,value)
      }).filter(f=>{
      val flag = f._2!=null || f._1!=null
      flag
    })
//    println(allRecords)
    val mappingFunc = (key: String, newState: Option[InputMsg], state: State[InputMsg]) => {
      val newSta =  newState.get
      var output:(String,ArrayBuffer[ResultRecord]) = null
      // 获取之前状态的值
      state.getOption() match {
        case Some(res) =>
          //获取旧记录res
          //获取新记录的ResultRecord
          val ncleanData = (new DataCleanService).clean(newSta)
          val ncreatKV =  (new CreateKeyAndValueService).create(ncleanData)
          //获取旧记录的ResultRecord
          val ocleanData = (new DataCleanService).clean(res)
          val ocreatKV =  (new CreateKeyAndValueService).create(ocleanData)
          //判断新旧记录是否相同
          val flag = compare(res,newSta)
          if(!flag){
            //套牌分析
            output = (new CompareService).compare((key,ncreatKV),(key,ocreatKV))
          }else{
            //更新状态
            state.update(newSta)
          }
        case None =>
          //无旧记录，直接更新状态
          state.update(newSta)
      }
      //返回kv
      output
    }
    val states = allRecords.mapWithState(StateSpec.function(mappingFunc).initialState(iniRDD))
    states.foreachRDD(stat=>{
      if (stat!=null&&stat.count()!=0){
        val scol = stat.filter(v=>(
          v !=null && v._1 !=null && !v._1.contains(propConf.exclude_plate_startwiths))).collect()
//        val scol = stat.filter(v=>(v !=null && v._1 !=null)).collect()
        val mapcol = scol.map(_._2(0))
          Send2DBService.sendFakeCar2DB(mapcol)
      }
    })
  }


  def createSource(sparkContx: StreamingContext, kafkaParams: Map[String,String]): InputDStream[ConsumerRecord[Array[Byte], Array[Byte]]] = {
    try {
      val streams = {
        KafkaUtils.
          createDirectStream[Array[Byte], Array[Byte]](
          sparkContx,
          LocationStrategies.PreferConsistent,
          ConsumerStrategies.Subscribe[Array[Byte], Array[Byte]](Set(kafka_Proper.get("service.receive.topic.name").get), kafka_Proper)
        )
      }
      broadcastList = sparkContx.sparkContext.broadcast(parma.getconf())
      streams.checkpoint(Seconds(10))
      streams
    }catch {
      case e:Exception =>
        error("create Spark kafka stream error ,info is %s".format(e.getMessage))
    }

  }


  def compare(inputMsg1: InputMsg,inputMsg2:InputMsg): Boolean ={
    val str1 = inputMsg1.toString
    val str2 = inputMsg2.toString
   // println(str1)
  //  println(str2)
    if (str1.equals(str2)) true else false
  }

/**
  * 比对两个车牌号码的相似度,并返回一个0-100之间的数值
  * @param arr1
  * @param arr2
  * @return
  */
def levenshtein(arr1:Array[Byte],arr2:Array[Byte]):Float={
  // 计算两个字符串的长度
  val len1:Int = arr1.length
  val len2:Int = arr2.length
  // 建立上面说的数组，比字符长度大一个空间
  var dif = Array.ofDim[Int](len1+1,len2+1)
  // 赋初值
  for(a:Int <- 0 to len1){
  dif(a)(0) = a
}
  for(a:Int <- 0 to len2){
  dif(0)(a) = a
}
  // 计算两个字符是否一样，计算左上的值
  var temp:Int = -1
  for(i<-1 to len1){
  for(j<-1 to len2){
  if(arr1(i-1) == arr2(j-1)){
  temp = 0
}else{
  temp = 1
}
  // 取三个值中最小的
  dif(i)(j) = min(dif(i - 1)(j - 1) + temp, dif(i)(j - 1) + 1, dif(i - 1)(j) + 1)
}
}
  // 计算相似度
  val max_length = Math.max(arr1.length, arr2.length)
  val similarity = (1 - dif(len1)(len2).asInstanceOf[Float] / max_length) * 100
  similarity
}
  private def min(in:Int*):Int={
  var min = Integer.MAX_VALUE
  for(i<-in){
  if(min>i){
  min = i
}
}
  min
}

  object parma{
    val p:Properties =new Properties()
    def getconf():Properties ={
      val s:Properties =new Properties()
      s.load(getClass.getClassLoader.getResourceAsStream("kafka.properties"))
      p
    }

    def setConf(inputS:InputStream):Unit={
//      println(inputS)
      p.load(inputS)
    }

  }


}
