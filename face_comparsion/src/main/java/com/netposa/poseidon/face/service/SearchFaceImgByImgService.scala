package com.netposa.poseidon.face.service

import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util
import java.util.{PriorityQueue, Properties}

import com.netposa.poseidon.face.bean.CacheFaceFeature
import com.netposa.poseidon.face.main.FaceFeatureDataAnalyzeMain.{IGNITE, IGNITE_NAME, SERVER_PROP}
import com.netposa.poseidon.face.rpc.{SearchImgByImgInputRecord, SearchImgResult}
import com.netposa.poseidon.face.util.{FaceFeatureVerify, FaceFeatureVerifyDg, HbaseUtil}
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HRegionLocation, TableName}
import org.apache.ignite.cache.CachePeekMode
import org.apache.ignite.cluster.ClusterNode
import org.apache.ignite.compute.{ComputeJob, ComputeJobAdapter, ComputeJobResult, ComputeTaskAdapter}
import org.apache.ignite.{Ignite, IgniteCache, IgniteClientDisconnectedException, Ignition}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConversions.{asScalaBuffer, seqAsJavaList, _}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by Administrator on 2016/10/28.
  */
object SearchFaceImgByImgService {
    val LOG: Logger = LoggerFactory.getLogger(SearchFaceImgByImgService.getClass)
    val TABLENAME: String = SERVER_PROP.getProperty("face.table.name", "face_feature").trim
    val ALGORITHM: String = SERVER_PROP.getProperty("algorithm.type", "netposa").trim
    var DEBUG: Boolean = SERVER_PROP.getProperty("service.process.debug.enable", "true").trim.toBoolean
    var FEATURESIZE: Int = SERVER_PROP.getProperty("face.feature.size").trim.toInt
    var STARTPOST: Int = SERVER_PROP.getProperty("face.tail.size").trim.toInt + SERVER_PROP.getProperty("face.version.size").trim.toInt - 1

    var IS_NODE_RECONNECITON = false
    def executeQuery(record: SearchImgByImgInputRecord, dates: java.util.List[String]): java.util.List[SearchImgResult] = {
        runJobs(record.getCameraId, record.getCount, record.getDistance, record.getFeature, dates)
    }
    def runJobs(cameraId: String, count: Int, distance: Int, feature: Array[Byte], dates: java.util.List[String]):
    List[SearchImgResult] = {
        val time0 = System.currentTimeMillis()
        val compute = IGNITE.compute(IGNITE.cluster().forServers())
        val time1 = System.currentTimeMillis()
        if(IS_NODE_RECONNECITON) {
            LOG.warn("The client is reconnecting ,please waiting. this search is return null")
            return List[SearchImgResult]()
        }
        val result = try {
            compute.execute[(String, Int, Int, Array[Byte], java.util.List[String]), List[Value]](new FaceComputeTask(), (cameraId, count, distance, feature, dates))
        }catch {
            case e:IgniteClientDisconnectedException=>
            LOG.warn("The client is reconnecting ,please waiting...")
             IS_NODE_RECONNECITON =true
             IGNITE = e.reconnectFuture().get().asInstanceOf[Ignite]
             IGNITE.active(true)
             IS_NODE_RECONNECITON = false
            LOG.info("The client has been reconnected!")
            return List[SearchImgResult]()
        }
            val time2 = System.currentTimeMillis()
            try {
                //(calh,LogNum,cameraId,gatherTime)
                val time3 = System.currentTimeMillis()
                val restr = result.take(count).map(v => {
                    val res = new SearchImgResult()
                    if (v.i._2 != null) {
                        res.setLogNum(v.i._2)
                    }
                    if (v.i._3 != null) {
                        res.setCameraId(v.i._3)
                    }
                    res.setGatherTime(v.i._4)
                    res.setScore(v.i._1)
                })
                val time4 = System.currentTimeMillis()
                if (DEBUG) {
                    LOG.info("====job has been finished ==`time is " + (time2 - time1))
                }
                restr
            } catch {
                case e: Exception => {
                    LOG.error("search result mkstring or clean cache failed!,info is %s".format(e.getMessage), e)
                    List[SearchImgResult]()
                }
            }
        }

}

/**
  * 组装比对任务
  */
private class FaceComputeTask extends ComputeTaskAdapter[(String, Int, Int, Array[Byte], java.util.List[String]), List[Value]] with Serializable {
    val log: Logger = LoggerFactory.getLogger("FaceComputeTask")

    override def map(subgrid: util.List[ClusterNode], record: (String, Int, Int, Array[Byte], java.util.List[String])): util.Map[ComputeJob, ClusterNode] = {
        val startTime = System.currentTimeMillis()
        val result = new util.HashMap[ComputeJob, ClusterNode]()
        subgrid.foreach(node => {
            record._5.foreach { date =>
                val job = new FaceComputeJob((record._1, record._2, record._3, record._4, date), IGNITE_NAME)
//                    log.info("============parallel job map time is: " + (System.currentTimeMillis() - startTime) + "============ms!")
                result.put(job, node)
            }
        })
        result
    }

    //合并计算结果
    override def reduce(results: util.List[ComputeJobResult]): List[Value] = {
        val startTime = System.currentTimeMillis()
        val result = new mutable.PriorityQueue[Value]()
        results.foreach(re => {
            val re2 = re.getData[List[Value]]
//                log.info("============parallel job  reduce time is: " + (System.currentTimeMillis() - startTime) + "============ms!")
            re2.foreach(v => {
                result += v
            })
        })
        result.dequeueAll.toList
    }
}

/**
  * 并行在ignite集群节点上开始比对任务
  * record (cameraId,count,distance,feature,date)
  */
private class FaceComputeJob(record: (String, Int, Int, Array[Byte], String), igniteName: String) extends ComputeJobAdapter with Serializable {
    val log: Logger = LoggerFactory.getLogger("FaceComputeJob")
    val FAMILY: Array[Byte] = "cf".getBytes
    val CAMERAID: Array[Byte] = "cameraId".getBytes
    val JLBH: Array[Byte] = "logNum".getBytes
    val FEATURE: Array[Byte] = "feature".getBytes
    val GATHERTIME: Array[Byte] = "gatherTime".getBytes
    val PROP_CACHE: IgniteCache[String, Properties] = Ignition.ignite(igniteName).cache(igniteName + "_FACE_GLOBAL")
    override def execute(): List[Value] = {
        val start = System.currentTimeMillis()
        val sdf: SimpleDateFormat = new SimpleDateFormat("yyyyMMdd")
        var dataSource: String = ""
        var loadTime: Long = 0
        var pqueue = new PriorityQueue[Value](record._2)
        var y = 0
        val time1 = System.currentTimeMillis()
        if (Ignition.ignite(igniteName).cacheNames().contains(record._5)) {
            dataSource = "Ignite"
            val re = Ignition.ignite(igniteName).cache[String, CacheFaceFeature](record._5).localEntries(CachePeekMode.PRIMARY)
            val time2 = System.currentTimeMillis()
            if ("netposa".equalsIgnoreCase(PROP_CACHE.get("SERVER").getProperty("algorithm.type"))) {
                if (record._1 != null && !"".equals(record._1)) {
                    val cameraIds = record._1.split(",")
                    re.foreach { cacheFeature =>
                        y = y + 1
                        val calh = FaceFeatureVerify.verify(record._4, 0, cacheFeature.getValue.getFeature, 0)
                        if (calh >= record._3 && cameraIds.contains(new String(cacheFeature.getValue.getCameraId))) {
                            pqueue.offer(Value(calh, cacheFeature.getKey, new String(cacheFeature.getValue.getCameraId), new String(cacheFeature.getValue.getGatherTime).toLong))
                            if (pqueue.size() > record._2) {
                                pqueue.poll()
                            }
                        }
                    }
                } else {
                    re.foreach { cacheFeature =>
                        y = y + 1
                        val calh = FaceFeatureVerify.verify(record._4, 0, cacheFeature.getValue.getFeature, 0)
                        if (calh >= record._3) {
                            pqueue.offer(Value(calh, cacheFeature.getKey, new String(cacheFeature.getValue.getCameraId), new String(cacheFeature.getValue.getGatherTime).toLong))
                            if (pqueue.size() > record._2) {
                                pqueue.poll()
                            }
                        }
                    }
                }
            } else {
                if (record._1 != null && !"".equals(record._1)) {
                    val cameraIds = record._1.split(",")
                    re.foreach { cacheFeature =>
                        y = y + 1
                        val calh = FaceFeatureVerifyDg.verify(record._4, 0, cacheFeature.getValue.getFeature, 0)
                        if (calh >= record._3 && cameraIds.contains(new String(cacheFeature.getValue.getCameraId))) {
                            pqueue.offer(Value(calh, cacheFeature.getKey, new String(cacheFeature.getValue.getCameraId), new String(cacheFeature.getValue.getGatherTime).toLong))
                            if (pqueue.size() > record._2) {
                                pqueue.poll()
                            }
                        }
                    }
                } else {
                    re.foreach { cacheFeature =>
                        y = y + 1
                        val calh = FaceFeatureVerifyDg.verify(record._4, 0, cacheFeature.getValue.getFeature, 0)
                        if (calh >= record._3) {
                            pqueue.offer(Value(calh, cacheFeature.getKey, new String(cacheFeature.getValue.getCameraId), new String(cacheFeature.getValue.getGatherTime).toLong))
                            if (pqueue.size() > record._2) {
                                pqueue.poll()
                            }
                        }
                    }
                }
            }
            loadTime = time2 - time1
        } else {
            try {
                dataSource = "HBase"
                val conn = HbaseUtil.getConn
                val table: Table = conn.getTable(TableName.valueOf(PROP_CACHE.get("SERVER").getProperty("face.table.name")))
                val hostRowKey = splitSearch(PROP_CACHE).toList
                val hostname = InetAddress.getLocalHost.getHostName
                hostRowKey.foreach { hrk =>
                    if (hrk._1.equals(hostname)) {
                        val millis: Long = sdf.parse(record._5.split("_")(2)).getTime
                        val startRow: String = hrk._2 + millis
                        val endRow: String = hrk._3 + (millis + 86400000)
                        val scan = new Scan
                        scan.setStartRow(startRow.getBytes)
                        scan.setStopRow(endRow.getBytes)
                        scan.setCaching(10000)
                        val start: Long = System.currentTimeMillis()
                        val scanner = table.getScanner(scan)
                        val mid: Long = System.currentTimeMillis()
                        if ("netposa".equalsIgnoreCase(PROP_CACHE.get("SERVER").getProperty("algorithm.type"))) {
                            if (record._1 != null && !"".equals(record._1)) {
                                val cameraIds = record._1.split(",")
                                scanner.foreach { result =>
                                    val logNum = result.getValue(FAMILY, JLBH)
                                    val gatherTime = result.getValue(FAMILY, GATHERTIME)
                                    val cameraId = result.getValue(FAMILY, CAMERAID)
                                    val feature = result.getValue(FAMILY, FEATURE)
                                    if (null != result && null != logNum && null != gatherTime && null != cameraId && null != feature) {
                                        y = y + 1
                                        val calh = FaceFeatureVerify.verify(record._4, 0, feature, 0)
                                        if (calh >= record._3 && cameraIds.contains(new String(cameraId))) {
                                            pqueue.offer(Value(calh, new String(logNum), new String(cameraId), new String(gatherTime).toLong))
                                            if(pqueue.size()>record._2){
                                                pqueue.poll()
                                            }
                                        }
                                    }
                                }
                            } else {
                                scanner.foreach { result =>
                                    val logNum = result.getValue(FAMILY, JLBH)
                                    val gatherTime = result.getValue(FAMILY, GATHERTIME)
                                    val cameraId = result.getValue(FAMILY, CAMERAID)
                                    val feature = result.getValue(FAMILY, FEATURE)
                                    if (null != result && null != logNum && null != gatherTime && null != cameraId && null != feature) {
                                        y = y + 1
                                        val calh = FaceFeatureVerify.verify(record._4, 0, feature, 0)
                                        if (calh >= record._3 ) {
                                            pqueue.offer(Value(calh, new String(logNum), new String(cameraId), new String(gatherTime).toLong))
                                            if(pqueue.size()>record._2){
                                                pqueue.poll()
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            if (record._1 != null && !"".equals(record._1)) {
                                val cameraIds = record._1.split(",")
                                scanner.foreach { result =>
                                    val logNum = result.getValue(FAMILY, JLBH)
                                    val gatherTime = result.getValue(FAMILY, GATHERTIME)
                                    val cameraId = result.getValue(FAMILY, CAMERAID)
                                    val feature = result.getValue(FAMILY, FEATURE)
                                    if (null != result && null != logNum && null != gatherTime && null != cameraId && null != feature) {
                                        y = y + 1
                                        val calh = FaceFeatureVerifyDg.verify(record._4, 0, feature, 0)
                                        if (calh >= record._3 && cameraIds.contains(new String(cameraId))) {
                                            pqueue.offer(Value(calh, new String(logNum), new String(cameraId), new String(gatherTime).toLong))
                                            if(pqueue.size()>record._2){
                                                pqueue.poll()
                                            }
                                        }
                                    }
                                }
                            } else {
                                scanner.foreach { result =>
                                    val logNum = result.getValue(FAMILY, JLBH)
                                    val gatherTime = result.getValue(FAMILY, GATHERTIME)
                                    val cameraId = result.getValue(FAMILY, CAMERAID)
                                    val feature = result.getValue(FAMILY, FEATURE)
                                    if (null != result && null != logNum && null != gatherTime && null != cameraId && null != feature) {
                                        y = y + 1
                                        val calh = FaceFeatureVerifyDg.verify(record._4, 0, feature, 0)
                                        if (calh >= record._3) {
                                            pqueue.offer(Value(calh, new String(logNum), new String(cameraId), new String(gatherTime).toLong))
                                            if(pqueue.size()>record._2){
                                                pqueue.poll()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        loadTime = loadTime + mid - start
                    }
                }
            } catch {
                case e: Exception => log.error(e.getMessage, e)
            }
        }
        val computeTime: Long = System.currentTimeMillis() - start;
        log.info("===LOCAL TASK==date is [" + record._5 + "] , execute count : " + y + " ,==end queue size is "
          + pqueue.size + " ,==data source is " + dataSource + " ,==load data time is " + loadTime + " ms , ==compute time is " + computeTime + " ms")
        pqueue.toList
    }

    //执行split 操作  对查询条件分片
    def splitSearch(propCache: IgniteCache[String, Properties]): ListBuffer[(String, String, String)] = {
        val regions = ListBuffer[(String, String, String)]()
        val regionLocator = HbaseUtil.getConn.getRegionLocator(TableName.valueOf(propCache.get("SERVER").getProperty("face.table.name").getBytes))
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
            log.warn("Hbase regionServer is empty!....")
        }
        regions
    }
}

case class Value(i: (Float, String, String, Long)) extends Ordered[Value] {
    def compare(that: Value) =
        if (this.i._1 > that.i._1) 1
        else if (this.i._1 == that.i._1) 0 else -1
}
