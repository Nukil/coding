package com.netposa.poseidon.face.service

import java.util.concurrent.{Callable, ExecutorService, Future}
import java.util.{Collections, PriorityQueue, Properties}

import com.netposa.HbaseUtil
import com.netposa.poseidon.face.bean.FaceFeature
import com.netposa.poseidon.face.rpc.outrpc.FrequentPassInputRecord
import com.netposa.poseidon.face.util.{FaceFeatureVerify, LoadPropers}
import kafka.utils.Logging
import org.apache.hadoop.hbase.client.Connection
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HRegionLocation, TableName}

import scala.collection.JavaConversions.asScalaBuffer
import scala.util.control.Breaks

object FrequentPassService extends Logging {
    val properties: Properties = LoadPropers.getSingleInstance.getProperties("server")
    val cameraIdType: String = properties.getProperty("camera.id.type", "long")
    val tableName: String = properties.getProperty("hbase.table.name", "face_feature")
    var CONN: Connection = _

    def executeQuery(record: FrequentPassInputRecord, pools: ExecutorService): java.util.ArrayList[java.util.ArrayList[String]] = {
        while (CONN == null || CONN.isClosed) {
            CONN = HbaseUtil.getConn
            if (CONN == null || CONN.isClosed) {
                error("hbase conn is null")
                Thread.sleep(2000)
            }
        }
        val regionLocator = CONN.getRegionLocator(TableName.valueOf(tableName.getBytes))
        if (!regionLocator.getAllRegionLocations.isEmpty) {
            val startTime = record.getStartTime
            val endTime = record.getEndTime
            val cameraids = record.getCameraId.split(",")
            val faceList = cameraids.map { x =>
                val executes = new java.util.ArrayList[Future[PriorityQueue[FaceFeature]]]()
                val cameraId = cameraIdType.equalsIgnoreCase("long") match {
                    case true => "%010d".format(x.toLong)
                    case _ => x
                }
                regionLocator.getAllRegionLocations.toArray(Array[HRegionLocation]()).foreach(region => {
                    val future = pools.submit(new Callable[PriorityQueue[FaceFeature]]() {
                        override def call(): PriorityQueue[FaceFeature] = {
                            val queue = new PriorityQueue[FaceFeature]()
                            val key = Bytes.toString(region.getRegionInfo.getStartKey)
                            val startKey = "".equals(key) match {
                                case false => key
                                case _ => "00"
                            }
                            val results = HbaseUtil.getRows(tableName, "%s%d".format(startKey, startTime), "%s%d".format(startKey, endTime))
                            results.map { res =>
                                if (cameraId.equals(new String(res.getValue("cf".getBytes(), "cameraId".getBytes())))) {
                                    val faceFeature = new FaceFeature
                                    faceFeature.setJlbh(new String(res.getValue("cf".getBytes(), "logNum".getBytes())))
                                    faceFeature.setCameraId(cameraId)
                                    faceFeature.setFeature(res.getValue("cf".getBytes(), "feature".getBytes()))
                                    faceFeature.setGatherTime(new String(res.getValue("cf".getBytes(), "gatherTime".getBytes())))
                                    faceFeature
                                } else {
                                    null
                                }
                            }.filter(_ != null).foreach { x =>
                                queue.offer(x)
                            }
                            queue
                        }
                    })
                    executes.add(future)
                })
                executes.map { x => x.get.toArray(Array[FaceFeature]()) }.flatMap { x => x }.toList
            }.flatMap { x => x }.toList
            getResult(faceList, record.getDistence, pools)
        } else {
            new java.util.ArrayList[java.util.ArrayList[String]]
        }
    }

    def getResult(faceFeatures: List[FaceFeature], distence: Int, pools: ExecutorService): java.util.ArrayList[java.util.ArrayList[String]] = {
        val threadNum = Runtime.getRuntime.availableProcessors()
        var map = new java.util.concurrent.ConcurrentHashMap[(String, String), Float]
        var matrix = Collections.synchronizedList(new java.util.ArrayList[(String, Float)])
        var resultList = new java.util.ArrayList[java.util.ArrayList[String]]
        val futures: java.util.List[Future[Boolean]] = new java.util.ArrayList[Future[Boolean]]()
        val split = faceFeatures.size / threadNum
        for (x <- 0 until threadNum) {
            val partOne = x == (threadNum - 1) match {
                case true => faceFeatures.slice(x * split, faceFeatures.size)
                case _ => faceFeatures.slice(x * split, (x + 1) * split)
            }
            val future = pools.submit(new Callable[Boolean]() {
                override def call(): Boolean = {
                    partOne.foreach { one =>
                        faceFeatures.foreach { two =>
                            val score = FaceFeatureVerify.verify(one.getFeature, 0, two.getFeature, 0)
                            if (score >= distence) {
                                matrix.add((one.getJlbh, score))
                                matrix.add((two.getJlbh, score))
                                map.put((one.getJlbh, two.getJlbh), score)
                            }
                        }
                    }
                    true
                }
            })
            futures.add(future)
        }
        futures.foreach { x => x.get }
        val weightList = matrix.groupBy(f => f._1).map(x => (x._1, x._2.map(z => z._2))).map(f => {
            (f._1, f._2.reduce((x, y) => (x + y)))
        }).toList
        val seqLikeList = weightList.sortBy(f => f._2).reverse
        seqLikeList.foreach(f => {
            resultList = insertList(resultList, f, map, distence)
        })
        val list = faceFeatures.map(f => f.getJlbh)
        val seq = seqLikeList.map(f => f._1)
        val tmp = list.filter { x =>
            !(seq.contains(x))
        }
        tmp.map { x =>
            var temp = new java.util.ArrayList[String]
            temp.add(x)
            resultList.add(temp)
        }
        resultList
    }

    //将元素逐个插入到类别中
    def insertList(list: java.util.ArrayList[java.util.ArrayList[String]], element: (String, Float),
                   map: java.util.concurrent.ConcurrentHashMap[(String, String), Float], distence: Int): java.util.ArrayList[java.util.ArrayList[String]] = {
        var resultList = new java.util.ArrayList[java.util.ArrayList[String]]
        resultList.addAll(list)
        val loop = new Breaks
        if (resultList.size == 0 || element._2 < 50) { //如果目标类别个数为0或与任何元素相似度都小于阈值，则直接创建类别
            var temp = new java.util.ArrayList[String]
            temp.add(element._1)
            resultList.add(temp)
            resultList
        } else { //类别个数不为0，开始比对
            val size = resultList.size
            loop.breakable {
                for (i <- 0 until size) {
                    var category = resultList.get(i)
                    val centerElement = category.get(0) //拿到该类别的第一个元素（中心元素）
                    val score = map.containsKey(element._1, centerElement) match {
                        case true => map.get(element._1, centerElement)
                        case _ => map.containsKey(centerElement, element._1) match {
                            case true => map.get(centerElement, element._1)
                            case _ => 0.0
                        }
                    } //得到待插入元素和目标类别中心元素的相似度
                    if (score >= distence) { //如果与中心元素相似度大于阈值
                        category.add(element._1)
                        loop.break
                    }
                    if (i == size - 1) {
                        var temp = new java.util.ArrayList[String]
                        temp.add(element._1)
                        resultList.add(temp)
                    }
                }
            }
            resultList
        }
    }
}