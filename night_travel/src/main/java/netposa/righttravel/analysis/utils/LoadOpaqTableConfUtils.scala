package netposa.righttravel.analysis.utils

import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.{ Calendar, Properties }
import scala.xml.XML
import org.json.simple.JSONObject
import scala.collection.JavaConversions._

/**
 * 用于加载从OPAQ中需要读取的数据表配置文件的工具类
 */
object LoadOpaqTableConfUtils {

  /**
   * 通过传入的过车记录表配置文件[car_record_conf.xml]读取到的InputStream信息,解析出对应的值,并存储到zk中
   */
  def parseCarRecordConfByStream(conf_stream: InputStream): String = {
    var record = Map[String, String]()
    //加载stream到xml信息
    val car_record_conf = XML.load(conf_stream)
    //解析xml信息的connection部分并存储到map中
    (car_record_conf \ "connection" \\ "_").map(e => {
      e.label match {
        case "driver_class" => record += ("driver_class" -> e.text)
        case "url" => record += ("url" -> e.text)
        case "user" => record += ("user" -> e.text)
        case "password" => {
          val password = e.text
          if (!"NULL".equalsIgnoreCase(password)) {
            record += ("password" -> password)
          }
        }
        case _ =>
      }
    })
    //解析xml的table部分并存储到map中
    val table_name = (car_record_conf \ "table" \ "name").text
    record += ("table_name" -> table_name)
    //解析XML中的fields部分并存储到map中
    val traffic_time = (car_record_conf \ "fields" \ "traffic_time" \ "field").text
    val import_time = (car_record_conf \ "fields" \ "import_time" \ "field").text
    val plate_num = (car_record_conf \ "fields" \ "plate_num" \ "field").text
    val platenum_color = (car_record_conf \ "fields" \ "platenum_color" \ "field").text
    //cache to map
    record += ("traffic_time" -> traffic_time)
    record += ("import_time" -> import_time)
    record += ("plate_num" -> plate_num)
    record += ("platenum_color" -> platenum_color)

    val json = JSONObject.toJSONString(record)

    json
  }

  /**
   * 通过传入的昼伏夜出输出表配置文件[outpu_record_conf.xml]读取到的InputStream信息,解析出对应的值,并存储到zk中
   */
  def parseOutputRecordConfByStream(conf_stream: InputStream): String = {
    var record = Map[String, String]()
    //加载stream到xml信息
    val output_record_conf = XML.load(conf_stream)
    //解析xml信息的connection部分并存储到map中
    (output_record_conf \ "connection" \\ "_").map(e => {
      e.label match {
        case "driver_class" => record += ("driver_class" -> e.text)
        case "url" => record += ("url" -> e.text)
        case "user" => record += ("user" -> e.text)
        case "password" => {
          val password = e.text
          if (!"NULL".equalsIgnoreCase(password)) {
            record += ("password" -> password)
          }
        }
        case _ =>
      }
    })
    //解析xml的table部分并存储到map中
    val table_name = (output_record_conf \ "table" \ "name").text
    record += ("table_name" -> table_name)
    //解析xml信息的fields部分并存储到map中
    (output_record_conf \ "fields" \\ "_").map(e => {
      e.label match {
        case "record_id" => record += ("record_id" -> (e \ "field").text)
        case "record_date" => record += ("record_date" -> (e \ "field").text)
        case "record_type" => record += ("record_type" -> (e \ "field").text)
        case "carnum" => record += ("carnum" -> (e \ "field").text)
        case "carnum_color" => record += ("carnum_color" -> (e \ "field").text)
        case "traffic_count" => record += ("traffic_count" -> (e \ "field").text)
        case "import_time" => {
          val import_time = (e \ "field").text
          if (!"NULL".equalsIgnoreCase(import_time)) {
            record += ("import_time" -> import_time)
          }
        }
        case _ =>
      }
    })

    val json = JSONObject.toJSONString(record)

    json
  }

  /**
   * 返回值,查询的开始时间,查询的结束时间,分析日期,夜出开始-1,夜出开始-2,夜出结束-1,夜出结束-2,并行任务数量
   * @return
   */
  def buildSearchCond(): (String, String, String, Int, Int, Int, Int, Int) = {
    //读取config.properties配置文件,判断要读取的数据是那一天的数据
    val inStream = getClass.getClassLoader.getResourceAsStream(AnalysisConstants.PROP_CONF_NAME)
    val props = new Properties()
    props.load(inStream)
    inStream.close()

    //得到要计算的时间范围
    val begin_time: String = {
      val tmp = props.getProperty("night.start.begin", "21").toInt
      val hour = if (tmp < 10) { "0" + tmp } else { tmp.toString }
      val time = hour + ":00:00"
      time
    }
    val end_time: String = {
      val tmp = props.getProperty("night.start.end", "0").toInt
      val hour = if (tmp < 10) { "0" + tmp } else { tmp.toString }
      val time = hour + ":00:00"
      time
    }

    //得到要延时计算的天数
    val delay_day = {
      var tmp = props.getProperty("compute.delay.day", "1").toInt
      if (tmp < 1) {
        tmp = 1
      }
      tmp
    }

    val night_begin_1 = props.getProperty("night.start.begin", "21").toInt
    val night_begin_2 = props.getProperty("night.start.end", "0").toInt
    val night_stop_1 = props.getProperty("night.stop.begin", "0").toInt
    val night_stop_2 = props.getProperty("night.stop.end", "0").toInt
    val maxtask = props.getProperty("task.execute.max.size", "24").toInt

    //得到当前的日期值
    //默认按当前日期减去delay_day进行计算
    val calendar = Calendar.getInstance()
    //此处的日期还需要进行减一操作,如果按当前日期算,结束时间是当前日期减去delay_day的夜间时段的开始值
    calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - delay_day)
    var month = calendar.get(Calendar.MONTH) + 1
    var date = calendar.get(Calendar.DATE)

    val env_search_date = System.getenv("searchdate")
    val current_date = calendar.get(Calendar.YEAR) + "-" +
      (if (month < 10) { "0" + month } else { month.toString }) + "-" +
      (if (date < 10) { "0" + date } else { date.toString })

    //查询日期
    val search_date = if (env_search_date != null) env_search_date else current_date

    val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
    val time = dateFormat.parse(search_date).getTime
    calendar.setTimeInMillis(time)

    //查询的结束时间
    //以下的00:00:00部分的小时值需要修改在日期的夜晚计算的开始时段的最后一个值,也就是默认0点,
    // 此时的0点是计算的查找数据的结束时间
    if (end_time.compareTo(begin_time) < 0) {
      //此时的开始时间最后值要小于开始时间的开始值,表示跨天,日期加一
      calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1)
      month = calendar.get(Calendar.MONTH) + 1
      date = calendar.get(Calendar.DATE)
    }
    val stop_time = calendar.get(Calendar.YEAR) + "-" +
      (if (month < 10) { "0" + month } else { month.toString }) + "-" +
      (if (date < 10) { "0" + date } else { date.toString }) + " " + end_time

    //查询的开始时间
    //此处的日期还需要进行减一操作,如果按当前日期算,开始时间是当前日期减去2的夜间时段的开始值
    if (end_time.compareTo(begin_time) < 0) {
      //此时的开始时间最后值要小于开始时间的开始值,表示跨天,日期需要先减一
      calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1)
    }
    calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1)
    month = calendar.get(Calendar.MONTH) + 1
    date = calendar.get(Calendar.DATE)
    //以下的00:00:00部分的小时值需要修改在日期的上一天的夜晚计算的开始时段的第一个值,也就是默认21点,
    // 此时的21点是计算的查找数据的开始时间
    val start_time = calendar.get(Calendar.YEAR) + "-" +
      (if (month < 10) { "0" + month } else { month.toString }) + "-" +
      (if (date < 10) { "0" + date } else { date.toString }) + " " + begin_time

    (start_time, stop_time, search_date, night_begin_1, night_begin_2, night_stop_1, night_stop_2, maxtask)
  }

  def main(args: Array[String]): Unit = {
    //val conf_stream = getClass getResourceAsStream "/" + AnalysisConstants.OUTPUT_XML_CONF
    //println(LoadOpaqTableConfUtils.parseOutputRecordConfByStream(conf_stream))
    //val conf_stream = getClass getResourceAsStream "/" + AnalysisConstants.INPUT_XML_CONF
    //println(LoadOpaqTableConfUtils.parseCarRecordConfByStream(conf_stream))
    println(buildSearchCond)
  }

}
