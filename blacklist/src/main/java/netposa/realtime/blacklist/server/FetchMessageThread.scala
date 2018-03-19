package netposa.realtime.blacklist.server

import kafka.utils.Logging
import java.util.{Calendar, Date, Properties}

import netposa.realtime.kafka.utils.DirectKafkaConsumer

import scala.collection.mutable.Buffer
import netposa.realtime.kafka.utils.RowMessage
import netposa.realtime.blacklist.bean.BlacklistBean

import scala.collection.mutable.ListBuffer
import netposa.realtime.kafka.utils.ByteUtils
import java.nio.charset.Charset

import netposa.message.gcjl.Gcjl
import netposa.realtime.blacklist.bean.CheckpointConfBean
import java.sql.Connection
import java.sql.Statement
import java.text.SimpleDateFormat

import kafka.common.TopicAndPartition

import scala.collection.mutable

/**
  * 用于启动与KAFKA通信的监听线程,并定时接收数据进行处理
  */
class FetchMessageThread(val props: Properties,
                         val ck_conf: CheckpointConfBean,
                         val control_utils: LoadControlUtils) extends Thread with Logging {
    var totalInput: Int = 0
    var currentInput: Int = 0
    var totalProcess: Int = 0
    var currentProcess: Int = 0
    var totalOutput: Int = 0
    var currentOutput: Int = 0

    val charset = Charset.forName("UTF-8")
    val sdf: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    import scala.collection.mutable.Map

    val checkpoints = Map[Checkpoint, (Array[Byte], Double, Double)]()
    var load_checkpoints_time: Long = 0
    val reflush_checkpoints_ms: Long =
        props.getProperty("checkpoint.metadata.reflush.hour", "24").toInt * 60 * 60 * 1000

    val blacklistMap = Map[PlateNumber, Map[Long, BlacklistBean]]()
    val blacklistMap_fuzzy = Map[PlateNumber, Map[Long, BlacklistBean]]()
    var load_blacklist_time: Long = 0
    val reflush_blaclist_ms: Long =
        props.getProperty("blacklist.metadata.reflush.minute", "20").toInt * 60 * 1000

    var consumer: DirectKafkaConsumer = null
    var stoped = false
    val interval = props.getProperty("blacklist.fetch.messages.interval.ms", "3000").toLong

    val interface = props.getProperty("blacklist.result.send.interface", "http").toLowerCase()

    val debug = props.getProperty("blacklist.process.debug.enable", "false").toBoolean

    override def run(): Unit = {
        info("init kafka fetch thread, interval is %d .......".format(interval))

        while (consumer == null && !stoped) {
            try {
                info("new kafka direct consumer instance .........")
                consumer = new DirectKafkaConsumer(props)
            } catch {
                case e: Throwable => {
                    warn("connection kafka cluster error is %s".format(e.getMessage()), e)
                    info("sleep this thread(kafka consumer) 30s.........")
                    Thread.sleep(30 * 1000)
                } //end case exception
            } //end try catch
        } //end while consumer init

        var connection: Connection = null
        var statement: Statement = null

        info("init load checkpoint metadata ........")
        val ck_utils = new LoadCheckpointUtils(ck_conf, debug)
        while (!stoped && connection == null) {
            try {
                info("new checkpoint db connection instance ......")
                connection = ck_utils.initConnection
                statement = connection.createStatement()
                val tmp = ck_utils.fetchMetadata(statement)
                load_checkpoints_time = System.currentTimeMillis()
                checkpoints.synchronized({
                    checkpoints.clear
                    tmp.foreach(row => checkpoints += row._1 -> row._2)
                })
            } catch {
                case e: Throwable => {
                    warn("new checkpoint db connection instance error => %s".format(e.getMessage()), e)
                    info("sleep this thread(checkpoint db connection) 30s.........")
                    ck_utils.closeStatement(statement)
                    ck_utils.closeConnection(connection)
                    statement = null
                    connection = null
                    Thread.sleep(30 * 1000)
                }
            }
        } //end while checkpoint db connection
        ck_utils.closeStatement(statement)
        ck_utils.closeConnection(connection)
        statement = null
        connection = null

        info("init load control metadata ........")
        while (!stoped && connection == null) {
            try {
                info("new control db connection instance ......")
                connection = control_utils.initConnection
                statement = connection.createStatement()
                val tmp = control_utils.fetchAllMetadata(statement)
                load_blacklist_time = System.currentTimeMillis()
                blacklistMap.synchronized({
                    blacklistMap.clear
                    blacklistMap_fuzzy.clear
                    tmp.foreach(f => {
                        if (f._1.getPlateNumber().contains("*") || f._1.getPlateNumber().contains("?")) {
                            blacklistMap_fuzzy += f._1 -> f._2
                        } else {
                            blacklistMap += f._1 -> f._2
                        }
                    })
                })
            } catch {
                case e: Throwable => {
                    warn("new control db connection instance error => %s".format(e.getMessage()), e)
                    info("sleep this thread(control db connection) 30s.........")
                    control_utils.closeStatement(statement)
                    control_utils.closeConnection(connection)
                    statement = null
                    connection = null
                    Thread.sleep(30 * 1000)
                }
            }
        } //end while control db connection
        control_utils.closeStatement(statement)
        control_utils.closeConnection(connection)
        statement = null
        connection = null

        info("init http client utils .......")
        val httpclient: SendHttpMessageUtils = interface match {
            case "http" => new SendHttpMessageUtils(props, consumer)
            case _ => null
        }

        info("start consumer fetch message listener ......")

        while (!stoped) {
            try {
                if (debug) {
                    info("fetch kafka message execute .......")
                }
                val messages = consumer.fetchMessages
                if (debug) {
                    info("fetch kafka message size is %d".format(messages.size))
                }
                totalInput += messages.size
                currentInput += messages.size
                executeBatch(messages, httpclient)
            } catch {
                case e: Throwable => {
                    warn("fetch kafka message error %s".format(e.getMessage()), e)
                }
            } //end try catch

            info("streaming data statistics[service_name:%s,total_input:%d,current_input:%d,total_process:%d,current_process:%d,total_output:%d,current_output:%d]"
              .format("blacklist_realtime", totalInput, currentInput, totalProcess, currentProcess, totalOutput, currentOutput))
            currentInput = 0
            currentProcess = 0
            currentOutput = 0

            //check and reflush checkpoints
            if (System.currentTimeMillis() > (load_checkpoints_time + reflush_checkpoints_ms)) {
                val thread = new Thread() {
                    override def run() = {
                        try {
                            val db_conn = ck_utils.initConnection
                            val db_state = db_conn.createStatement()
                            try {
                                val tmp = ck_utils.fetchMetadata(db_state)
                                load_checkpoints_time = System.currentTimeMillis()
                                checkpoints.synchronized({
                                    checkpoints.clear
                                    tmp.foreach(row => checkpoints += row._1 -> row._2)
                                })
                            } catch {
                                case e: Throwable => warn("reflush checkpoint db metadata error %s".format(e.getMessage()), e)
                            } finally {
                                ck_utils.closeStatement(db_state)
                                ck_utils.closeConnection(db_conn)
                            }
                        } catch {
                            case e: Throwable => warn("reflush checkpoint db metadata error %s".format(e.getMessage()), e)
                        }
                    }
                }
                thread.setDaemon(true)
                thread.start()
            }
            //check and reflush controls
            if (System.currentTimeMillis() > (load_blacklist_time + reflush_blaclist_ms)) {
                val thread = new Thread() {
                    override def run() = {
                        try {
                            val db_conn = control_utils.initConnection
                            val db_state = db_conn.createStatement()
                            try {
                                val tmp = control_utils.fetchAllMetadata(db_state)
                                load_blacklist_time = System.currentTimeMillis()
                                blacklistMap.synchronized({
                                    blacklistMap.clear
                                    blacklistMap_fuzzy.clear
                                    tmp.foreach(f => {
                                        if (f._1.getPlateNumber().contains("*") || f._1.getPlateNumber().contains("?")) {
                                            blacklistMap_fuzzy += f._1 -> f._2
                                        } else {
                                            blacklistMap += f._1 -> f._2
                                        }
                                    })
                                })
                            } catch {
                                case e: Throwable => warn("reflush controls db metadata error %s".format(e.getMessage()), e)
                            } finally {
                                control_utils.closeStatement(db_state)
                                control_utils.closeConnection(db_conn)
                            }
                        } catch {
                            case e: Throwable => warn("reflush controls db metadata error %s".format(e.getMessage()), e)
                        }
                    }
                }
                thread.setDaemon(true)
                thread.start()
            }

            Thread.sleep(interval)
        } //end while interval

        if (httpclient != null) {
            httpclient.shutdown
        }
        shutdown
    } //end run method

    def shutdown(): Unit = {
        stoped = true
        consumer match {
            case null =>
            case _ => {
                try {
                    consumer.shutdown
                } catch {
                    case e: Throwable => {
                        warn("close thread error %s".format(e.getMessage()), e)
                    }
                } //end try catch
            } //end case
        } //end match consumer
    } //end shutdown method

    /**
      * 执行布控的比对
      */
    private def executeBatch(messages: Buffer[RowMessage], httpclient: SendHttpMessageUtils) {
        messages.foreach(row => {
            //数据转换
            val gcjl: Gcjl.GcjlMsg = try {
                Gcjl.GcjlMsg.parseFrom(row.message)
            } catch {
                case e: Throwable => {
                    warn("parse input message error => %s".format(e.getMessage()), e)
                    null
                }
            }

            //比对数据
            if (null != gcjl) {
                try {
                    compactGcjl(gcjl, httpclient, ck_conf)
                } catch {
                    case e: Throwable => {
                        warn("can not compact gcjl by traffic is [%s], error is %s"
                                .format(gcjl.getJlbh().toStringUtf8(), e.getMessage()), e)
                    }
                }
            }

            //加入读取后的offset
            httpclient.offsets.synchronized({
                if (httpclient.offsets.keySet.exists(p => p.topic.equals(row.tp.topic)
                        && p.partition == row.tp.partition)) {
                    val last_offset = httpclient.offsets.get(row.tp).get
                    if (last_offset <= row.offset) {
                        httpclient.offsets += row.tp -> row.offset
                    }
                } else {
                    httpclient.offsets += row.tp -> row.offset
                } //end set offsets
            })
        }) //end foreach messages
    }

    private def compactGcjl(gcjl: Gcjl.GcjlMsg, httpclient: SendHttpMessageUtils, ck_conf: CheckpointConfBean): Boolean = {
        //此处从解析出来的结构体中得到车牌
        val platenumber: Array[Byte] = gcjl.getHphm().toByteArray()
        val platecolor: Array[Byte] = gcjl.getHpys().toByteArray()
        val vehiclebrand: Array[Byte] = gcjl.getClpp().toByteArray()
        val vehicletype: Array[Byte] = gcjl.getCllx().toByteArray()
        val vehiclecolor: Array[Byte] = gcjl.getCsys().toByteArray()
        val orgid: Array[Byte] = gcjl.getXzqh.toByteArray()
        val checkpoint: Array[Byte] = gcjl.getKkbh().toByteArray()
        val checkpoint_obj = new Checkpoint(checkpoint)
        //TODO 过车时间
        val traffic_time: Long = try {
            ByteUtils.parseBytesToLongtime(gcjl.getJgsj().toByteArray())
        } catch {
            case e: Throwable => {
                warn("parse traffic time bytes to long time error => %s".format(e.getMessage()), e)
                0
            }
        }
        val traffic_id: Array[Byte] = gcjl.getJlbh().toByteArray()
        //TODO 得到卡口对应的区域
        //val ck_def = checkpoints.getOrElse(checkpoint_obj, null)
        //val area: Array[Byte] = if (ck_def != null) ck_def._1 else null
        //if (null == area) {
        //  warn("input traffic record checkpoint can not be set orgid value, checkpoint is %s"
        //    .format(checkpoint_obj.toString))
        //}

        val platenumber_str: String = if (debug) new String(platenumber, charset) else null
        if (debug) info("execute compact blacklist control by platenumber [%s]".format(platenumber_str))

        //TODO 得到车牌对应的所有布控信息
        val key = new PlateNumber(platenumber)
        var blacklists = blacklistMap.getOrElse(key, null)

        //val blacklists = Map[Long, BlacklistBean]()
        blacklistMap_fuzzy.foreach(f => {
            var plateNumber = f._1
            var blackListTemp = f._2.foreach(p => {
                var isMatch = FliterBlackList.compare(key.getPlateNumber(), new String(p._2.platenumber));
                if (isMatch) {
                    if (debug) info("compare blacklist control by platenumber [%s], regex is [%s]"
                            .format(key.getPlateNumber(), new String(p._2.platenumber)))
                    if (null == blacklists) blacklists = Map[Long, BlacklistBean]()
                    blacklists.put(p._2.id, p._2);
                }
            })
        })

        if (null == blacklists || blacklists.size < 1) {
            if (debug) {
                info("execute compact blacklist control by platenumber [%s], can not be find blacklist controls".format(platenumber_str))
            }
            return false
        }

        if (debug) {
            info("execute compact blacklist control by platenumber [%s], find blacklist size is %d".format(platenumber_str, blacklists.size))
        }
        totalProcess += 1
        currentProcess += 1
        //TODO 执行比对,还需要对要比对的属性进行设置,目前全是使用的null来替代
        blacklists.foreach(b => {
            b._2.enable_alarm match {
                case true => {
                    var flag = true

                    /**比对过车时间是否在指定的时间范围内*/
                    flag = b._2.begintime <= traffic_time && traffic_time <= b._2.endtime
                    if (debug) {
                        info("compact platenumber [%s] attr by traffic time flag is %s, traffic time is %s, blacklist is %s and %s"
                                .format(platenumber_str, flag.toString, sdf.format(new Date(traffic_time)), sdf.format(new Date(b._2.begintime)), sdf.format(new Date(b._2.endtime))))
                    }

                    if (flag && !b._2.disable_platecolor) {
                        /** 比对车牌颜色是否相同 */
                        flag = ByteUtils.equals(platecolor, b._2.platecolor)
                        if (debug) {
                            info("compact platenumber [%s] attr by platecolor flag is %s, disable is %s, traffic platecolor is %s, blacklist is %s"
                                    .format(platenumber_str, flag.toString, b._2.disable_platecolor.toString,
                                        new String(if (platecolor == null) {
                                            Array[Byte]()
                                        } else {
                                            platecolor
                                        }),
                                        new String(if (b._2.platecolor == null) {
                                            Array[Byte]()
                                        } else {
                                            b._2.platecolor
                                        })))
                        }
                    } else {
                        false
                    }

                    if (flag && !b._2.disable_vehiclebrand) {
                        /** 比对车辆品牌是否相同 */
                        flag = ByteUtils.equals(vehiclebrand, b._2.vehiclebrand)
                        if (debug) {
                            info("compact platenumber [%s] attr by vehiclebrand flag is %s, disable is %s, traffic vehiclebrand is %s, blacklist is %s"
                                    .format(platenumber_str, flag.toString, b._2.disable_vehiclebrand.toString,
                                        new String(if (vehiclebrand == null) {
                                            Array[Byte]()
                                        } else {
                                            vehiclebrand
                                        }),
                                        new String(if (b._2.vehiclebrand == null) {
                                            Array[Byte]()
                                        } else {
                                            b._2.vehiclebrand
                                        })))
                        }
                    } else {
                        false
                    }

                    if (flag && !b._2.disable_vehicletype) {
                        /** 比对车辆类型是否相同 */
                        flag = ByteUtils.equals(vehicletype, b._2.vehicletype)
                        if (debug) {
                            info("compact platenumber [%s] attr by vehicletype flag is %s, disable is %s, traffic vehicletype is %s, blacklist is %s"
                                    .format(platenumber_str, flag.toString, b._2.disable_vehicletype.toString,
                                        new String(if (vehicletype == null) {
                                            Array[Byte]()
                                        } else {
                                            vehicletype
                                        }),
                                        new String(if (b._2.vehicletype == null) {
                                            Array[Byte]()
                                        } else {
                                            b._2.vehicletype
                                        })))
                        }
                    } else {
                        false
                    }

                    if (flag && !b._2.disable_vehiclecolor) {
                        /** 比对车辆颜色是否相同 */
                        flag = ByteUtils.equals(vehiclecolor, b._2.vehiclecolor)
                        if (debug) {
                            info("compact platenumber [%s] attr by vehiclecolor flag is %s, disable is %s, traffic vehiclecolor is %s, blacklist is %s"
                                    .format(platenumber_str, flag.toString, b._2.disable_vehiclecolor.toString,
                                        new String(if (vehiclecolor == null) Array[Byte]() else vehiclecolor),
                                        new String(if (b._2.vehiclecolor == null) Array[Byte]() else b._2.vehiclecolor)))
                        }
                    } else {
                        false
                    }

                    if (flag && !b._2.disable_orgid) {
                        /** 比对卡口区域是否包含在布控的区域 */
                        flag = ByteUtils.exists(b._2.orgids, orgid)
                        if (!flag) {
                            if (!b._2.disable_monitorid) {
                                /** 比对卡口是否包含在布控的卡口 */
                                flag = ByteUtils.exists(b._2.monitorids, checkpoint)
                            } else {
                                false
                            }
                        }
                        if (debug) {
                            info("compact platenumber [%s] attr by orgid flag is %s, disable is %s, traffic checkpoint is %s, blacklist is %s"
                                    .format(platenumber_str, flag.toString, b._2.disable_orgid.toString, checkpoint_obj.toString,
                                        if (b._2.orgid == null) "" else b._2.orgid))
                        }
                    } else {
                        false
                    }

                    if (flag) {
                        totalOutput += 1
                        currentInput += 1

                        //TODO 匹配成功,需要对此条数据进行报警
                        val traffic_id_str = new String(traffic_id)
                        info("add platenumber [%s] to blacklist queue, traffic id is [%s], blacklist id is [%d]".format(new String(platenumber, charset), traffic_id_str, b._1))

                        if (null == checkpoints || checkpoints.size < 1) {
                            info("检测到卡口数据为空，现在重新加载！")
                            var connection: Connection = null
                            var statement: Statement = null
                            val ck_utils = new LoadCheckpointUtils(ck_conf, debug)
                            try {
                                info("new checkpoint db connection instance ......")
                                connection = ck_utils.initConnection
                                statement = connection.createStatement()
                                val tmp = ck_utils.fetchMetadata(statement)
                                load_checkpoints_time = System.currentTimeMillis()
                                checkpoints.synchronized({
                                    checkpoints.clear
                                    tmp.foreach(row => checkpoints += row._1 -> row._2)
                                })
                            } catch {
                                case e: Throwable => {
                                    warn("new checkpoint db connection instance error => %s".format(e.getMessage()), e)
                                    info("sleep this thread(checkpoint db connection) 30s.........")
                                    ck_utils.closeStatement(statement)
                                    ck_utils.closeConnection(connection)
                                    statement = null
                                    connection = null
                                    Thread.sleep(30 * 1000)
                                }
                            }
                        }

                         if (null != checkpoint && !"".equals(checkpoint.toString) &&  null != checkpoints.get(checkpoint_obj)) {
                             //new alarm record
                             val record = new AlarmRecord(
                                 b._1, traffic_id_str, platenumber, System.currentTimeMillis(),
                                 b._2.controllevel, b._2.alarmtype, b._2.alarmstatus,
                                 platecolor, checkpoint, checkpoints.get(checkpoint_obj).get._2,
                                 checkpoints.get(checkpoint_obj).get._3, traffic_time, vehiclecolor,
                                 vehiclebrand, vehicletype, gcjl.getClsd(), gcjl.getCdbh().toByteArray(),
                                 gcjl.getHpzl().toByteArray(), gcjl.getQjtp().toByteArray(), gcjl.getXzqh.toStringUtf8(), b._2.controluser)
                             //send to client utils
                             httpclient match {
                                 case null =>
                                 case _ => httpclient.addAlarm(record)
                             }
                         }
                    } //end send flag
                }
                case _ => {
                    if (debug) info("this blacklist id:[%d], traffic_id is [%s] not enable control,skip this compact".format(b._1, new String(traffic_id)))
                }
            } //end enable_alarm match
        }) //end foreach blacklists
        true
    }

}
