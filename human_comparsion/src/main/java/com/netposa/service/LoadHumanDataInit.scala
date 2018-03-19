package com.netposa.service

import java.util

import com.netposa.init.LoadPropers

import scala.collection.mutable.ListBuffer
import org.apache.ignite.cluster.ClusterNode
import com.netposa.main.HumanFeatureDataAnalyzeMain.{ignite, props}
import com.netposa.util.HbaseUtil
import org.apache.hadoop.hbase.TableName

import scala.collection.JavaConversions.collectionAsScalaIterable
import org.apache.hadoop.hbase.HRegionLocation
import org.apache.hadoop.hbase.util.Bytes
import org.apache.ignite.Ignition
import org.apache.ignite.compute.ComputeTaskAdapter
import org.apache.ignite.compute.ComputeJob
import org.apache.ignite.compute.ComputeJobResult
import org.slf4j.{Logger, LoggerFactory}

object LoadHumanDataInit {
    val LOG: Logger = LoggerFactory.getLogger(LoadHumanDataInit.getClass)
    val TABLENAME: String = props.getProperty("human.table.name", "human_feature")
    var DEBUG: Boolean = true
    val IGNITE_NAME: String = LoadPropers.getProperties().getProperty("ignite.name").trim

    //计算资源初始化,加载数据
    def initCaches(): Int = {
        //删除历史数据块
        val cacheNames: util.Collection[String] = Ignition.ignite(IGNITE_NAME).cacheNames()
        cacheNames.toArray.foreach(cacheName => {
            if (cacheName.toString.split("_")(1).equals("HUMAN") && !cacheName.toString.split("_")(2).equals("GLOBAL")) {
                Ignition.ignite(IGNITE_NAME).destroyCache(cacheName.toString)
                LOG.info("destroy cache [%s]".format(cacheName.toString))
            }
        })

        DEBUG = props.getProperty("service.process.debug.enable", "true").toBoolean
        LOG.info("init hbase connection and ignite caches start......")
        val start = System.currentTimeMillis()
        if (HbaseUtil.tableIsExists(TABLENAME)) {
            val regionServers = splitSearch()
            val CacheMap = collection.mutable.Map[ClusterNode, ListBuffer[(String, String, String)]]()
            ignite.cluster().forServers().nodes().toList.foreach(node => {
                CacheMap.put(node, regionServers)
            })
            //加载数据
            LOG.info("load human data to caches start......")
            val compute = ignite.compute((ignite.cluster().forServers()))
            val count = compute.execute[Map[ClusterNode, ListBuffer[(String, String, String)]], Int](new LoadHumanDataTask(), CacheMap.toMap)
            LOG.info("==================human load " + count + " data finish,used time: " + (System.currentTimeMillis() - start) + "ms!================")
            count
        } else {
            LOG.info("====table is not exists ,human load data finish,used time: " + (System.currentTimeMillis() - start) + "ms!================")
            0
        }
    }

    //执行split 操作  对查询条件分片
    def splitSearch(): ListBuffer[(String, String, String)] = {
        val regions = ListBuffer[(String, String, String)]()
        val regionLocator = HbaseUtil.getConn.getRegionLocator(TableName.valueOf(TABLENAME.getBytes))
        if (!regionLocator.getAllRegionLocations.isEmpty) {
            regionLocator.getAllRegionLocations.toArray(Array[HRegionLocation]()).foreach(region => {
                val key = Bytes.toString(region.getRegionInfo.getStartKey)
                val startKey = "".equals(key) match {
                    case false => key
                    case _ => "00"
                }
                regions += ((region.getHostname, startKey, startKey))

            })
        } else {
            LOG.warn("Hbase regionServer is empty!....")
        }
        regions
    }

    /**
      * 加载数据
      */
    private class LoadHumanDataTask extends ComputeTaskAdapter[Map[ClusterNode, ListBuffer[(String, String, String)]], Int] with Serializable {
        override def map(subgrid: java.util.List[ClusterNode], caches: Map[ClusterNode, ListBuffer[(String, String, String)]]): java.util.Map[ComputeJob, ClusterNode] = {
            val result = new java.util.HashMap[ComputeJob, ClusterNode]()
            subgrid.foreach(node => {
                if (caches.contains(node)) {
                    val cacheInfo = caches(node)
                    cacheInfo.foreach(cache => {
                        val job = new LoadHumanDataJob(cache._1, cache._2, IGNITE_NAME)
                        result.put(job, node)
                    })
                }
            })
            result
        }

        //合并计算结果
        override def reduce(results: java.util.List[ComputeJobResult]): Int = {
            var result = 0
            results.foreach(re => {
                val re2 = re.getData[Int]
                result += re2
            })
            result
        }
    }

}