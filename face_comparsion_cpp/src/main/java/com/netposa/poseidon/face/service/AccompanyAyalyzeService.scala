package com.netposa.poseidon.face.service

import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.concurrent.{Callable, ExecutorService, Future}
import java.util.{ArrayList, Collections, PriorityQueue, Properties, UUID}

import com.netposa.HbaseUtil
import com.netposa.poseidon.face.bean.FaceFeature
import com.netposa.poseidon.face.rpc.outrpc.{AccompanyAyalyzeInputRecord, AccompanyAyalyzeResult, SearchImgByImgInputRecord}
import com.netposa.poseidon.face.util.{FaceFeatureVerify, LoadPropers}
import kafka.utils.Logging
import org.apache.hadoop.hbase.client.Connection
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HRegionLocation, TableName}

import scala.collection.JavaConversions.{asScalaBuffer, asScalaIterator, seqAsJavaList}

object AccompanyAyalyzeService extends Logging {
  val THREAD_NUM: Int = Runtime.getRuntime.availableProcessors()
  val COLS = new java.util.HashMap[String, Array[String]]
  val colsArray = Array("logNum", "gatherTime", "feature", "cameraId")
  COLS.put("cf", colsArray)
  val PROPERTIES: Properties = LoadPropers.getSingleInstance.getProperties("server")
  val TABLE_NAME: String = PROPERTIES.getProperty("hbase.table.name", "face_feature").trim()
  val DEBUG: Boolean = PROPERTIES.getProperty("service.process.debug.enable", "true").toBoolean
  val cameraIdType: String = PROPERTIES.getProperty("camera.id.type", "long")
  //去重阈值
  val distinct: Int = PROPERTIES.getProperty("accompany.face.distinct", "30").trim().toInt * 1000
  var CONN: Connection = _

  def executeQuery(record: AccompanyAyalyzeInputRecord, pools: ExecutorService): java.util.List[AccompanyAyalyzeResult] = {
    val time1 = System.currentTimeMillis()
    val searchFaceImgByImgInputRecord = new SearchImgByImgInputRecord(record.getStartTime, record.getEndTime, record.getCameraId, ByteBuffer.wrap(record.getFeature), record.getDistence, 50)

    //以图搜图
    val faceImgList = SearchFaceImgByImgService.executeQuery(searchFaceImgByImgInputRecord, UUID.randomUUID().toString)
    //排序
    val tmpList = faceImgList.sortWith((x, y) => { //按照cameraId排序，同一cameraId则按照采集时间排序
      if (!x.getCameraId.equals(y.getCameraId)) {
        x.getCameraId.compareTo(y.getCameraId) > 0
      } else {
        x.getGatherTime.compareTo(y.getGatherTime) > 0
      }
    }).toList
    //将重复数据进行标记
    for (x <- 0 until tmpList.size - 1) {
      val one = tmpList.apply(x)
      val two = tmpList.apply(x + 1)
      //同一个相机ID且采集时间相差不到5分钟则标记删除
      if (one.getCameraId.equals(two.getCameraId) && one.getGatherTime - two.getGatherTime <= distinct) {
        two.setScore(-1.0)
      }
    }
    //去重
    val recordList = tmpList.filter(p => p.getScore > 0).map { x =>
      (x.getCameraId, x.getGatherTime, x.getLogNum)
    }
    if (DEBUG) {
      info("SearchFaceImgByImg result count :%d".format(faceImgList.size()))
      info("The number of the photos after to heavy :%d".format(recordList.size()))
    }


    val time2 = System.currentTimeMillis()
    if (recordList != null && recordList.nonEmpty) {
      //找到所有与目标人物有伴随关系的人脸照片
      val faceFeatures = getFaceFeature(recordList, record, pools)
      val time3 = System.currentTimeMillis()
      val resultList = getResult(faceFeatures, record.getDistence, pools)
      val time4 = System.currentTimeMillis()
      if (DEBUG) {
        val faceCount = faceFeatures.map { x => x.size }
        info("faceCount is :%d".format(faceCount.sum))
        info("以图搜图+图片去重用时%d毫秒！".format(time2 - time1))
        info("找到所有有伴随关系的人脸照片用时%d毫秒！".format(time3 - time2))
        info("基于'中心元素'的人脸归类用时%d毫秒！".format(time4 - time3))
      }
      resultList.map(res => {
        val acc = new AccompanyAyalyzeResult
        acc.setLogNums(res)
        acc.setAccompanyCount(res.size)
      }).filter { x => x.getAccompanyCount >= record.getAccompanyCount }.sortBy(x => x.getAccompanyCount).reverse
    } else {
      new java.util.ArrayList[AccompanyAyalyzeResult]()
    }
  }

  /**
    * 查找所有有伴随行为的区域人脸
    * arg1:人脸信息集合(cameraId,gatherTime,logNum)
    * arg2:伴随人员分析查询条件
    */
  def getFaceFeature(faceImgList: java.util.List[(String, Long, String)], record: AccompanyAyalyzeInputRecord, pools: ExecutorService): List[List[FaceFeature]] = {
    val executes = new java.util.ArrayList[Future[PriorityQueue[FaceFeature]]]()
    while (CONN == null || CONN.isClosed) {
      CONN = HbaseUtil.getConn
      if (CONN == null || CONN.isClosed) {
        error("hbase conn is null")
        Thread.sleep(2000)
      }
    }
    val regionLocator = CONN.getRegionLocator(TableName.valueOf(TABLE_NAME.getBytes))
    if (!regionLocator.getAllRegionLocations.isEmpty) {
      faceImgList.foreach { faceImg =>
        val cameraId = cameraIdType.equalsIgnoreCase("long") match {
          case true => "%010d".format(faceImg._1.toLong)
          case _ => faceImg._1
        }
        val startTime = faceImg._2 - (record.getAccompanyTime * 1000)
        val endTime = faceImg._2 + (record.getAccompanyTime * 1000)
        regionLocator.getAllRegionLocations.toArray(Array[HRegionLocation]()).foreach(region => {
          val key = Bytes.toString(region.getRegionInfo.getStartKey)
          val startKey = "".equals(key) match {
            case false => key
            case _ => "00"
          }
          val future = pools.submit(new Callable[PriorityQueue[FaceFeature]]() {
            override def call(): PriorityQueue[FaceFeature] = {
              val queue = new PriorityQueue[FaceFeature]()
              val results = HbaseUtil.getRows(TABLE_NAME, "%s%d".format(startKey, startTime), "%s%d".format(startKey, endTime), COLS, HbaseUtil.eqFilter("cf", "cameraId", cameraId.getBytes))
              results.map { res =>
                val faceFeature = new FaceFeature
                faceFeature.setJlbh(new String(res.getValue("cf".getBytes(), "logNum".getBytes())))
                faceFeature.setCameraId(faceImg._1)
                faceFeature.setFeature(res.getValue("cf".getBytes(), "feature".getBytes()))
                faceFeature.setGatherTime(new String(res.getValue("cf".getBytes(), "gatherTime".getBytes())))
                faceFeature
              }.filter { x => x != null && !x.getJlbh.equals(faceImg._3) }.foreach { x => queue.offer(x) }
              queue
            }
          })
          executes.add(future)
        })
      }
    }
    executes.map { x => x.get.iterator().toList }.toList
  }

  /**
    * 基于"中心元素"对人脸归类
    * arg1:按区域分类的人脸照片集合
    * arg2:相似度阈值
    */
  def getResult(faceFeatures: List[List[FaceFeature]], distence: Int, pools: ExecutorService): List[List[String]] = {
    val time1 = System.currentTimeMillis()
    var map = new java.util.concurrent.ConcurrentHashMap[(String, String), Float]
    var matrix = Collections.synchronizedList(new ArrayList[(String, Int, Float)])
    var resultList = new ArrayList[ArrayList[(String, Int)]]
    //        for (i <- 0 until (faceFeatures.size - 1)) {
    //            val srcPartOne = faceFeatures.get(i)
    //            val split = srcPartOne.size / THREAD_NUM
    //            val futures: java.util.List[Future[Boolean]] = new ArrayList[Future[Boolean]]()
    //            for (x <- 0 until THREAD_NUM) {
    //                val partOne = x == (THREAD_NUM - 1) match {
    //                    case true => srcPartOne.slice(x * split, srcPartOne.size)
    //                    case _ => srcPartOne.slice(x * split, (x + 1) * split)
    //                }
    //                val future = pools.submit(new Callable[Boolean]() {
    //                    override def call(): Boolean = {
    //                        for (j <- (i + 1) until faceFeatures.size) {
    //                            val partTwo = faceFeatures.get(j)
    //                            partOne.foreach { one =>
    //                                partTwo.foreach { two =>
    //                                    val score = FaceFeatureVerify.verify(one.getFeature, 0, two.getFeature, 0)
    //                                    if (score >= distence) {
    //                                        matrix.add((one.getJlbh, i, score))
    //                                        matrix.add((two.getJlbh, j, score))
    //                                        map.put((one.getJlbh, two.getJlbh), score)
    //                                    }
    //                                }
    //                            }
    //                        }
    //                        true
    //                    }
    //                })
    //                futures.add(future)
    //            }
    //            futures.foreach { x => x.get }
    //        }
    val futures: java.util.List[Future[Boolean]] = new ArrayList[Future[Boolean]]()

    for (i <- 0 until (faceFeatures.size - 1)) {
      val srcPartOne = faceFeatures.get(i)
      for (j <- (i + 1) until faceFeatures.size) {
        val future = pools.submit(new Callable[Boolean]() {
          override def call(): Boolean = {
              val partTwo = faceFeatures.get(j)
              srcPartOne.foreach { one =>
                partTwo.foreach { two =>
                  val score = FaceFeatureVerify.verify(one.getFeature, 0, two.getFeature, 0)
                  if (score >= distence) {
                    matrix.add((one.getJlbh, i, score))
                    matrix.add((two.getJlbh, j, score))
                    map.put((one.getJlbh, two.getJlbh), score)
                  }
                }
              }
            true
          }
        })
        futures.add(future)
      }
    }
    futures.foreach { x => x.get }
    val time5 = System.currentTimeMillis()
    info("计算图片两两相似度并存储用时%d毫秒！".format(time5 - time1))
    val weightList = matrix.groupBy(f => f._1).map(x => (x._1, x._2.map(z => (z._2, z._3)))).map(f => {
      (f._1, f._2.reduce((x, y) => (x._1, x._2 + y._2)))
    }).toList
    val seqLikeList = weightList.toList.sortBy(f => f._2._2).reverse
    val time2 = System.currentTimeMillis()
    info("将数据以'权重'排序用时%d毫秒！".format(time2 - time5))
    var list = faceFeatures.flatMap { x => x.map { f => f.getJlbh } }
    seqLikeList.foreach(f => {
      resultList = insertList(resultList, f, map, distence)
    })
    list = list.filter(x => {
      !(seqLikeList.map(f => f._1).contains(x))
    })
    list.foreach { x =>
      var temp = new ArrayList[(String, Int)]
      temp.add((x, 0))
      resultList.add(temp)
    }
    val time3 = System.currentTimeMillis()
    info("遍历归类用时%d毫秒！".format(time3 - time2))
    resultList.map(f => {
      f.map(x => {
        x._1
      }).toList
    }).toList
  }

  /**
    * 将元素逐个插入到类别中
    * list 已创建好的类别集合
    * element 待插入的元素
    * map 存储两张照片分数的容器，key为两张照片的ID，value为比对分数
    * distence 相似度阈值
    */
  def insertList(list: ArrayList[ArrayList[(String, Int)]], element: (String, (Int, Float)), map: java.util.concurrent.ConcurrentHashMap[(String, String), Float], distence: Int): ArrayList[ArrayList[(String, Int)]] = {
    var resultList = new ArrayList[ArrayList[(String, Int)]]
    resultList.addAll(list)
    val logNum = element._1
    if (resultList.size == 0 || element._2._2 < 50) { //如果目标类别个数为0或者待插入元素分数为0，则直接创建类别
      var temp = new ArrayList[(String, Int)]
      temp.add((logNum, element._2._1))
      resultList.add(temp)
      resultList
    } else { //类别个数不为0，开始比对
      val size = resultList.size
      var srcflag = true
      for (i <- 0 until size) {
        if (srcflag) {
          var category = resultList.get(i)
          val centerElement = category.get(0) //拿到该类别的第一个元素（中心元素）
          if (element._2._1 != centerElement._2) { //如果待插入元素和中心元素类别相同，则直接跳过
            val score = map.containsKey(logNum, centerElement._1) match {
              case true => map.get(logNum, centerElement._1)
              case _ => map.containsKey(centerElement._1, logNum) match {
                case true => map.get(centerElement._1, logNum)
                case _ => 0.0
              }
            } //得到待插入元素和目标类别中心元素的相似度
            if (score >= distence) { //如果与中心元素相似度大于阈值
              var flag = true
              var old: (String, Int) = null
              category.foreach(re => {
                if (element._2._1 == re._2) {
                  flag = false
                  old = re
                }
              })
              if (flag) { //类别内没有同一区域元素，直接添加
                category.add((logNum, element._2._1))
                srcflag = false
              } else { //类别内有同一区域元素
                val oldScore = map.containsKey(centerElement._1, old._1) match {
                  case true => map.get(centerElement._1, old._1)
                  case _ => map.containsKey(old._1, centerElement._1) match {
                    case true => map.get(old._1, centerElement._1)
                    case _ => 0.0f
                  }
                } //拿出其中同一区域的元素和中心元素的相似度
                if (score > oldScore) { //若待插入的元素与中心元素相似度大于已存在的同区域元素和中心元素的相似度，则插入待插入元素，删除原有的同区域元素。并将删除的元素重新做循环
                  category.add((logNum, element._2._1))
                  category.remove(old)
                  resultList = insertList(resultList, (old._1, (old._2, oldScore)), map, distence)
                  srcflag = false
                }
              }
            }
          }
          //循环到最后一个类别，元素依然没有被归类，则自建一类
          if (srcflag && i == size - 1) {
            var temp = new ArrayList[(String, Int)]
            temp.add((logNum, element._2._1))
            resultList.add(temp)
          }
        }
      }
      resultList
    }
  }
}