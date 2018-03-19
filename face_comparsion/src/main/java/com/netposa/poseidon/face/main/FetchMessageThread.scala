package com.netposa.poseidon.face.main

import java.nio.charset.Charset
import java.util.{Calendar, Date}

import collection.JavaConversions._
import com.netposa.poseidon.face.util.ParseBytesUtils
import kafka.common.TopicAndPartition
import kafka.utils.Logging
import netposa.realtime.kafka.utils.DirectKafkaConsumer
import netposa.realtime.kafka.utils.RowMessage
import org.apache.commons.lang.StringUtils
import org.apache.hadoop.hbase.client.Put
import java.text.SimpleDateFormat

import org.apache.ignite.Ignition
import org.apache.ignite.configuration.CacheConfiguration
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.cache.CacheAtomicityMode
import com.netposa.poseidon.face.main.FaceFeatureDataAnalyzeMain.{IGNITE_NAME, KAFKA_PROP, SERVER_PROP, FACE_DATA_COUNT, FACE_IGNITE_DATA_COUNT}
import com.netposa.poseidon.face.bean.{CacheFaceFeature, FaceFeature}
import com.netposa.poseidon.face.util.{HashAlgorithm, HbaseUtil}

import scala.collection.mutable

/**
  * 接收kafka消息,并执行人脸搜索源数据处理
  */
class FetchMessageThread() extends Thread with Logging {
    val CHARSET: Charset = Charset.forName("UTF-8")
    val OFFSETS: mutable.Map[TopicAndPartition, Long] = mutable.Map[TopicAndPartition, Long]()
    lazy val parseBytesUtils = new ParseBytesUtils
    val cacheCfg = new CacheConfiguration[String, CacheFaceFeature]()
    // 设置数据缓存策略
    cacheCfg.setCacheMode(CacheMode.PARTITIONED)
    // 设置动态缓存,新加入的数据会自动分散在各个server
    cacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)
    //kafka消息接收器
    var CONSUMER: DirectKafkaConsumer = _
    //是否停止程序
    var STOPPED = false
    //读取数据与计算的间隔时间
    val INTERVAL: Long = SERVER_PROP.getProperty("service.fetch.messages.interval.ms", "5000").trim.toLong
    //调试开关
    val DEBUG: Boolean = SERVER_PROP.getProperty("service.process.debug.enable", "false").trim.toBoolean
    //人脸特征配置
    val FACE_FEATURE_SIZE: Int = SERVER_PROP.getProperty("face.feature.size", "512").trim.toInt
    val FACE_VERSION_SIZE: Int = SERVER_PROP.getProperty("face.version.size", "12").trim.toInt
    val FACE_BYTE_SIZE: Int = SERVER_PROP.getProperty("face.tail.size", "0").trim.toInt
    val FACE_FULL_FEATURE_SIZE: Int = FACE_FEATURE_SIZE + FACE_VERSION_SIZE + FACE_BYTE_SIZE
    //kafka单次拉取数据量
    var DATA_COUNT: Int = 0
    //kafka拉取数据总量
    var FACE_DATA_COUNT_SUM: Int = 0
    //保存到Hbase数据总量
    var FACE_VALID_DATA_COUNT: Int = 0
    //人脸数据表名
    val FACE_TABLE_NAME: String = SERVER_PROP.getProperty("face.table.name", "face_feature")

    val SDF = new SimpleDateFormat("yyyyMMdd")

    override def run(): Unit = {
        info("init kafka fetch thread, interval is %d .......".format(INTERVAL))
        if (!HbaseUtil.tableIsExists(FACE_TABLE_NAME)) {
            HbaseUtil.createTable(FACE_TABLE_NAME, Array("cf"), HashAlgorithm.getSplitKeys)
        }
        while (CONSUMER == null && !STOPPED) {
            try {
                info("new kafka direct face consumer instance .........")
                CONSUMER = new DirectKafkaConsumer(KAFKA_PROP, "face")
            } catch {
                case e: Throwable =>
                    warn("connection kafka cluster error is %s".format(e.getMessage), e)
                    info("sleep this thread(kafka consumer) 30s.........")
                    Thread.sleep(30 * 1000)
            }
        }
        while (!STOPPED) {
            try {
                if (DEBUG) {
                    debug("fetch kafka message execute .......")
                }
                val faceMessages = CONSUMER.fetchMessages()
                DATA_COUNT = faceMessages.size
                FACE_DATA_COUNT_SUM = FACE_DATA_COUNT_SUM + faceMessages.size
//                  info("==== this batch has fetched face message size is %d ====".format(FACE_DATA_COUNT_SUM))
                executeBatch(faceMessages, FACE_FULL_FEATURE_SIZE)
            } catch {
                case e: Throwable =>
                    warn("fetch kafka message error %s".format(e.getMessage), e)
            } //end try catch
            if (DATA_COUNT < 500) {
                try {
                    Thread.sleep(INTERVAL)
                } catch {
                    case e: Throwable =>
                        warn("sleep interval error %s".format(e.getMessage), e)
                }
            }
            DATA_COUNT = 0
        }
    }
    private def executeBatch(messages: mutable.Buffer[RowMessage], featureSize: Int) {
        var dataList: List[FaceFeature] = List()
        messages.foreach(row => {
            //数据转换
            val f: FaceFeature = try {
                val f = parseBytesUtils.dataForETL2PB(row.message, featureSize)
                f
            } catch {
                case e: Throwable =>
                    warn("parse input message error => %s".format(e.getMessage), e)
                    null
            }
            if (f != null && !StringUtils.isEmpty(f.getCameraId) &&
              !StringUtils.isEmpty(f.getGatherTime) &&
              !StringUtils.isEmpty(f.getJlbh) &&
              f.getFeature != null &&
              f.getFeature.length >= FACE_FULL_FEATURE_SIZE) {
                val feature = new Array[Byte](FACE_FEATURE_SIZE)
                System.arraycopy(f.getFeature, FACE_VERSION_SIZE, feature, 0, FACE_FEATURE_SIZE)
                f.setFeature(feature)
                dataList = f :: dataList
            }
            //加入读取后的offset
            OFFSETS.synchronized({
                if (OFFSETS.keySet.exists(p =>
                    p.topic.equals(row.tp.topic) && p.partition == row.tp.partition)) {
                    val last_offset = OFFSETS.get(row.tp).get
                    if (last_offset <= row.offset) {
                        OFFSETS += row.tp -> row.offset
                    }
                } else {
                    OFFSETS += row.tp -> row.offset
                } //end set offsets
            })
        })

//        FaceFeatureDataAnalyzeMain.FACE_DATA_COUNT += dataList.size
        FACE_VALID_DATA_COUNT = FACE_VALID_DATA_COUNT + dataList.size
        //保存数据
        val flag = writDateFile(dataList)
        //提交offset
        if (flag._1) {
            FACE_IGNITE_DATA_COUNT = FACE_IGNITE_DATA_COUNT + flag._2
            info("streaming data statistics [service_name:%s,total_input:%s,current_input:%s,total_process:%s,current_process:%s,total_output:%s,current_output:%s]"
              .format("face_comparison", FACE_DATA_COUNT_SUM+FaceFeatureDataAnalyzeMain.FACE_DATA_COUNT, messages.size, FACE_DATA_COUNT_SUM+FaceFeatureDataAnalyzeMain.FACE_DATA_COUNT, messages.size, FACE_VALID_DATA_COUNT+FaceFeatureDataAnalyzeMain.FACE_DATA_COUNT, dataList.size))
            OFFSETS.synchronized({
                if (OFFSETS.nonEmpty) {
                    if (DEBUG) {
                        debug("commit kafka messages offset begin .......")
                        debug(OFFSETS.toString())
                    }
                    CONSUMER.commitOffsets(OFFSETS.toMap)
                    OFFSETS.clear()
                }
            })
        } else {
            error("when an exception occurs in saving face feature to hbase! ")
        }
    }
    def shutdown(): Unit = {
        STOPPED = true
        CONSUMER match {
            case null =>
            case _ =>
                try {
                    CONSUMER.shutdown()
                } catch {
                    case e: Throwable =>
                        warn("close thread error %s".format(e.getMessage), e)
                }
        }
    }
    def writDateFile(dataList: List[FaceFeature]): (Boolean, Int) = {
        var save2IgniteCount = 0
        if (dataList.isEmpty) {
            debug("faceFeature list is null!")
            (true, 0)
        } else {
            val dataPuts = dataList.map(faceFeature => {
                val gatherTime = faceFeature.getGatherTime
                val rowKey = gatherTime + faceFeature.getJlbh
                val put = new Put((HashAlgorithm.hash(rowKey)).getBytes())
                put.addColumn("cf".getBytes, "logNum".getBytes, faceFeature.getJlbh.getBytes)
                put.addColumn("cf".getBytes, "feature".getBytes, faceFeature.getFeature)
                put.addColumn("cf".getBytes, "gatherTime".getBytes, gatherTime.toString.getBytes)
                put.addColumn("cf".getBytes, "cameraId".getBytes, faceFeature.getCameraId.getBytes)
            })
            val calendar: Calendar = Calendar.getInstance()
            calendar.setTimeInMillis(new Date().getTime)
            calendar.add(Calendar.DATE, (-1) * SERVER_PROP.getProperty("duration.time").toInt)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            dataList.foreach { faceFeature =>
                val date = SDF.format(new Date(faceFeature.getGatherTime.toLong))
                if(Ignition.ignite(IGNITE_NAME).cacheNames().contains(IGNITE_NAME + "_FACE_" + date) || faceFeature.getGatherTime.toLong >= calendar.getTimeInMillis) {
                    if (faceFeature != null && faceFeature.getJlbh != null && faceFeature.getGatherTime != null && faceFeature.getCameraId != null && faceFeature.getFeature != null) {
                        cacheCfg.setName(IGNITE_NAME + "_FACE_" + date)
                        cacheCfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
                        cacheCfg.setMemoryPolicyName("MeM_RANDOM_2_LRU");
                        val igniteCache = Ignition.ignite(IGNITE_NAME).getOrCreateCache[String, CacheFaceFeature](cacheCfg)
                        igniteCache.put(faceFeature.getJlbh, new CacheFaceFeature(faceFeature.getGatherTime.getBytes, faceFeature.getCameraId.getBytes, faceFeature.getFeature))
                        save2IgniteCount = save2IgniteCount + 1
                    }
                }
            }
            HbaseUtil.save(dataPuts, FACE_TABLE_NAME)
            (true, save2IgniteCount)
        }
    }
}
