package netposa.righttravel.analysis.client

import java.io._
import java.text.SimpleDateFormat
import java.util.{Calendar, Properties}

import netposa.righttravel.analysis.utils.{AnalysisConstants, LoginUtil}
import org.apache.hadoop.conf.Configuration

import scala.collection.mutable.ListBuffer
import org.slf4j.LoggerFactory
import org.apache.log4j.PropertyConfigurator

/**
  * 昼伏夜出分析提供给java应用端调用的接口工具类
  */
object NightTravelAnalysisClient {

    val log4j = getClass.getClassLoader.getResource("config/log4j.properties")
    PropertyConfigurator.configure(log4j)

    val LOG = LoggerFactory.getLogger(NightTravelAnalysisClient.getClass)

    var stoped = false
    val df = new SimpleDateFormat("yyyy-MM-dd")

    def main(args: Array[String]): Unit = {
        //设置停止的监听事件
        Runtime.getRuntime.addShutdownHook(new Thread() {
            override def run(): Unit = {
                stoped = true
            }
        })

        //读取config.properties配置文件,判断要读取的数据是那一天的数据
        val inStream = getClass.getClassLoader.getResourceAsStream(AnalysisConstants.PROP_CONF_NAME)
        val props = new Properties()
        props.load(inStream)
        inStream.close()

        if (props.getProperty("enable.huawei.login", "false").toBoolean) {
            val userPrincipal = "sparkuser"
            val userKeytabPath = props.getProperty("huawei.login.path") + "user.keytab"
            val krb5ConfPath = props.getProperty("huawei.login.path") + "krb5.conf"
            val hadoopConf: Configuration = new Configuration()
            LoginUtil.login(userPrincipal, userKeytabPath, krb5ConfPath, hadoopConf)
        }

        val delay_time_str = props.getProperty("timer.dispatcher.times", "2:00")
        val delay_time_arr = delay_time_str.split(':')
        if (delay_time_arr.length < 1) {
            LOG.error("timer.dispatcher.times hour can not be config, please check config.properties, example is 2:00")
            System.exit(-1)
        }
        val hour = delay_time_arr(0).toInt
        if (hour < 0 || hour > 23) {
            LOG.error("timer.dispatcher.times hour value" +
              " must greater than or equals 0 and less than or equals 23," +
              "please check config.properties, example is 2:00")
            System.exit(-1)
        }
        val minute = if (delay_time_arr.length > 1) {
            delay_time_arr(1).toInt
        } else {
            0
        }
        if (minute < 0 || minute > 59) {
            LOG.error("timer.dispatcher.times minute value" +
              " must greater than or equals 0 and less than or equals 59," +
              "please check config.properties, example is 2:00")
            System.exit(-1)
        }

        val home = System.getenv("NIGHT_HOME")
        LOG.info("task shell directory is %s".format(home))
        //val home = this.getClass.getProtectionDomain.getCodeSource.getLocation.getPath
        val spark_home = System.getenv("SPARK_HOME")
        LOG.info("spark shell directory is %s".format(spark_home))
        val jar_name = "%s.jar".format(System.getenv("SERVER_NAME"))

        var submitArgs = ""
        if ("local".equals(props.getProperty("execute.mode"))) {
            submitArgs = "--master local[*] " +
              "--class netposa.righttravel.analysis.server.NightTravelAnalysisServer " +
              "--name night_travel_analysis_opaq --executor-memory " + props.getProperty("execute.memory") + " " +
              "--driver-memory " + props.getProperty("execute.memory") + " " + args(1)
        } else if ("cluster".equals(props.getProperty("execute.mode"))) {
            submitArgs = "--master " + props.getProperty("spark.url") + " " +
              "--class netposa.righttravel.analysis.server.NightTravelAnalysisServer " +
              "--name night_travel_analysis_opaq --executor-memory " + props.getProperty("execute.memory") + " " +
              "--driver-memory " + props.getProperty("execute.memory") + " --total-executor-cores " + props.getProperty("execute.core.number") + " " + args(1)
        }
        /*val env = Array[String](
          "SPARK_MEM=" + System.getenv("SPARK_MEM"),
          "UI_PORT=" + System.getenv("UI_PORT"))*/
        val env = Array[String]()

        val execStr = "bash -l %s/bin/%s org.apache.spark.deploy.SparkSubmit %s"
          .format(spark_home, "spark-class", submitArgs)

        val spark_class = new File("%s/bin/%s".format(spark_home, "spark-class"))

        LOG.info("execute shell is %s, execute memory is %s".format(execStr, System.getenv("SPARK_MEM")))

        val historyDirPath = props.getProperty("history.data.dir", "history")

        val history_dir = new File(home, historyDirPath)
        if (!history_dir.exists()) {
            history_dir.mkdirs()
        }
        val history_file = new File(history_dir, "history")
        if (!history_file.exists()) {
            history_file.createNewFile()
        }
        val historyDays = props.getProperty("task.execute.history.max.day", "365").toInt
        val historyDateBuffer = new ListBuffer[String]()
        val tmpBuffer = new Array[Byte](10)
        val historyFileStream = new FileInputStream(history_file)

        //得到要延时计算的天数
        val delay_day = {
            var tmp = props.getProperty("compute.delay.day", "1").toInt
            if (tmp < 1) {
                tmp = 1
            }
            tmp
        }
        //得到当前的日期值
        //默认按当前日期减去delay_day进行计算
        val calendarHistory = Calendar.getInstance()
        //此处的日期还需要进行减一操作,如果按当前日期算,结束时间是当前日期减去delay_day的夜间时段的开始值
        calendarHistory.set(Calendar.DATE, calendarHistory.get(Calendar.DATE) - delay_day)
        calendarHistory.set(Calendar.DATE, calendarHistory.get(Calendar.DATE) - historyDays)
        var month = calendarHistory.get(Calendar.MONTH) + 1
        var date = calendarHistory.get(Calendar.DATE)

        val lastSearchDate = calendarHistory.get(Calendar.YEAR) + "-" +
          (if (month < 10) {
              "0" + month
          } else {
              month.toString
          }) + "-" +
          (if (date < 10) {
              "0" + date
          } else {
              date.toString
          })

        while (historyFileStream.read(tmpBuffer) != -1) {
            val date = new String(tmpBuffer)
            if (date.compareTo(lastSearchDate) >= 0) {
                historyDateBuffer += date
            }
        }
        historyFileStream.close()

        //计算历史
        val historyBeginDate = props.getProperty("task.execute.history.begin.date", "none")
        LOG.info("history begin date config is %s, is execute is %s".format(historyBeginDate, "none".equalsIgnoreCase(historyBeginDate)))
        if (!"none".equalsIgnoreCase(historyBeginDate)) {
            val beginDate = if (historyBeginDate.compareTo(lastSearchDate) < 0)
                lastSearchDate
            else
                historyBeginDate

            var searchdate = beginDate
            var currentdate = currentSearchDate(delay_day)
            while (searchdate.compareTo(currentdate) < 0) {

                if (!spark_class.exists()) {
                    LOG.error("spark home %s or 'spark-class' is not found".format(spark_home))
                    throw new ClassNotFoundException("spark home %s or 'spark-class' is not found".format(spark_home))
                    System.exit(-1)
                }

                if (!historyDateBuffer.exists(date => date.equals(searchdate))) {
                    val newEnv = new Array[String](env.length + 1)
                    System.arraycopy(env, 0, newEnv, 0, env.length)
                    newEnv(env.length) = "searchdate=" + searchdate
                    var result = -1
                    while (result != 0) {
                        LOG.info("execute night travel analysis task, execute search date is %s".format(searchdate))
                        val process = Runtime.getRuntime.exec(execStr, newEnv)
                        result = process.waitFor()
                        LOG.info("process return code is %s".format(result))
                    }

                    val fileWriter = new FileWriter(history_file, true)
                    fileWriter.write(searchdate)
                    fileWriter.flush()
                    fileWriter.close()
                } else {
                    LOG.info("skip night travel analysis task, skip search date is %s".format(searchdate))
                }

                searchdate = getNextDate(searchdate)
                currentdate = currentSearchDate(delay_day)
            }
        }

        //计算下一次执行的时间
        val calendar = Calendar.getInstance()
        calendar.setTimeInMillis(System.currentTimeMillis())
        val baseTime = calendar.getTimeInMillis
        calendar.set(Calendar.HOUR, hour)
        calendar.set(Calendar.MINUTE, minute)
        //得到下一次发起的时间,如果发起时间小于当前时间,设置到下一天去.
        var nextExecuteTime = calendar.getTimeInMillis
        if (nextExecuteTime < baseTime) {
            calendar.setTimeInMillis(nextExecuteTime)
            calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1)
            nextExecuteTime = calendar.getTimeInMillis
        }

        //如果配置有启动时,先执行一次,在启动时先执行,并重新计算下次执行时间
        val startTask = props.getProperty("task.execute.start.enable", "true").toBoolean

        if (startTask) {
            //由于启动时需要执行一次任务,重新设置下一次执行时间(主要是在同一天的情况下)
            val dateFormat = new SimpleDateFormat("yyyyMMdd")
            calendar.setTimeInMillis(System.currentTimeMillis())
            val currentDateStr = dateFormat.format(calendar.getTime)
            calendar.setTimeInMillis(nextExecuteTime)
            val nextDateStr = dateFormat.format(calendar.getTime)
            if (nextDateStr.equals(currentDateStr)) {
                //在同一天内,重新设置next execute time
                calendar.setTimeInMillis(nextExecuteTime)
                calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1)
                nextExecuteTime = calendar.getTimeInMillis
            }

            //执行任务.
            LOG.info(
                "execute night travel analysis task, next run time is %d"
                  .format(nextExecuteTime))

            val currentdate = currentSearchDate(delay_day)
            val newEnv = new Array[String](env.length + 1)
            System.arraycopy(env, 0, newEnv, 0, env.length)
            newEnv(env.length) = "searchdate=" + currentdate

            if (!spark_class.exists()) {
                LOG.error("spark home %s or 'spark-class' is not found".format(spark_home))
                throw new ClassNotFoundException("spark home %s or 'spark-class' is not found".format(spark_home))
                System.exit(-1)
            }

            var result = -1
            while (result != 0) {
                LOG.info(
                    "execute night travel analysis task, execute search date is %s".format(currentdate))
                val process = Runtime.getRuntime.exec(execStr, newEnv)
                result = process.waitFor()
            }

            val fileWriter = new FileWriter(history_file, true)
            fileWriter.write(currentdate)
            fileWriter.flush()
            fileWriter.close()
        }

        //启动定时器
        while (!stoped) {

            if (!spark_class.exists()) {
                LOG.error("spark home %s or 'spark-class' is not found".format(spark_home))
                throw new ClassNotFoundException("spark home %s or 'spark-class' is not found".format(spark_home))
                System.exit(-1)
            }

            if (nextExecuteTime <= System.currentTimeMillis()) {
                calendar.setTimeInMillis(nextExecuteTime)
                calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1)
                nextExecuteTime = calendar.getTimeInMillis

                //执行任务.
                LOG.info(
                    "execute night travel analysis task, next run time is %d"
                      .format(nextExecuteTime))

                val currentdate = currentSearchDate(delay_day)
                val newEnv = new Array[String](env.length + 1)
                System.arraycopy(env, 0, newEnv, 0, env.length)
                newEnv(env.length) = "searchdate=" + currentdate

                var result = -1
                while (result != 0) {
                    LOG.info(
                        "execute night travel analysis task, execute search date is %s".format(currentdate))
                    val process = Runtime.getRuntime.exec(execStr, newEnv)
                    result = process.waitFor()
                }

                val fileWriter = new FileWriter(history_file, true)
                fileWriter.write(currentdate)
                fileWriter.flush()
                fileWriter.close()

            } else {
                Thread.sleep(60 * 1000)
            }
        }

    }

    private def getNextDate(currentDate: String): String = {
        val time = df.parse(currentDate).getTime
        val calendar = Calendar.getInstance()
        calendar.setTimeInMillis(time)
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1)
        var month = calendar.get(Calendar.MONTH) + 1
        var date = calendar.get(Calendar.DATE)
        //查询日期
        val search_date = calendar.get(Calendar.YEAR) + "-" +
          (if (month < 10) {
              "0" + month
          } else {
              month.toString
          }) + "-" +
          (if (date < 10) {
              "0" + date
          } else {
              date.toString
          })
        search_date
    }

    private def currentSearchDate(delayDays: Int): String = {
        //得到当前的日期值
        //默认按当前日期减去delay_day进行计算
        val calendar = Calendar.getInstance()
        //此处的日期还需要进行减一操作,如果按当前日期算,结束时间是当前日期减去delay_day的夜间时段的开始值
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - delayDays)
        var month = calendar.get(Calendar.MONTH) + 1
        var date = calendar.get(Calendar.DATE)

        //查询日期
        val search_date = calendar.get(Calendar.YEAR) + "-" +
          (if (month < 10) {
              "0" + month
          } else {
              month.toString
          }) + "-" +
          (if (date < 10) {
              "0" + date
          } else {
              date.toString
          })
        search_date
    }

}
