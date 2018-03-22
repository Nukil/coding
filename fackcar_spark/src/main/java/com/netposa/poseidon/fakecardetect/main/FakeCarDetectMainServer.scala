package com.netposa.poseidon.fakecardetect.main

import java.io.InputStream
import java.util
import java.util.{Calendar, Properties}
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

import com.netposa.poseidon.fakecardetect.bean._
import com.netposa.poseidon.fakecardetect.service.MessageHandler.parma
import com.netposa.poseidon.fakecardetect.service._
import com.netposa.poseidon.fakecardetect.utils._
import org.apache.hadoop.conf.Configuration
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.log4j.{Level, Logger}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.{SparkConf, SparkContext, TaskContext, rdd}
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, State, StateSpec, StreamingContext}
import org.slf4j.LoggerFactory

import scala.reflect.io.Path

object FakeCarDetectMainServer {
  val LOG = LoggerFactory.getLogger(FakeCarDetectMainServer.getClass)
  Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
  Logger.getLogger("org.eclipse.jetty.server").setLevel(Level.OFF)
  Logger.getLogger("org.apache.kafka.clients.consumer.internals.AbstractCoordinator").setLevel(Level.OFF)
  Logger.getLogger("org.apache.kafka.common.utils.AppInfoParser").setLevel(Level.OFF)
  Logger.getLogger("org.apache.kafka.clients.consumer.ConsumerConfig").setLevel(Level.OFF)
  Logger.getLogger("org.spark_project.jetty.server.handler.ContextHandler").setLevel(Level.OFF)
  Logger.getLogger("org.apache.spark.executor.Executor").setLevel(Level.OFF)
  Logger.getLogger("  org.apache.spark.streaming.kafka010.KafkaRDD").setLevel(Level.OFF)
  //加载spark和kafka以及服务配置文件和子品牌字典表
  val spark_Proper = LoadInitUtil.initProp2Map("spark.properties")
  val kafka_Proper = LoadInitUtil.initProp2Map("kafka.properties")
  val server_proper = LoadInitUtil.initProps("server.properties")
  val childLogoDic = LoadInitUtil.loadVehicleSubBrand()
  val logoDic = LoadInitUtil.loadVehicleBrand()
  val propConf = new PropConfig
  propConf.initConfig(server_proper)
  var broadcastList:Broadcast[Properties]=null
  var iniRDD:RDD[(String,InputMsg)]=null
  val ISStartFromCheckPoint = spark_Proper.getOrElse("is.start.from.checkpoint","true").toBoolean

  def main(args: Array[String]): Unit = {

    val ssc= if(ISStartFromCheckPoint){
      StreamingContext.getOrCreate(propConf.checkpointDir, createSsc)
    }else createSsc()

    ssc.start()
    ssc.awaitTermination()
  }

  def createSsc(): StreamingContext = {
    parma.setConf(getClass.getClassLoader.getResourceAsStream("kafka.properties"))
    val sparkConf = new SparkConf().setAppName("FakeCarDetect").set("spark.serializer","org.apache.spark.serializer.KryoSerializer").set("spark.local.dir","./log/tmp")//.setMaster("local[2]")
    if (spark_Proper.size > 0) {
      spark_Proper.foreach(p => {
        sparkConf.set(p._1, p._2)
      })
    }
    //创建Spark context
    val sparkContx: StreamingContext = new StreamingContext(sparkConf, Seconds(spark_Proper.getOrElse("batchDuration", "1").toInt))
    //获取历史状态
    val path:Path=propConf.checkpointDir
    iniRDD = org.apache.spark.streaming.rdd.CheckPoint.getStateRDD(sparkContx.sparkContext,path.separatorStr)
    sparkContx.checkpoint(propConf.checkpointDir)

    //创建基于kafka源的Spark stream
    val stream= MessageHandler.createSource(sparkContx,kafka_Proper)
    stream.foreachRDD(rdd=>{
      LOG.info(s"this batch get message size is [${rdd.count()}]")
    })
    MessageHandler.msgHandler(stream)
    sparkContx
  }




}
