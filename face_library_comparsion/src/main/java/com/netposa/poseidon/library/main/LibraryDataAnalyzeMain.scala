package com.netposa.poseidon.library.main

import java.io.InputStream
import java.util
import java.util.Properties

import org.apache.ignite.{Ignite, IgniteCache, Ignition}
import com.netposa.poseidon.library.init.{LoadPropers, ThreadExceptionHandler}
import com.netposa.poseidon.library.service.{FreeIgniteMemoey, LoadComparisonData}
import com.netposa.poseidon.library.util.HbaseUtil
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.configuration.CacheConfiguration
import org.slf4j.{Logger, LoggerFactory}

/**
  * 服务程序入口
  */
object LibraryDataAnalyzeMain {
  val LOG: Logger = LoggerFactory.getLogger(LibraryDataAnalyzeMain.getClass)
  val props: Properties = LoadPropers.getProperties()
  val IGNITE_NAME: String = LoadPropers.getProperties().getProperty("ignite.name", "Netposa")
  //加载Ignite配置
  val stream: InputStream = getClass.getClassLoader.getResourceAsStream("default-config.xml")
  //启动客户端
  Ignition.setClientMode(LoadPropers.getProperties().getProperty("is.rpc.server").toBoolean)
  val ignite: Ignite = Ignition.start(stream)
  //激活持久化功能
  ignite.active(true)

  def main(args: Array[String]): Unit = {
    if (props.getProperty("is.rpc.server").toBoolean) {
      println("....")
      while (ignite.cluster().forServers().nodes().size() < props.getProperty("server.total.nodes", "3").toInt) {
        LOG.error("ignite server nodes can not init finish! Try again in 5 seconds.")
        Thread.sleep(5000)
      }

      //设置全局变量cache块
      if (!setGlobalCache()) {
        LOG.error("set global config error")
        return
      }
      if (props.getProperty("is.load.data","true").toBoolean) {
        //大库小库数据加载
        LoadComparisonData.loadData()
      }
      val META_TABLE = props.getProperty("hbase.meta.data.table.name", "meta_data")
      if (!HbaseUtil.tableIsExists(META_TABLE)) {
        HbaseUtil.createTable(META_TABLE, Array[String]("cf"), null)
      }

      //启动主线程
      val rpcPort = props.getProperty("rpc.port", "30057").trim().toInt
      val thread = new Thread(new LibraryDataAnalyzeRpcServer(rpcPort), "LibraryDataAnalyzeThread")
      thread.setDaemon(true)
      thread.setUncaughtExceptionHandler(new ThreadExceptionHandler)
      thread.start()

      Runtime.getRuntime.addShutdownHook(new Thread() {
        override def run(): Unit = {
          LOG.info("==========execute to exit=========")
          new FreeIgniteMemoey(IGNITE_NAME).freeCache()
          ignite.close()
        }
      })

      while (true) {
        try {
          Thread.sleep(3600000)
        } catch {
          case e: Exception =>
            LOG.error(e.getMessage, e)
        }
      }
      ignite.close()
      LOG.info("shutdown library analyze server ......")
    } else {
      LOG.info("[ignite server start success!]")
    }
  }

  def setGlobalCache(): Boolean = {
    try {
      val cfg = new CacheConfiguration[String,String]()
      cfg.setCacheMode(CacheMode.REPLICATED)
      cfg.setName(IGNITE_NAME + "_LIBRARY_GLOBAL")
      cfg.setMemoryPolicyName("MeM_RANDOM_2_LRU")
      cfg.setRebalanceBatchSize(props.getProperty("rebalance.batch.size.bytes","524288").toInt)
      val cache: IgniteCache[String, String] = Ignition.ignite(IGNITE_NAME).getOrCreateCache(cfg)
      //Ignition.ignite(IGNITE_NAME).destroyCache("LIBRARY_INPUT_RECORD");
      cache.put("rebalance.batch.size.bytes",props.getProperty("rebalance.batch.size.bytes","524288"))
      cache.put("CLIENT_PORT", HbaseUtil.hConfiguration.get("hbase.zookeeper.property.clientPort"))
      cache.put("ZK_QUORUM", HbaseUtil.hConfiguration.get("hbase.zookeeper.quorum"))
      cache.put("TABLENAME", props.getProperty("hbase.meta.data.table.name"))
      cache.put("ALGORITHM", props.getProperty("algorithm.type"))
      cache.put("REGIONNUM", props.getProperty("hbase.split.region.num"))
      cache.put("IGNITENAME", props.getProperty("ignite.name"))
      cache.put("VERSIONSIZE", props.getProperty("face.version.size"))
      cache.put("VERSION", props.getProperty("face.arithmetic.version"))
      true
    } catch {
      case e: Exception =>
        LOG.error(e.getMessage, e)
        false
    }
  }
}