package com.netposa.poseidon.face.main

import java.io.{File, InputStream}
import java.text.SimpleDateFormat
import java.util.{Calendar, Properties}

import org.apache.ignite.{Ignite, IgniteCache, Ignition}
import com.netposa.poseidon.face.init.ThreadExceptionHandler
import com.netposa.poseidon.face.init.LoadPropers
import com.netposa.poseidon.face.service.LoadFaceDataInit
import com.netposa.poseidon.face.util.HbaseUtil
import org.slf4j.{Logger, LoggerFactory}
import scala.collection.JavaConversions._

/**
  * 服务程序入口
  */
object FaceFeatureDataAnalyzeMain {
    val LOG: Logger = LoggerFactory.getLogger(FaceFeatureDataAnalyzeMain.getClass)
    val SERVER_PROP: Properties = LoadPropers.getProperties()
    val KAFKA_PROP: Properties = LoadPropers.getProperties("kafka-config")
    //加载Ignite配置
    val STREAM: InputStream = getClass.getClassLoader.getResourceAsStream("default-config.xml")
    //启动客户端
    Ignition.setClientMode(SERVER_PROP.getProperty("is.rpc.server", "true").trim.toBoolean)
    val Data_LOAD:Boolean = SERVER_PROP.getProperty("is.load.data", "true").trim.toBoolean
    var IGNITE: Ignite = Ignition.start(STREAM)
        IGNITE.active(true)
    var FACE_DATA_COUNT = 0
    var FACE_IGNITE_DATA_COUNT = 0
    val DURATION: Int = SERVER_PROP.getProperty("duration.time", "30").trim.toInt
    val SDF: SimpleDateFormat = new SimpleDateFormat("yyyyMMdd")
    val IGNITE_NAME: String = SERVER_PROP.getProperty("ignite.name", "Netposa").trim
    val GLOBAL_CACHE_NAME: String = IGNITE_NAME + "_FACE_GLOBAL"
    val ZK_CACHE_NAME: String = "ZK" + "_FACE_GLOBAL"
//    val ZK_QUORUM:String = HbaseUtil.hConfiguration.get("hbase.zookeeper.quorum")
//    val CLIENT_PORT:String = HbaseUtil.hConfiguration.get("hbase.zookeeper.property.clientPort")
    var START_TIME: Long = 0
    def main(args: Array[String]): Unit = {
        if (SERVER_PROP.getProperty("is.rpc.server").toBoolean) {
            while (IGNITE.cluster().forServers().nodes().size() < Integer.parseInt(SERVER_PROP.getProperty("server.total.nodes"))) {
                LOG.error(">>>")
                LOG.error(">>> server nodes is not enough!")
                LOG.error(">>>")
                Thread.sleep(5000)
            }
            if (DURATION < 0) {
                LOG.error("config duration.time can not less than 0")
                return
            }
            //设置全局变量cache块
            try {
                val cache: IgniteCache[String, Properties]  = Ignition.ignite(IGNITE_NAME).getOrCreateCache(GLOBAL_CACHE_NAME)
                cache.put("SERVER", SERVER_PROP)
                cache.put("KAFKA", KAFKA_PROP)
                val zkcache: IgniteCache[String, String]  = Ignition.ignite(IGNITE_NAME).getOrCreateCache(ZK_CACHE_NAME)
                zkcache.put("ZK_QUORUM",HbaseUtil.hConfiguration.get("hbase.zookeeper.quorum"))
                zkcache.put("CLIENT_PORT",HbaseUtil.hConfiguration.get("hbase.zookeeper.property.clientPort"))
            } catch {
                case e: Exception =>
                    LOG.error(e.getMessage, e)
            }

            //人脸以图搜图数据加载
          if(Data_LOAD){
            LOG.info("destroyCache the [face_comparision_service] created cache...")
            destroyCache()
            FACE_DATA_COUNT = LoadFaceDataInit.initCaches()
          }else LOG.info("Data has been loaded,skipping load from hbase!")
          FACE_IGNITE_DATA_COUNT = FACE_IGNITE_DATA_COUNT + FACE_DATA_COUNT
          //从kafka获取数据存储到Hbase线程
            val fetchMessageThread = new FetchMessageThread()
            fetchMessageThread.setUncaughtExceptionHandler(new ThreadExceptionHandler())
            fetchMessageThread.start()
            //启动主线程
            val rpcPort = SERVER_PROP.getProperty("rpc.port", "30055").trim().toInt
            val thread = new Thread(new FaceFeatureDataAnalyzeRpcServer(rpcPort), "FaceFeatureDataAnalyzeThread")
            thread.setDaemon(true)
            thread.setUncaughtExceptionHandler(new ThreadExceptionHandler)
            thread.start()

            //JVM退出时执行资源释放操作
            Runtime.getRuntime.addShutdownHook(new Thread() {
                override def run(): Unit = {
                    LOG.info("==========execute to exit=========")
                    IGNITE.close()
                    fetchMessageThread.shutdown()
                }
            })

            try {
                START_TIME = new SimpleDateFormat("yyyy-MM-dd").parse(SERVER_PROP.getProperty("start.time")).getTime
            } catch {
                case e: Exception =>
                    LOG.error(e.getMessage, e)
            }

            while (true) {
                try {
                    val calendar: Calendar = Calendar.getInstance()
                    calendar.add(Calendar.DATE, (-1) * (DURATION + 1))
                    while (calendar.getTimeInMillis > START_TIME) {
                        val overDate: String = SDF.format(calendar.getTime)
                        val cacheName: String = IGNITE_NAME + "_FACE_" + overDate
                        // 清除过期人脸数据
                        if (Ignition.ignite(IGNITE_NAME).cacheNames().contains(cacheName)) {
                            Ignition.ignite(IGNITE_NAME).destroyCache(cacheName)
                            LOG.info("destroy over date data, date is : %s".format(overDate))
                        }
                        calendar.add(Calendar.DATE, -1)
                    }
                    Thread.sleep(3600000)
                } catch {
                    case e: Exception =>
                        LOG.error(e.getMessage, e)
                }
            }
            IGNITE.close()
            LOG.info("shutdown face feature date analyze server ......")
        } else {
            LOG.error("face comparsion service should be a client, please config [is.rpc.server] to true!")
        }
    }

    private def destroyCache():Unit={
      val caches = IGNITE.cacheNames().filter(
        cName=>(cName.startsWith(s"${IGNITE_NAME}_FACE_") && !cName.endsWith("_FACE_GLOBAL"))
      )
       IGNITE.destroyCaches(caches)
       LOG.info(s"destroy caches is ${caches.toString()}")
    }
}