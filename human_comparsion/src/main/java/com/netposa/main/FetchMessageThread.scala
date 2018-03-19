package com.netposa.main

import java.nio.charset.Charset
import java.util.Properties

import collection.JavaConversions._
import scala.collection.mutable.Buffer
import com.netposa.util.ParseBytesUtils
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
import java.util.Date

import com.netposa.main.HumanFeatureDataAnalyzeMain.humanDataCount
import com.netposa.bean.{CacheFaceFeature, FaceFeature}
import com.netposa.init.LoadPropers
import com.netposa.util.{HashAlgorithm, HbaseUtil}

import scala.collection.mutable

/**
  * 接收kafka消息,并执行人脸搜索源数据处理
  */
class FetchMessageThread(val props: Properties) extends Thread with Logging {

    val IGNITE_NAME: String = LoadPropers.getProperties().getProperty("ignite.name")

    val charset: Charset = Charset.forName("UTF-8")
    val offsets: mutable.Map[TopicAndPartition, Long] = mutable.Map[TopicAndPartition, Long]()
    lazy val parseBytesUtils = new ParseBytesUtils
    val cacheCfg = new CacheConfiguration[String, CacheFaceFeature]()
    // 设置数据缓存策略
    cacheCfg.setCacheMode(CacheMode.PARTITIONED)
    // 设置动态缓存,新加入的数据会自动分散在各个server
    cacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)

    //kafka消息接收器
    var humanConsumer: DirectKafkaConsumer = _
    //是否停止程序
    var stoped = false
    //读取数据与计算的间隔时间
    val interval: Long = props.getProperty("service.fetch.messages.interval.ms", "5000").trim.toLong
    //调试开关
    val debug: Boolean = props.getProperty("service.process.debug.enable", "false").trim.toBoolean
    //人体特征配置
    val HUMAN_FEATURE_SIZE: Int = props.getProperty("human.feature.size", "256").trim.toInt
    val HUMAN_VERSION_SIZE: Int = props.getProperty("human.version.size", "32").trim.toInt
    val HUMAN_BYTE_SIZE: Int = props.getProperty("human.tail.size", "0").trim.toInt
    val HUMAN_FULL_FEATURE_SIZE: Int = HUMAN_FEATURE_SIZE + HUMAN_VERSION_SIZE + HUMAN_BYTE_SIZE

    var data_count: Int = 0
    var human_data_count_sum = 0
    var human_valid_data_count = 0
    //人体数据表名
    val humanTableName: String = props.getProperty("human.table.name", "human_feature")
    val sdf = new SimpleDateFormat("yyyyMMdd")

    override def run(): Unit = {
        info("init kafka fetch thread, interval is %d .......".format(interval))
        if (!HbaseUtil.tableIsExists(humanTableName)) {
            HbaseUtil.createTable(humanTableName, Array("cf"), HashAlgorithm.getSplitKeys)
        }
        while (humanConsumer == null && !stoped) {
            try {
                info("new kafka direct face consumer instance .........")
                humanConsumer = new DirectKafkaConsumer(props, "human")
            } catch {
                case e: Throwable =>
                    warn("connection kafka cluster error is %s".format(e.getMessage), e)
                    info("sleep this thread(kafka consumer) 30s.........")
                    Thread.sleep(30 * 1000)
            }
        }
        while (!stoped) {
            try {
                if (debug) {
                    debug("fetch kafka message execute .......")
                }
                val humanMessages = humanConsumer.fetchMessages()
                human_data_count_sum = human_data_count_sum + humanMessages.size
                if (debug) {
                    info("current fetch human message size is %d,altogether fetch human message size is %d".format(humanMessages.size, human_data_count_sum))
                }
                data_count = data_count + humanMessages.size
                executeBatch(humanMessages, HUMAN_FULL_FEATURE_SIZE)
            } catch {
                case e: Throwable =>
                    warn("fetch kafka message error %s".format(e.getMessage), e)
            } //end try catch
            if (data_count < 500) {
                try {
                    Thread.sleep(interval)
                } catch {
                    case e: Throwable =>
                        warn("sleep interval error %s".format(e.getMessage), e)
                }
            }
            data_count = 0
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
              f.getFeature.length >= HUMAN_FULL_FEATURE_SIZE) {
                val feature = new Array[Byte](HUMAN_FEATURE_SIZE)
                System.arraycopy(f.getFeature, HUMAN_VERSION_SIZE, feature, 0, HUMAN_FEATURE_SIZE)
                f.setFeature(feature)
                dataList = f :: dataList
            }
            //加入读取后的offset
            offsets.synchronized({
                if (offsets.keySet.exists(p =>
                    p.topic.equals(row.tp.topic) && p.partition == row.tp.partition)) {
                    val last_offset = offsets.get(row.tp).get
                    if (last_offset <= row.offset) {
                        offsets += row.tp -> row.offset
                    }
                } else {
                    offsets += row.tp -> row.offset
                } //end set offsets
            })
        })


        //保存数据
        val flag = writDateFile(dataList)
        //提交offset
        if (flag) {
            human_valid_data_count = human_valid_data_count + dataList.size
            if (debug) {
                info("current save to hbase table %s and cache in ignite features size is : %s,altogether save to hbase table %s and cache in ignite features size is : %s  "
                  .format(humanTableName, dataList.size.toString, humanTableName, human_valid_data_count))
                info("This startup altogether had %d human data in system,history data %d"
                  .format(humanDataCount + human_valid_data_count, humanDataCount))
            }


            offsets.synchronized({
                if (offsets.nonEmpty) {
                    if (debug) {
                        debug("commit kafka messages offset begin .......")
                        debug(offsets.toString())
                    }
                    humanConsumer.commitOffsets(offsets.toMap)
                    offsets.clear()
                }
            })
        } else {
            error("when an exception occurs in saving face feature to hbase! ")
        }
    }

    def shutdown(): Unit = {
        stoped = true
        humanConsumer match {
            case null =>
            case _ =>
                try {
                    humanConsumer.shutdown()
                } catch {
                    case e: Throwable =>
                        warn("close thread error %s".format(e.getMessage), e)
                }
        }
    }

    def writDateFile(dataList: List[FaceFeature]): Boolean = {
        if (dataList.isEmpty) {
            debug("human feature list is null!")
            true
        } else {
            val dataPuts = dataList.map(faceFeature => {
                val gatherTime = faceFeature.getGatherTime
                val rowKey = gatherTime + faceFeature.getJlbh
                val put = new Put((HashAlgorithm.hash(rowKey) + rowKey).getBytes())
                put.addColumn("cf".getBytes, "logNum".getBytes, faceFeature.getJlbh.getBytes)
                put.addColumn("cf".getBytes, "feature".getBytes, faceFeature.getFeature)
                put.addColumn("cf".getBytes, "gatherTime".getBytes, gatherTime.toString.getBytes)
                put.addColumn("cf".getBytes, "cameraId".getBytes, faceFeature.getCameraId.getBytes)
            })
            dataList.foreach { faceFeature =>
                if (faceFeature != null && faceFeature.getJlbh != null && faceFeature.getGatherTime != null && faceFeature.getCameraId != null && faceFeature.getFeature != null) {
                    val date = sdf.format(new Date(faceFeature.getGatherTime.toLong))
                    cacheCfg.setName(IGNITE_NAME + "_HUMAN_" + date)
                    val igniteCache = Ignition.ignite(IGNITE_NAME).getOrCreateCache[String, CacheFaceFeature](cacheCfg)
                    igniteCache.put(faceFeature.getJlbh, new CacheFaceFeature(faceFeature.getGatherTime.getBytes, faceFeature.getCameraId.getBytes, faceFeature.getFeature))
                }
            }
            HbaseUtil.save(dataPuts, humanTableName)
        }
    }
}
