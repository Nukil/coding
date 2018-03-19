package netposa.righttravel.analysis.server

import netposa.righttravel.analysis.utils.{AnalysisConstants, LoadOpaqTableConfUtils, LoginUtil, OpaqInputKeyPartitioner}
import org.apache.log4j.PropertyConfigurator

import scala.collection.JavaConversions._
import org.apache.spark.Logging
import org.json.simple.JSONValue
import org.json.simple.JSONObject
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.Date

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.hadoop.mapreduce.Job
import netposa.righttravel.analysis.lib.db.OpaqInputFormat
import netposa.righttravel.analysis.lib.db.OpaqInputKey
import netposa.righttravel.analysis.lib.db.OpaqInputValue
import netposa.righttravel.analysis.lib.db.RightTravelOutputValue
import netposa.righttravel.analysis.lib.db.OpaqOutputFormat
import java.util.Calendar

import org.apache.hadoop.conf.Configuration

import scala.collection.mutable.Buffer
import scala.collection.mutable.ListBuffer

object NightTravelAnalysisServer extends Logging {
    val test = getClass.getClassLoader
    val log4j = getClass.getClassLoader.getResource("config/log4j.properties")
    PropertyConfigurator.configure(log4j)

    /**
      * 从zk中加载过车记录表字段配置与昼伏夜出分析结果存储表字段配置信息
      */
    private def loadRecordConf(): (Map[String, String], Map[String, String]) = {
        var car_record_conf = Map[String, String]()
        var output_record_conf = Map[String, String]()
        try {
            //加载过车记录表的配置信息
            val inStream = getClass.getClassLoader.getResourceAsStream(AnalysisConstants.INPUT_XML_CONF)
            val tmp_car = LoadOpaqTableConfUtils.parseCarRecordConfByStream(inStream)
            val json_car = new String(tmp_car)
            val obj_car = JSONValue.parse(json_car).asInstanceOf[JSONObject]
            obj_car.keySet().foreach(row => {
                obj_car.get(row) match {
                    case null => {}
                    case value: String => {
                        if (!"NULL".equalsIgnoreCase(value)) {
                            car_record_conf += (row.toString -> value)
                        }
                    }
                    case _ => {}
                }
            })
            inStream.close()
            //加载分析结果输出表的配置信息
            val outStream = getClass.getClassLoader.getResourceAsStream(AnalysisConstants.OUTPUT_XML_CONF)
            val tmp_output = LoadOpaqTableConfUtils.parseOutputRecordConfByStream(outStream)
            val json_output = new String(tmp_output)
            val obj_output = JSONValue.parse(json_output).asInstanceOf[JSONObject]
            obj_output.keySet().foreach(row => {
                obj_output.get(row) match {
                    case null => {}
                    case value: String => {
                        if (!"NULL".equalsIgnoreCase(value)) {
                            output_record_conf += (row.toString -> value)
                        }
                    }
                    case _ => {}
                }
            })
            outStream.close()
        } finally {
        }

        (car_record_conf, output_record_conf)
    }

    private def getConnection(class_name: String, url: String, user: String, password: String): Connection = {
        var connection: Connection = null

        while (null == connection) {
            Class.forName(class_name)
            if (null == user) {
                logInfo("user is null get db connection is %s".format(url))
                connection = DriverManager.getConnection(url)
            } else {
                logInfo("user is not null get db connection is %s, user is %s".format(url, user))
                if (password != null) {
                    logInfo("password is not null value is %s".format(password))
                }
                connection = DriverManager.getConnection(url, user, password)
            }
        }

        connection
    }

    /**
      * 删除指定日期下的昼伏夜出输出结果数据
      */
    private def deleteOutputDataByDate(
                                        statement: Statement,
                                        output_conf: Map[String, String],
                                        delete_date: String) {
        try {
            val tmp_sql = new StringBuffer()
            tmp_sql.append("DELETE FROM ").append(output_conf("table_name")).
              append(" WHERE ").append(output_conf("record_date")).
              append(" >= '" + delete_date + " 00:00:00'").append(" and ").
              append(output_conf("record_date")).
              append(" <= '" + delete_date + " 23:59:59'")

            val sql = tmp_sql.toString()

            logInfo(sql)

            statement.execute(sql)
        } catch {
            case _: Exception => {}
        }
    }

    private def selectAbnormalDataByDate(begin_time: String, end_time: String, car_conf: Map[String, String]): String = {
        val hphm_sql = new StringBuffer()
        val tmp_sql = new StringBuffer()

        tmp_sql.append("SELECT hphm,COUNT(1) count FROM ")
          .append(car_conf("table_name"))
          .append(" where hphm not in ('00000000','非机动车','无车牌') and jgsj>='")
          .append(begin_time)
          .append("' and jgsj<'")
          .append(end_time)
          .append("' GROUP BY hphm having count > 2000 ORDER BY count DESC ")

        logInfo("select Abnormal Data By Date sql is :" + tmp_sql.toString())

        val connection = getConnection(car_conf("driver_class"), car_conf("url"), car_conf("user"),
            if (car_conf.keySet.exists(_.equals("password"))) {
                car_conf("password")
            } else {
                null
            })
        val statment = connection.createStatement()
        val rs = statment.executeQuery(tmp_sql.toString())
        while (rs.next()) {
            hphm_sql.append(",'" + rs.getString("hphm") + "'")
        }
        statment.close()
        connection.close()
        hphm_sql.toString()
    }

    /**
      * 生成查询过车记录表的sql语句,没有过车时间条件部分
      */
    private def buildSearchSql(begin_time: String, end_time: String, conf: Map[String, String]): String = {
        val tmp_sql = new StringBuffer()
        tmp_sql.append("SELECT ").append(conf("plate_num") + ",").
          append(conf("platenum_color") + ",").append(conf("traffic_time"))
          .append(" FROM ").append(conf("table_name"))
          .append(" WHERE " + conf("plate_num") + " not in ('00000000','非机动车','无车牌'")

        val hphms = selectAbnormalDataByDate(begin_time, end_time, conf)
        if (null != hphms && hphms.length() > 1) {
            tmp_sql.append(hphms)
        }
        tmp_sql.append(")")
        tmp_sql.toString()
    }

    /**
      * 单次计算执行
      */
    private def executeByCond(begin_time: String, end_time: String,
                              search_date: String, car_conf: Map[String, String],
                              maxtask: Int, job: Job, output_job: Job, sc: SparkContext,
                              start_begin: Int, start_end: Int, stop_begin: Int, stop_end: Int) {
        //bulid search sql
        val search_sql = buildSearchSql(begin_time, end_time, car_conf)

        val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val import_time = dateFormat.format(new Date())

        //conf
        var tmp_conf = Map[String, String]()
        tmp_conf += ("analysis.jdbc.input.query.sql" -> search_sql)
        tmp_conf += ("analysis.jdbc.input.query.begintime" -> begin_time)
        tmp_conf += ("analysis.jdbc.input.query.endtime" -> end_time)
        tmp_conf += ("analysis.jdbc.input.query.searchdate" -> search_date)

        car_conf.foreach(row => {
            row._1 match {
                case "driver_class" => tmp_conf += ("analysis.jdbc.driver.class" -> row._2)
                case "url" => tmp_conf += ("analysis.jdbc.url" -> row._2)
                case "user" => tmp_conf += ("analysis.jdbc.username" -> row._2)
                case "password" => tmp_conf += ("analysis.jdbc.password" -> row._2)
                case "traffic_time" => tmp_conf += ("analysis.jdbc.split.field.name" -> row._2)
                case _ =>
            }
        })
        //conf to hadoop
        tmp_conf.foreach(entry => {
            job.getConfiguration().set(entry._1, entry._2)
        })

        //生成执行程序,当前日期的,此处加载数据
        val opaqRDD = sc.newAPIHadoopRDD(job.getConfiguration(),
            classOf[OpaqInputFormat], classOf[OpaqInputKey], classOf[OpaqInputValue])

        val numPartitions = if (opaqRDD.partitioner.isDefined) {
            opaqRDD.partitioner.get.numPartitions
        } else {
            maxtask
        }

        //按车牌/车牌颜色对车辆过车记录进行分组
        val groupRDD = opaqRDD.groupByKey(new OpaqInputKeyPartitioner(numPartitions)).filter(f => {
            if (f._2.size > 1000) {
                false
            } else {
                true
            }
        })

        val reduceRDD = groupRDD.reduceByKey(new OpaqInputKeyPartitioner(numPartitions), (v1, v2) => {
            val buf: Buffer[OpaqInputValue] = v1.toBuffer ++ v2.toBuffer
            buf.toIterable
        })

        val calendar = Calendar.getInstance();
        val filterRDD = reduceRDD.filter(record => {
            var flag = true
            val tmp_arr = record._2.iterator
            while (tmp_arr.hasNext && flag) {
                val tmp_time = tmp_arr.next.getTrafficTime()
                calendar.setTimeInMillis(tmp_time)
                flag = (calendar.get(Calendar.HOUR_OF_DAY) >= start_begin || calendar.get(Calendar.HOUR_OF_DAY) < stop_end)
            }
            flag
        }).filter(record => {
            //把大于1次的值取出来,此部分部用来写入到数据库
            val flag = record._2.size > 1
            flag
        })

        //执行晚间计算
        val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val df_date = new SimpleDateFormat("yyyy-MM-dd")
        val df_date_str = new SimpleDateFormat("yyyyMMdd")
        var begin_flag = true
        var begin = start_begin
        //迭代开始夜间时段
        while (begin_flag) {
            val begin_hour = (if (begin < 10) {
                "0" + begin
            } else {
                begin.toString
            })
            //计算出需要判断的开始时间
            val begin_date = df.parse(search_date + " " + begin_hour + ":00:00")
            val calendar = Calendar.getInstance()
            calendar.setTime(begin_date)
            //把日期减去1
            if (begin >= start_begin && begin <= 23) {
                calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1)
            }
            //得到判断的开始时间值
            val firsttime = calendar.getTimeInMillis()

            var stop_flag = true
            var stop = stop_begin
            //迭代结束夜间时段
            while (stop_flag) {
                val stop_hour = (if (stop < 10) {
                    "0" + stop
                } else {
                    stop.toString
                })
                val lasttime = df.parse(search_date + " " + stop_hour + ":00:00").getTime()

                //把符合条件的车辆进行次数统计
                val right_travel_type = begin_hour + stop_hour

                //过滤掉在非夜间范围内出现过的车辆
                val rigthTravelRDD = filterRDD.filter(record => {
                    var flag = true
                    val tmp_arr = record._2.iterator
                    while (tmp_arr.hasNext && flag) {
                        val tmp_time = tmp_arr.next.getTrafficTime()
                        flag = (tmp_time >= firsttime && tmp_time < lasttime)
                    }
                    flag
                }).filter(record => {
                    //把大于1次的值取出来,此部分部用来写入到数据库
                    val flag = record._2.size > 1
                    flag
                })

                if (!rigthTravelRDD.isEmpty()) {

                    val tmpCalendar = Calendar.getInstance()
                    tmpCalendar.setTimeInMillis(df.parse(search_date + " 00:00:00").getTime)
                    //tmpCalendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1)
                    tmpCalendar.set(Calendar.DATE, tmpCalendar.get(Calendar.DATE))

                    val real_date = df_date.format(tmpCalendar.getTime)
                    val real_date_str = df_date_str.format(tmpCalendar.getTime)

                    logInfo("filter size is **, type is [%s], search date is [%s], real date is [%s]"
                      .format(right_travel_type, search_date, real_date))

                    val countRDD = rigthTravelRDD.map(car => {
                        val tmp_v = new RightTravelOutputValue()
                        tmp_v.setSearchDate(real_date)
                        tmp_v.setResultType(right_travel_type)
                        tmp_v.setCount(car._2.size)
                        tmp_v.setImportTime(import_time)
                        tmp_v.setKey(real_date_str + "|" + right_travel_type + "|" + car._1.getPlateNum() + "|" + car._1.getPlateNumColor())

                        (car._1, tmp_v)
                    })

                    //TODO 写入数据库
                    output_job.setOutputKeyClass(classOf[OpaqInputKey])
                    output_job.setOutputValueClass(classOf[RightTravelOutputValue])

                    countRDD.saveAsNewAPIHadoopDataset(output_job.getConfiguration())
                }

                stop += 1
                if (stop == (stop_end + 1)) {
                    stop_flag = false
                }
            } //end while stop

            begin += 1
            if (begin > 23) {
                begin = 0
            }
            val end = if (start_end == 23) {
                0
            } else {
                (start_end + 1)
            }
            if (begin == end) {
                begin_flag = false
            }
        } //end while begin

    }

    /**
      * 执行分析处理
      */
    def executeAnalysis(begin_time: String,
                        end_time: String,
                        search_date: String,
                        car_conf: Map[String, String],
                        output_conf: Map[String, String],
                        start_begin: Int, start_end: Int,
                        stop_begin: Int, stop_end: Int, maxtask: Int) {
        //val maxtask = "24" //执行的任务并行度

        val sparkConf = new SparkConf().setAppName("night_travel_analysis_" + search_date)
        sparkConf.set("spark.shuffle.compress", "true")
        sparkConf.set("spark.storage.memoryFraction", "0.8")
        sparkConf.set("spark.shuffle.file.buffer.kb", "2048")
        sparkConf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
        sparkConf.set("spark.kryo.registrator",
            "netposa.righttravel.analysis.utils.AnalysisRegistrator")
        sparkConf.set("spark.kryoserializer.buffer.mb", "64")
        if (System.getenv("UI_PORT") != null) {
            sparkConf.set("spark.ui.port", System.getenv("UI_PORT"))
        }

        //sc.setCheckpointDir("")
        val job = Job.getInstance()
        job.getConfiguration().set("exclude.carnum.startwith", "0000,8888,9999")
        job.getConfiguration().set("analysis.jdbc.task.num", maxtask + "")

        //conf output to hadoop job
        val output_job = Job.getInstance()
        var output_tmp_conf = Map[String, String]()
        output_conf.foreach(row => {
            row._1 match {
                case "driver_class" => output_tmp_conf += ("analysis.jdbc.driver.class" -> row._2)
                case "url" => output_tmp_conf += ("analysis.jdbc.url" -> row._2)
                case "user" => output_tmp_conf += ("analysis.jdbc.username" -> row._2)
                case "password" => output_tmp_conf += ("analysis.jdbc.password" -> row._2)
                case _ =>
            }
        })
        output_tmp_conf.foreach(entry => {
            output_job.getConfiguration().set(entry._1, entry._2)
        })
        val output_table_name = output_conf("table_name")
        val output_field_names = Array[String](output_conf("record_id"),
            output_conf("carnum"), output_conf("carnum_color"),
            output_conf("record_date"), output_conf("record_type"),
            output_conf("import_time"), output_conf("traffic_count"))

        OpaqOutputFormat.setOutput(output_job, output_table_name, output_field_names)

        //对前几天的记录进行分析,检查是否需要重新计算,通过读取历史天数上的最大导入时间与现存的日期上的最大导入时间比较
        var connection: Connection = null
        var statement: Statement = null

        while (connection == null) {
            try {
                connection = getConnection(output_conf("driver_class"),
                    output_conf("url"), output_conf("user"),
                    if (output_conf.keySet.exists(_.equals("password"))) {
                        output_conf("password")
                    } else {
                        null
                    })
            } catch {
                case e: Exception => {
                    log.error("Get deleteOutput Connection fail , message is : " + e.getMessage, e)
                }
                    Thread.sleep(1000 * 60)
            }
        }

        //删除需要重新计算的数据
        try {
            statement = connection.createStatement()
            //删除old的输出数据
            deleteOutputDataByDate(statement, output_conf, search_date)
        } finally {
            if (null != statement) statement.close()
            if (null != connection) connection.close()
        }

        val sc = new SparkContext(sparkConf)

        //执行当前的请求
        executeByCond(begin_time, end_time, search_date, car_conf,
            maxtask, job, output_job, sc,
            start_begin, start_end, stop_begin, stop_end)

    }

    def main(args: Array[String]): Unit = {

        try {
            //解析传入参数
            val searchConds = LoadOpaqTableConfUtils.buildSearchCond()

            logInfo("execute analysis search date is %s".format(searchConds._3))

            //加载表配置信息
            val conf = loadRecordConf()

            logInfo("input config file value is => %s".format(conf._1))
            logInfo("outout config file value is => %s".format(conf._2))

            //执行分析
            executeAnalysis(
                searchConds._1,
                searchConds._2,
                searchConds._3,
                conf._1,
                conf._2,
                searchConds._4,
                searchConds._5,
                searchConds._6,
                searchConds._7,
                searchConds._8)
        } catch {
            case e: Exception => {
                log.error(e.getMessage, e)
            }
        }

    }

}
