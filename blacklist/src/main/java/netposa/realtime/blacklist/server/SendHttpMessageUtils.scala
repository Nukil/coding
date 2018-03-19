package netposa.realtime.blacklist.server

import java.io._
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.mutable.ListBuffer
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.HTTP
import kafka.common.TopicAndPartition
import kafka.utils.Json
import kafka.utils.Logging
import netposa.realtime.kafka.utils.DirectKafkaConsumer
import netposa.realtime.kafka.utils.ThreadExceptionHandler
import scala.io.Source

/**
  * 向综合报警服务器发送布控结果信息的工具
  */
class SendHttpMessageUtils(val props: Properties, consumer: DirectKafkaConsumer) extends Logging {

    val charset = Charset.forName("UTF-8")
    val date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    val host_part = props.getProperty("blacklist.result.send.http.host_port", "localhost:8080")
    val uri = props.getProperty("blacklist.result.send.http.app_uri", "uams/alarm/saveAlarms")
    val param_key = props.getProperty("blacklist.result.send.http.param.key", "alarms")
    val url = s"http://${host_part}/${uri}"

    //  val error_path = "error/"
    val error_path = props.getProperty("error.dir", "/netposa/data/black_list/error/")
    val error_move_path = props.getProperty("error.move.dir", "/netposa/data/black_list/error_move/")
    val sdf: SimpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss")

    info("get connection uri is %s".format(url))
    val interval_thread: Int = 500
    val flush_interval: Int = {
        val tmp = props.getProperty("blacklist.result.flush.interval.ms", "3000").toInt
        if (tmp < interval_thread) {
            interval_thread
        } else {
            tmp
        }
    }
    val flush_number: Int = props.getProperty("blacklist.result.flush.number", "100").toInt
    var last_flush_time: Long = 0
    val max_attemp = 6

    import scala.collection.mutable.Map

    val offsets = Map[TopicAndPartition, Long]()
    var offset_last_flush_time: Long = -1

    val buffer = new ListBuffer[AlarmRecord]()
    var buffer_size: Int = 0

    var is_send: Boolean = false
    var stoped: Boolean = false

    val debug = props.getProperty("blacklist.process.debug.enable", "false").toBoolean

    val httpclient: CloseableHttpClient = HttpClients.createDefault()

    val flushThread = new Thread() {
        override def run() = {
            while (!stoped) {
                try {
                    /*
                    if (debug)
                      info("flushThread run, buffer size is %d and last flush time is %d"
                          .format(buffer_size,last_flush_time))
                    */
                    if (buffer_size >= flush_number) {
                        if (debug) info("execute send data to alarm server by flush number case!")
                        is_send = true
                        try {
                            sendData()
                        } catch {
                            case e: Throwable => warn("flush numbers datas error => %s".format(e.getMessage()), e)
                        }
                        last_flush_time = System.currentTimeMillis()
                        is_send = false
                        //commit offset by flush to db
                        offsets.synchronized({
                            if (offsets.size > 0) {
                                consumer.commitOffsets(offsets.toMap)
                                offsets.clear()
                                offset_last_flush_time = System.currentTimeMillis()
                            }
                        })
                    } else if (System.currentTimeMillis() >= (last_flush_time + flush_interval) && buffer_size > 0) {
                        if (debug) info("execute send data to alarm server by flush interval case!")
                        is_send = true
                        try {
                            sendData()
                        } catch {
                            case e: Throwable => warn("flush interval datas error => %s".format(e.getMessage()), e)
                        }
                        last_flush_time = System.currentTimeMillis()
                        is_send = false
                        //commit offset by flush to db
                        offsets.synchronized({
                            if (offsets.size > 0) {
                                consumer.commitOffsets(offsets.toMap)
                                offsets.clear()
                                offset_last_flush_time = System.currentTimeMillis()
                            }
                        })
                    } //end if
                    offsets.synchronized({
                        if (offsets.size > 0
                                && System.currentTimeMillis() >= (offset_last_flush_time + flush_interval)) {
                            consumer.commitOffsets(offsets.toMap)
                            offsets.clear()
                            offset_last_flush_time = System.currentTimeMillis()
                        }
                    })
                    Thread.sleep(interval_thread)
                } catch {
                    case e: Throwable => {
                        warn("send http message thread while error %s".format(e.getMessage()), e)
                    }
                }
            } //end while
        } //end run
    }

    flushThread.setDaemon(true)
    flushThread.setUncaughtExceptionHandler(new ThreadExceptionHandler)
    flushThread.start()

    /**
      * 添加需要推送的布控报警信息,等待对信息进行推送
      */
    def addAlarm(record: AlarmRecord) {
        buffer.synchronized({
            buffer += record
            buffer_size += 1
        })
    }

    private def sendData(): Boolean = {
        val datas = buffer.synchronized({
            val tmp = buffer.clone
            buffer.clear
            buffer_size = 0
            tmp
        })
        import scala.collection.immutable.Map
        val tmp_buffer = new ListBuffer[Map[String, Any]]()
        datas.foreach(f => {
            val record = Map[String, Any](
                "oriId" -> f.oriId.toString,
                "alarmObj" -> f.alarmObj,
                "alarmObjName" -> new String(f.alarmObjName, charset),
                "alarmLevel" -> f.alarmLevel,
                "alarmType" -> f.alarmType,
                "alarmStatus" -> f.alarmStatus,
                "source" -> f.source,
                "absTime" -> date_format.format(new Date(f.absTime)),
                "comment" -> f.comment,
                "dealStatus" -> f.dealStatus,
                "dealUser" -> f.dealUser,
                "dealTime" -> f.dealTime,
                "orgId" -> f.orgId,
                //"monitorId" -> f.monitorid,
                "userId" -> f.userId,
                "detail" -> Map[String, String](
                    "plateColor" -> new String(f.detail_plateColor, charset),
                    "monitorId" -> new String(f.detail_monitorId, charset),
                    "x" -> f.detail_x.toString,
                    "y" -> f.detail_y.toString,
                    "passTime" -> date_format.format(new Date(f.detail_passTime)),
                    "vehicleColor" -> new String(f.detail_vehicleColor, charset),
                    "vehicleBrand" -> new String(f.detail_vehicleBrand, charset),
                    "vehicleType" -> new String(f.detail_vehicleType, charset),
                    "speed" -> f.detail_speed.toString,
                    "channelId" -> new String(f.detail_channelId, charset),
                    "channelName" -> f.detail_channelName,
                    "monitorName" -> f.detail_monitorName,
                    "plateType" -> new String(f.detail_plateType, charset),
                    "imageURLs" -> new String(f.detail_imageURLs, charset)) //end detail map
            ) //end record map
            tmp_buffer += record
        })
        val send_datas = tmp_buffer.toList
        val json = Json.encode(tmp_buffer)
        //val json = JSONArray.toJSONString(send_datas)
        if (debug) {
            info("send message is %s".format(json))
        }
        var attemp: Int = 0
        var success: Boolean = false
        while (!success && attemp < max_attemp) {
            try {
                val httpost = new HttpPost(url);
                val nvps = List[NameValuePair](new BasicNameValuePair(param_key, json))
                httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8))
                val response = httpclient.execute(httpost)

                if (response.getStatusLine().getStatusCode() == 200) {
                    attemp = max_attemp
                    success = true
                    info("send data response code is %d".format(response.getStatusLine().getStatusCode()))
                } else {
                    attemp += 1
                    success = false
                    error("send data response code is %d".format(response.getStatusLine().getStatusCode()))
                }

                response.close()
            } catch {
                case e: Throwable => {
                    warn("send data error, retry is [%d], error msg => %s"
                            .format(attemp, e.getMessage()), e)
                    attemp += 1
                }
            }
        }

        if (success) {
            //处理异常数据（发送不成功的数据）
            sendErrorMsgFromFile()
        } else {
            //发送失败，把数据写入到文件中
            writeErrorMsgToFile(json)
        }

        true
    }

    def writeErrorMsgToFile(json: String) {
        val path = new File(error_path)
        if (!path.exists()) {
            path.mkdirs()
        }
        val file = new File(error_path + sdf.format(new Date()))
        val out = new FileOutputStream(file, true);

        try {
            val fw = new FileWriter(file);
            val writer = new BufferedWriter(fw);
            writer.write(json);
            writer.newLine(); //换行
            writer.flush();
            writer.close();
            fw.close();
        } catch {
            case e: Throwable => {
                warn("write Error Msg fail => %s".format(e.getMessage()), e)
            }
        }
    }

    def sendErrorMsgFromFile() {
        val move_path = new File(error_move_path)
        if (!move_path.exists()) {
            move_path.mkdir()
        }

        val path = new File(error_path)
        if (path.exists()) {
            val file = new File(error_path)
            val files = file.listFiles()
            if (null != files && files.size > 0) {
                files.foreach { f =>
                    try {
                        val reader = new FileReader(f)
                        val br = new BufferedReader(reader)
                        var json: String = br.readLine()
                        while (json != null) {
                            val httpost = new HttpPost(url)
                            val nvps = List[NameValuePair](new BasicNameValuePair(param_key, json))
                            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8))
                            val response = httpclient.execute(httpost)
                            response.close()
                            json = br.readLine()
                        }
                        br.close()
                        reader.close()
                        f.delete()
                    } catch {
                        case e: Throwable => {
                            warn("write Error Msg fail => %s".format(e.getMessage()), e)
                            try {
                                val writer = new BufferedWriter(new FileWriter(error_move_path + f.getName))
                                Source.fromFile(f).getLines().foreach(line => writer.write(line, 0, line.length))
                                writer.close()
                                f.delete()
                            } catch {
                                case e: Throwable => {
                                    warn("Move Error File fail => %s".format(e.getMessage()), e)
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    def shutdown() {
        while (is_send) {
            Thread.sleep(200)
        }
        stoped = true
        httpclient.close()
    }

}

case class AlarmRecord(
                              val oriId: Long,
                              val alarmObj: String,
                              val alarmObjName: Array[Byte],
                              val absTime: Long,
                              val alarmLevel: String,
                              val alarmType: String,
                              val alarmStatus: String,
                              val detail_plateColor: Array[Byte],
                              val detail_monitorId: Array[Byte],
                              val detail_x: Double,
                              val detail_y: Double,
                              val detail_passTime: Long,
                              val detail_vehicleColor: Array[Byte],
                              val detail_vehicleBrand: Array[Byte],
                              val detail_vehicleType: Array[Byte],
                              val detail_speed: Float,
                              val detail_channelId: Array[Byte],
                              val detail_plateType: Array[Byte],
                              val detail_imageURLs: Array[Byte],
                              val orgId: String,
                              val userId: String) {
    val detail_channelName: String = ""
    val detail_monitorName: String = ""
    val dealStatus: String = "0"
    val dealUser: String = ""
    val dealTime: String = ""
    val source: String = "3"
    val comment: String = "comment"
}
