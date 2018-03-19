package com.nukil.volume.main

import java.text.SimpleDateFormat
import java.util.{Properties, UUID}

import com.nukil.volume.bean.HumanBean
import com.nukil.volume.service.WriteFile2Disk
import dg.model.Common.{DataFmtType, ObjType}
import dg.model.{Common, Deepdata}
import dg.model.Deepdata.GenericObj
import kafka.common.TopicAndPartition
import kafka.utils.Logging
import netposa.realtime.kafka.utils.{DirectKafkaConsumer, RowMessage}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class FetchMessageThread(props: Properties) extends Thread with Logging{
    val SDF: SimpleDateFormat = new SimpleDateFormat("yyyyMMdd")
    val SDF1: SimpleDateFormat = new SimpleDateFormat("yyyyMMdd H")
    val LOG: Logger = LoggerFactory.getLogger(this.getClass)
    val THREAD_SLEEP_INTERVAL: Int = props.getProperty("service.fetch.messages.interval.ms", "5000").trim.toInt
    val DEBUG: Boolean = props.getProperty("service.process.debug.enable", "false").trim.toBoolean
    var LAST_SAVE_TIME: Long = System.currentTimeMillis()
    var NEXT_SAVE_TIME: Long = (LAST_SAVE_TIME +  60 * 60 * 1000) / 3600000 * 3600000
    val OFFSETS: mutable.Map[TopicAndPartition, Long] = mutable.Map[TopicAndPartition, Long]()
    var COUNTER_MAP: mutable.Map[(String, String, String, String), Int] = mutable.Map[(String, String, String, String), Int]()
    var CONSUMER: DirectKafkaConsumer = _
    var STOPPED: Boolean = false
    val MYSQL_TABLE_NAME: String = props.getProperty("mysql.tablename").trim
    val write2File: WriteFile2Disk = WriteFile2Disk.getInstance()

    override def run(): Unit = {
        info("init kafka fetch thread, interval is %d .......".format(THREAD_SLEEP_INTERVAL))
        while (CONSUMER == null && !STOPPED) {
            try {
                info("new kafka direct consumer instance .........")
                CONSUMER = new DirectKafkaConsumer(props)
            } catch {
                case e: Throwable =>
                    warn("connection kafka cluster error is %s".format(e.getMessage), e)
                    info("sleep this thread(kafka consumer) 30s.........")
                    Thread.sleep(30 * 1000)
            }
        }
        while (!STOPPED) {
            try {
                val messages: mutable.Buffer[RowMessage] = CONSUMER.fetchMessages()
                executeBatch(messages)
                sendTask2WriteDiskThread()
                if (messages.size < 500) {
                    try {
                        Thread.sleep(THREAD_SLEEP_INTERVAL)
                    } catch {
                        case e: Exception =>
                            LOG.error(e.getMessage, e)
                    }
                }
            } catch {
                case e: Throwable =>
                    warn("fetch kafka message error %s".format(e.getMessage), e)
                    while (null == CONSUMER && !STOPPED) {
                        try {
                            info("new kafka direct consumer instance .........")
                            CONSUMER = new DirectKafkaConsumer(props)
                        } catch {
                            case e: Throwable =>
                                warn("connection kafka cluster error is %s".format(e.getMessage), e)
                                info("sleep this thread(kafka consumer) 30s.........")
                                Thread.sleep(30 * 1000)
                        } finally {
                            sendTask2WriteDiskThread()
                        }
                    }
            }
        }
    }

    private def sendTask2WriteDiskThread(): Unit = {
        try {
            if (COUNTER_MAP.nonEmpty) {
                COUNTER_MAP.foreach(map => {
                    LOG.info("send task to write file to disk thread, task is %s".format(map.toString()))
                    write2File.addTask(new HumanBean(map._1._1, map._1._2, map._1._3, map._1._4, map._2, MYSQL_TABLE_NAME, UUID.randomUUID().toString.replaceAll("-", "")))
                })
                COUNTER_MAP.clear()
            }
        } catch {
            case e: Exception =>
                LOG.error(e.getMessage, e)
        }
    }

    private def executeBatch(messages: mutable.Buffer[RowMessage]): Unit = {
        messages.foreach(message => {
            val gobj: GenericObj = Deepdata.GenericObj.parseFrom(message.message)
            val ftype: DataFmtType = gobj.getFmtType
            val objType = gobj.getObjType
            if (ftype == DataFmtType.PROTOBUF) {
                if (objType == ObjType.OBJ_TYPE_PEDESTRIAN) {
                    val pobj = Common.PedestrianObj.parseFrom(gobj.getBinData)
                    val split = SDF1.format(pobj.getMetadata.getTimestamp).split(" ")
                    val key = (split(0), split(1), pobj.getMetadata.getRepoId.toString, pobj.getMetadata.getSensorId.toString)
                    if (COUNTER_MAP.contains(key)) {
                        COUNTER_MAP += key -> (COUNTER_MAP(key) + 1)
                    } else {
                        COUNTER_MAP += key -> 1
                    }
                }
            }
            //加入读取后的offset
            OFFSETS.synchronized({
                if (OFFSETS.keySet.exists(p =>
                    p.topic.equals(message.tp.topic) && p.partition == message.tp.partition)) {
                    val last_offset = OFFSETS.get(message.tp).get
                    if (last_offset <= message.offset) {
                        OFFSETS += message.tp -> message.offset
                    }
                } else {
                    OFFSETS += message.tp -> message.offset
                } //end set offsets
            })
        })
        //加入读取后的offset并提交
        OFFSETS.synchronized({
            if (OFFSETS.nonEmpty) {
                if (DEBUG) {
                    debug("commit kafka messages offset begin .......")
                }
                CONSUMER.commitOffsets(OFFSETS.toMap)
                OFFSETS.clear()
            }
        })
    }

    def shutDown(): Unit = {
        STOPPED = true
        CONSUMER match {
            case null =>
            case _ =>
                try {
                    CONSUMER.shutdown()
                } catch {
                    case e: Exception =>
                        warn("close thread failed %s".format(e.getMessage), e)
                }
        }
    }
}
