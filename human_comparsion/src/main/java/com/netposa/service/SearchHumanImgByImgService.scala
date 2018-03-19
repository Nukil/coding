package com.netposa.service

import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util
import java.util.UUID

import com.netposa.bean.CacheFaceFeature
import com.netposa.main.HumanFeatureDataAnalyzeMain.{IGNITE_NAME, ignite, props}
import com.netposa.rpc.{SearchImgByImgInputRecord, SearchImgResult}
import com.netposa.util.{CalcHammingDistUtil, HbaseUtil, HumanFeatureVerifyDg}
import org.apache.hadoop.hbase.{HRegionLocation, TableName}
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import org.apache.ignite.{IgniteCache, Ignition}
import org.apache.ignite.cache.{CacheMode, CachePeekMode}
import org.apache.ignite.cluster.ClusterNode
import org.apache.ignite.compute.{ComputeJob, ComputeJobAdapter, ComputeJobResult, ComputeTaskAdapter}
import org.apache.ignite.configuration.CacheConfiguration
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConversions.{asScalaBuffer, seqAsJavaList, _}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by Administrator on 2016/10/28.
  */
object SearchHumanImgByImgService {
    val LOG: Logger = LoggerFactory.getLogger(SearchHumanImgByImgService.getClass)
    val ALGORITHM: String = props.getProperty("algorithm.type", "netposa")
    var DEBUG: Boolean = props.getProperty("service.process.debug.enable", "true").toBoolean
    val FAMILY: Array[Byte] = "cf".getBytes
    val CAMERAID: Array[Byte] = "cameraId".getBytes
    val JLBH: Array[Byte] = "logNum".getBytes
    val FEATURE: Array[Byte] = "feature".getBytes
    val GATHERTIME: Array[Byte] = "gatherTime".getBytes
    val COL_MAP: util.Map[String, Array[String]] = new util.HashMap[String, Array[String]]()
    COL_MAP.put("cf", List("logNum", "feature", "gatherTime", "cameraId").toArray)

    def executeQuery(record: SearchImgByImgInputRecord, dates: java.util.List[String]): java.util.List[SearchImgResult] = {
        val uuid = UUID.randomUUID().toString
        val cacheCfg = new CacheConfiguration[String, (String, Int, Int, Array[Byte])]("HUMAN_INPUT_RECORD")
        cacheCfg.setCacheMode(CacheMode.REPLICATED)
        ignite.getOrCreateCache[String, (String, Int, Int, Array[Byte])](cacheCfg)
        ignite.cache[String, (String, Int, Int, Array[Byte])]("HUMAN_INPUT_RECORD").put(uuid, (record.cameraId, record.getCount, record.getDistance, record.getFeature))
        runJobs(uuid, record.getCameraId, record.getCount, dates)
    }

    def runJobs(uuid: String, cameraId: String, count: Int, dates: java.util.List[String]): List[SearchImgResult] = {
        val time1 = System.currentTimeMillis()
        val compute = ignite.compute(ignite.cluster().forServers())
        val result = compute.execute[(String, String, java.util.List[String]), List[Value]](new FaceComputeTask(), (uuid, cameraId, dates))
        val time2 = System.currentTimeMillis()
        try {
            //featureCache:(count , distance  ,feature)
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
            //计算结束释放缓存空间
            ignite.cache[String, (String, Int, Int, Array[Byte])]("HUMAN_INPUT_RECORD").remove(uuid)
            if (DEBUG) {
                LOG.info("====job has been finished ==`time is " + (time2 - time1))
                LOG.info("====take result and ignite.remove.cache used `time is " + (System.currentTimeMillis() - time2))
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
private class FaceComputeTask extends ComputeTaskAdapter[(String, String, java.util.List[String]), List[Value]] with Serializable {
    override def map(subgrid: util.List[ClusterNode], record: (String, String, java.util.List[String])): util.Map[ComputeJob, ClusterNode] = {
        val result = new util.HashMap[ComputeJob, ClusterNode]()
        subgrid.foreach(node => {
            record._3.foreach { date =>
                val job = new HumanComputeJob((record._1, record._2, date), IGNITE_NAME)
                result.put(job, node)
            }
        })
        result
    }

    //合并计算结果
    override def reduce(results: util.List[ComputeJobResult]): List[Value] = {
        val result = new mutable.PriorityQueue[Value]()
        results.foreach(re => {
            val re2 = re.getData[List[Value]]
            re2.foreach(v => {
                result += v
            })
        })
        result.dequeueAll.toList
    }
}

/**
  * 并行在ignite集群节点上开始比对任务
  * record (uuid,cameraId,date)
  */
private class HumanComputeJob(record: (String, String, String), igniteName: String) extends ComputeJobAdapter with Serializable {
    val log: Logger = LoggerFactory.getLogger("HumanComputeJob")
    val FAMILY: Array[Byte] = "cf".getBytes
    val CAMERAID: Array[Byte] = "cameraId".getBytes
    val JLBH: Array[Byte] = "logNum".getBytes
    val FEATURE: Array[Byte] = "feature".getBytes
    val GATHERTIME: Array[Byte] = "gatherTime".getBytes
    val PROP_CACHE: IgniteCache[String, String] = Ignition.ignite(igniteName).cache(igniteName + "_HUMAN_GLOBAL")
    override def execute(): List[Value] = {
        val sdf: SimpleDateFormat = new SimpleDateFormat("yyyyMMdd")
        var dataSource: String = null
        var loadTime: Long = 0
        var computeTime: Long = 0
        //featureCache:(count , distance  ,feature)
        val inputRecord = Ignition.ignite(igniteName).cache[String, (String, Int, Int, Array[Byte])]("HUMAN_INPUT_RECORD").get(record._1)
        var pqueue = new mutable.PriorityQueue[Value]()
        var y = 0
        if (inputRecord == null) {
            log.warn("inputRecord is null")
            return pqueue.dequeueAll.toList
        }
        val time1 = System.currentTimeMillis()
        if (Ignition.ignite(igniteName).cacheNames().contains(record._3)) {
            dataSource = "Ignite"
            val re = Ignition.ignite(igniteName).cache[String, CacheFaceFeature](record._3).localEntries(CachePeekMode.PRIMARY)
            val time2 = System.currentTimeMillis()
            if ("netposa".equalsIgnoreCase(PROP_CACHE.get("ALGORITHM"))) {
                if (record._2 != null && !"".equals(record._2)) {
                    val cameraIds = record._2.split(",")
                    re.foreach { cacheFeature =>
                        y = y + 1
                        val calh = CalcHammingDistUtil.calcHammingDist(inputRecord._4, 0, cacheFeature.getValue.getFeature, 0)
                        if (calh >= inputRecord._3 && cameraIds.contains(new String(cacheFeature.getValue.getCameraId))) {
                            pqueue += (Value(calh, cacheFeature.getKey, new String(cacheFeature.getValue.getCameraId), new String(cacheFeature.getValue.getGatherTime).toLong))
                        }
                    }
                } else {
                    re.foreach { cacheFeature =>
                        y = y + 1
                        val calh = CalcHammingDistUtil.calcHammingDist(inputRecord._4, 0, cacheFeature.getValue.getFeature, 0)
                        if (calh >= inputRecord._3) {
                            pqueue += (Value(calh, cacheFeature.getKey, new String(cacheFeature.getValue.getCameraId), new String(cacheFeature.getValue.getGatherTime).toLong))
                        }
                    }
                }
            } else {
                if (record._2 != null && !"".equals(record._2)) {
                    val cameraIds = record._2.split(",")
                    re.foreach { cacheFeature =>
                        y = y + 1
                        val calh = HumanFeatureVerifyDg.verify(inputRecord._4, 0, cacheFeature.getValue.getFeature, 0)
                        if (calh >= inputRecord._3 && cameraIds.contains(new String(cacheFeature.getValue.getCameraId))) {
                            pqueue += (Value(calh, cacheFeature.getKey, new String(cacheFeature.getValue.getCameraId), new String(cacheFeature.getValue.getGatherTime).toLong))
                        }
                    }
                } else {
                    re.foreach { cacheFeature =>
                        y = y + 1
                        val calh = HumanFeatureVerifyDg.verify(inputRecord._4, 0, cacheFeature.getValue.getFeature, 0)
                        if (calh >= inputRecord._3) {
                            pqueue += (Value(calh, cacheFeature.getKey, new String(cacheFeature.getValue.getCameraId), new String(cacheFeature.getValue.getGatherTime).toLong))
                        }
                    }
                }
            }
            loadTime = time2 - time1
            computeTime = System.currentTimeMillis() - time2
            //log.info("ignite load data used times: " + (time2 - time1) + ",compute used times: " + (System.currentTimeMillis() - time2))
        } else {
            try {
                dataSource = "HBase"
                var size: Int = 0
                val conn = HbaseUtil.getConn
                var table: Table = conn.getTable(TableName.valueOf(PROP_CACHE.get("TABLENAME")))
                var re = scala.collection.mutable.Map[String, (Array[Byte], Array[Byte], Array[Byte])]()
                val hostRowKey = splitSearch(PROP_CACHE)
                val hostname = InetAddress.getLocalHost.getHostName

                hostRowKey.foreach { hrk =>
                    if (hrk._1.equals(hostname)) {
                        val millis: Long = sdf.parse(record._3.split("_")(2)).getTime
                        val startRow: String = hrk._2 + millis
                        val endRow: String = hrk._3 + (millis + 86400000)
                        val scan = new Scan
                        scan.setStartRow(startRow.getBytes)
                        scan.setStopRow(endRow.getBytes)
                        scan.setCaching(10000)
                        val scanner = table.getScanner(scan)
                        scanner.foreach { result =>
                            if (null != result
                              && null != result.getValue(FAMILY, JLBH)
                              && null != result.getValue(FAMILY, GATHERTIME)
                              && null != result.getValue(FAMILY, CAMERAID)
                              && null != result.getValue(FAMILY, FEATURE)) {
                                size += 1
                                re += new String(result.getValue(FAMILY, JLBH)) -> (result.getValue(FAMILY, GATHERTIME), result.getValue(FAMILY, CAMERAID), result.getValue(FAMILY, FEATURE))
                            }
                        }
                    }
                }
                val time2 = System.currentTimeMillis()
                if ("netposa".equalsIgnoreCase(PROP_CACHE.get("ALGORITHM"))) {
                    if (record._2 != null && !"".equals(record._2)) {
                        val cameraIds = record._2.split(",")
                        re.keys.foreach { key =>
                            y = y + 1
                            val calh = CalcHammingDistUtil.calcHammingDist(inputRecord._4, 0, re(key)._3, 0)
                            if (calh >= inputRecord._3 && cameraIds.contains(new String(re(key)._3))) {
                                pqueue += (Value(calh, key, new String(re(key)._2), new String(re(key)._1).toLong))
                            }
                        }
                    } else {
                        re.keys.foreach { key =>
                            y = y + 1
                            val calh = CalcHammingDistUtil.calcHammingDist(inputRecord._4, 0, re(key)._3, 0)
                            if (calh >= inputRecord._3) {
                                pqueue += (Value(calh, key, new String(re(key)._2), new String(re(key)._1).toLong))
                            }
                        }
                    }
                } else {
                    if (record._2 != null && !"".equals(record._2)) {
                        val cameraIds = record._2.split(",")
                        re.keys.foreach { key =>
                            y = y + 1
                            val calh = CalcHammingDistUtil.calcHammingDist(inputRecord._4, 0, re(key)._3, 0)
                            if (calh >= inputRecord._3 && cameraIds.contains(new String(re(key)._3))) {
                                pqueue += (Value(calh, key, new String(re(key)._2), new String(re(key)._1).toLong))
                            }
                        }
                    } else {
                        re.keys.foreach { key =>
                            y = y + 1
                            val calh = CalcHammingDistUtil.calcHammingDist(inputRecord._4, 0, re(key)._3, 0)
                            if (calh >= inputRecord._3) {
                                pqueue += (Value(calh, key, new String(re(key)._2), new String(re(key)._1).toLong))
                            }
                        }
                    }
                }
                loadTime = time2 - time1
                computeTime = System.currentTimeMillis() - time2
                //log.info("HBase load data used times: " + (time2 - time1) + ",compute used times: " + (System.currentTimeMillis() - time2))
            } catch {
                case e: Exception => log.error(e.getMessage, e)
            }
        }
        log.info("===LOCAL TASK==date is [" + record._3 + "] , execute count : " + y + " ,==end queue size is "
          + pqueue.size + " ,==data source is " + dataSource + " ,==load data time is " + loadTime + " ms , ==compute time is " + computeTime + " ms")
        pqueue.dequeueAll.toList.take(inputRecord._2)
    }

    //执行split 操作  对查询条件分片
    def splitSearch(propCache: IgniteCache[String, String]): ListBuffer[(String, String, String)] = {
        val regions = ListBuffer[(String, String, String)]()
        val regionLocator = HbaseUtil.getConn.getRegionLocator(TableName.valueOf(propCache.get("TABLENAME").getBytes))
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
