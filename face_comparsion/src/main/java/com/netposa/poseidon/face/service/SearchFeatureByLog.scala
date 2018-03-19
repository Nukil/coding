package com.netposa.poseidon.face.service

import java.net.InetAddress
import java.util

import com.netposa.poseidon.face.main.FaceFeatureDataAnalyzeMain.{IGNITE_NAME, IGNITE}
import com.netposa.poseidon.face.rpc.SearchFeatureByLogInputRecord
import com.netposa.poseidon.face.util.HbaseUtil
import org.apache.hadoop.hbase.{HRegionLocation, TableName}
import org.apache.hadoop.hbase.client.{Get, Scan, Table}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.ignite.{IgniteCache, Ignition}
import org.apache.ignite.cluster.ClusterNode
import org.apache.ignite.compute.{ComputeJob, ComputeJobAdapter, ComputeJobResult, ComputeTaskAdapter}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.ListBuffer

object SearchFeatureByLog {
//    val LOG: Logger = LoggerFactory.getLogger(SearchFeatureByLog.getClass)
//
//    def executeQuery(record: SearchFeatureByLogInputRecord): List[Array[Byte]] = {
//        val compute = ignite.compute(ignite.cluster().forServers())
//        val result = compute.execute[SearchFeatureByLogInputRecord, List[Array[Byte]]](new SearchFeatureByLogTask(), record)
//        result
//    }
//}
//
///**
//  * 组装比对任务
//  */
//private class SearchFeatureByLogTask extends ComputeTaskAdapter[SearchFeatureByLogInputRecord, List[Array[Byte]]] with Serializable {
//    override def map(subgrid: util.List[ClusterNode], record: SearchFeatureByLogInputRecord): util.Map[ComputeJob, ClusterNode] = {
//        val result = new util.HashMap[ComputeJob, ClusterNode]()
//        subgrid.foreach(node => {
//            val job = new SearchFeatureByLogComputeJob(record, IGNITE_NAME)
//            result.put(job, node)
//        })
//        result
//    }
//
//    //合并计算结果
//    override def reduce(results: util.List[ComputeJobResult]): util.List[Array[Byte]] = {
//        val result = new util.ArrayList[Array[Byte]]()
//        results.foreach(re => {
//            val re2 = re.getData[List[Array[Byte]]]
//            re2.foreach(v => {
//                result += v
//            })
//        })
//        result
//    }
//}
//private class SearchFeatureByLogComputeJob(record: SearchFeatureByLogInputRecord, igniteName: String) extends ComputeJobAdapter with Serializable {
//    val PROP_CACHE: IgniteCache[String, String] = Ignition.ignite(igniteName).cache(igniteName + "_FACE_GLOBAL")
//    val LOG: Logger = LoggerFactory.getLogger(SearchFeatureByLog.getClass)
//    override def execute(): util.List[Array[Byte]] = {
//        val logNum: String = record.getLogNum
//        val tableName: String = PROP_CACHE.get("TABLENAME")
//        val resultList: util.List[Array[Byte]] = new util.ArrayList[Array[Byte]]
//        try {
//            val conn = HbaseUtil.getConn
//            val table: Table = conn.getTable(TableName.valueOf(tableName))
//            val hostRowKey = splitSearch(PROP_CACHE).toList
//            val hostname = InetAddress.getLocalHost.getHostName
//
//            hostRowKey.foreach { hrk =>
//                if (hrk._1.equals(hostname)) {
//                    val startRow: String = hrk._2 + record.getGatherTime + record.getLogNum
//                    val get = new Get(startRow.getBytes())
//                    val result = table.get(get)
//                    LOG.info(hrk._2 + " result size is : " + result.size())
//                    if (result != null && result.getValue("cf".getBytes(), "feature".getBytes()) != null) {
//                        resultList.add(result.getValue("cf".getBytes(), "feature".getBytes()))
//                    }
//                }
//            }
//        } catch {
//            case e: Exception =>
//                LOG.error(e.getMessage, e)
//        }
//        resultList
//    }
//    //执行split 操作  对查询条件分片
//    def splitSearch(propCache: IgniteCache[String, String]): ListBuffer[(String, String, String)] = {
//        val regions = ListBuffer[(String, String, String)]()
//        val regionLocator = HbaseUtil.getConn.getRegionLocator(TableName.valueOf(propCache.get("TABLENAME").getBytes))
//        if (!regionLocator.getAllRegionLocations.isEmpty) {
//            regionLocator.getAllRegionLocations.toArray(Array[HRegionLocation]()).foreach(region => {
//                val key = Bytes.toString(region.getRegionInfo.getStartKey)
//                val startKey = "".equals(key) match {
//                    case false => key
//                    case _ => "00"
//                }
//                regions += ((region.getHostname, startKey, startKey))
//
//            })
//        } else {
//            LOG.warn("Hbase regionServer is empty!....")
//        }
//        regions
//    }
}
