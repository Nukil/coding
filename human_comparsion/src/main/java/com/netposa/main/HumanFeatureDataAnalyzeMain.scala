package com.netposa.main

import java.io.InputStream
import java.text.SimpleDateFormat
import java.util
import java.util.{Calendar, Properties}

import org.apache.ignite.{Ignite, IgniteCache, Ignition}
import com.netposa.init.{LoadPropers, ThreadExceptionHandler}
import com.netposa.service.LoadHumanDataInit
import org.slf4j.{Logger, LoggerFactory}

/**
  * 服务程序入口
  */
object HumanFeatureDataAnalyzeMain {
    val LOG: Logger = LoggerFactory.getLogger(HumanFeatureDataAnalyzeMain.getClass)
    val props: Properties = LoadPropers.getProperties()
    //加载Ignite配置
    val stream: InputStream = getClass.getClassLoader.getResourceAsStream("default-config.xml")
    //启动客户端
    Ignition.setClientMode(LoadPropers.getProperties().getProperty("is.rpc.server").trim.toBoolean)
    val ignite: Ignite = Ignition.start(stream)
    var humanDataCount = 0
    val duration: Int = LoadPropers.getProperties().getProperty("duration.time", "30").trim.toInt
    val sdf: SimpleDateFormat = new SimpleDateFormat("yyyyMMdd")
    val IGNITE_NAME: String = LoadPropers.getProperties().getProperty("ignite.name").trim

    def main(args: Array[String]): Unit = {
        if (props.getProperty("is.rpc.server").trim.toBoolean) {
            while (ignite.cluster().forServers().nodes().size() < props.getProperty("server.total.nodes", "3").trim.toInt) {
                ignite.log().error(">>>")
                ignite.log().error(">>> server nodes can not init finish!")
                ignite.log().error(">>>")
                Thread.sleep(5000)
            }

            //设置人体全局变量
            if (!setGlobalCache()) {
                LOG.error("set global config error")
                return
            }

            //从kafka获取数据存储到Hbase线程
            val fetch_msg_thread = new FetchMessageThread(props)
            //人体以图搜图数据加载
            humanDataCount = LoadHumanDataInit.initCaches()

            fetch_msg_thread.setUncaughtExceptionHandler(new ThreadExceptionHandler())
            fetch_msg_thread.start()
            //启动主线程
            val rpcPort = props.getProperty("rpc.port", "30056").trim().toInt
            val thread = new Thread(new HumanFeatureDataAnalyzeRpcServer(rpcPort), "HumanFeatureDataAnalyzeThread")
            thread.setDaemon(true)
            thread.setUncaughtExceptionHandler(new ThreadExceptionHandler)
            thread.start()

            Runtime.getRuntime.addShutdownHook(new Thread() {
                override def run(): Unit = {
                    val cacheNames: util.Collection[String] = Ignition.ignite(IGNITE_NAME).cacheNames()
                    cacheNames.toArray.foreach(cacheName => {
                        if (cacheName.toString.split("_")(1).equals("HUMAN")) {
                            Ignition.ignite(IGNITE_NAME).destroyCache(cacheName.toString)
                            LOG.info("destroy cache [%s]".format(cacheName.toString))
                        }
                    })
                    ignite.close()
                    fetch_msg_thread.shutdown()
                }
            })

            while (true) {
                try {
                    val calendar: Calendar = Calendar.getInstance()
                    calendar.add(Calendar.DATE, (-1) * (duration + 1))
                    val overDate: String = sdf.format(calendar.getTime)
                    val cacheName: String = IGNITE_NAME + "_HUMAN_" + overDate
                    // 清除过期人体数据
                    if (Ignition.ignite(IGNITE_NAME).cacheNames().contains(cacheName)) {
                        Ignition.ignite(IGNITE_NAME).destroyCache(cacheName)
                    }
                    Thread.sleep(3600000)
                } catch {
                    case e: Exception =>
                        LOG.error(e.getMessage, e)
                }
            }
            ignite.close()
            LOG.info("shutdown human feature date analyze server ......")
        } else {
            LOG.info("[ignite server start success!]")
        }
    }

    def setGlobalCache(): Boolean = {
        try {
            val cache: IgniteCache[String, String]  = Ignition.ignite(IGNITE_NAME).getOrCreateCache(IGNITE_NAME + "_HUMAN_GLOBAL")
            cache.put("TABLENAME", props.getProperty("human.table.name"))
            cache.put("ALGORITHM", props.getProperty("algorithm.type"))
            cache.put("IGNITENAME", props.getProperty("ignite.name"))
            true
        } catch {
            case e: Exception =>
                LOG.error(e.getMessage, e)
                false
        }
    }
}