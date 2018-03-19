package com.netposa.poseidon.face.service

import java.io.File

import scala.collection.mutable.ListBuffer
import org.apache.ignite.cluster.ClusterNode
import com.netposa.poseidon.face.main.FaceFeatureDataAnalyzeMain.{IGNITE, IGNITE_NAME, SERVER_PROP}
import com.netposa.poseidon.face.util.HbaseUtil
import org.apache.hadoop.hbase.TableName

import scala.collection.JavaConversions.collectionAsScalaIterable
import org.apache.hadoop.hbase.HRegionLocation
import org.apache.hadoop.hbase.util.Bytes
import org.apache.ignite.compute.ComputeTaskAdapter
import org.apache.ignite.compute.ComputeJob
import org.apache.ignite.compute.ComputeJobResult
import org.slf4j.{Logger, LoggerFactory}


object LoadFaceDataInit {
    val LOG: Logger = LoggerFactory.getLogger(LoadFaceDataInit.getClass)
    val TABLE_NAME: String = SERVER_PROP.getProperty("face.table.name", "face_feature").trim
    var DEBUG: Boolean = true

    //计算资源初始化,加载数据
    def initCaches(): Int = {
        DEBUG = SERVER_PROP.getProperty("service.process.debug.enable", "true").toBoolean
        LOG.info("init hbase connection and ignite caches start......")
        val start = System.currentTimeMillis()
        if (HbaseUtil.tableIsExists(TABLE_NAME)) {
            val regionServers = splitSearch()
            LOG.info("regions : " + regionServers.toString())
            val CacheMap = collection.mutable.Map[ClusterNode, ListBuffer[(String, String, String)]]()
            IGNITE.cluster().forServers().nodes().toList.foreach(node => {
                CacheMap.put(node, regionServers)
            })
            //加载数据
            LOG.info("load face data to caches start......")
            val compute = IGNITE.compute(IGNITE.cluster().forServers())
            val count = compute.execute[Map[ClusterNode, ListBuffer[(String, String, String)]], Int](new LoadFaceDataTask(), CacheMap.toMap)
            LOG.info("==================face load " + count + " data finish,used time: " + (System.currentTimeMillis() - start) + "ms!================")
            count
        } else {
            LOG.info("====table is not exists ,face load data finish,used time: " + (System.currentTimeMillis() - start) + "ms!================")
            0
        }
    }


    //执行split 操作  对查询条件分片
    def splitSearch(): ListBuffer[(String, String, String)] = {
        val regions = ListBuffer[(String, String, String)]()
        val regionLocator = HbaseUtil.getConn.getRegionLocator(TableName.valueOf(TABLE_NAME.getBytes))
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
    private class LoadFaceDataTask extends ComputeTaskAdapter[Map[ClusterNode, ListBuffer[(String, String, String)]], Int] with Serializable {
        override def map(subgrid: java.util.List[ClusterNode], caches: Map[ClusterNode, ListBuffer[(String, String, String)]]): java.util.Map[ComputeJob, ClusterNode] = {
            val result = new java.util.HashMap[ComputeJob, ClusterNode]()
            subgrid.foreach(node => {
                if (caches.contains(node)) {
                    val cacheInfo = caches(node)
                    cacheInfo.foreach(cache => {
                        val job = new LoadFaceDataJob(cache._1, cache._2, IGNITE_NAME)
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