package com.netposa.poseidon.library.util

import java.nio.charset.Charset

import com.netposa.poseidon.library.bean.FaceFeature
import kafka.utils.Logging

class ParseBytesUtils extends Serializable with Logging {
  //===============需要解析的所有字段 声明==============================
  val JSON_START = "{".getBytes
  val JSON_END = "}".getBytes
  val JSON_STOP = ",".getBytes
  val EMPTY_CHAR = " ".getBytes
  val END_ARR = "]".getBytes
  val JSON_CHAR = ":".getBytes
  val JSON_MARK = "\"".getBytes

  val charset = Charset.forName("UTF-8")

  //pcc
  val _1_1 = "recordId".getBytes(charset);
  val _1_2 = "sourceId".getBytes(charset);
  val _1_3 = "absTime".getBytes(charset);

  /**
    * 在sour中查找target 的位置
    * 返回位置下标
    */
  def indexOf(sour: Array[Byte], target: Array[Byte]): Int = {
    if (target.length == 0) {
      return 0
    }
    if (sour.length < target.length) {
      return -1
    }
    var i = 0
    var j = 0
    var continue = false
    while (i < sour.length - target.length + 1 && !continue) {
      j = 0
      continue = true
      while (j < target.length && continue) {
        if (sour(i + j) != target(j)) {
          continue = false
        }
        j = j + 1
      }
      i = i + 1
    }
    if (continue) {
      i - 1
    } else {
      -1
    }
  }

  /**
    * 从sour中解析出 target的值
    * 返回类型Array[Byte]
    * index 在数据源中的起始位置
    */
  private def getValueToBytes(sour: Array[Byte], target: Array[Byte]): Array[Byte] = {
    var index = indexOf(sour, target)
    if (index < 0) {
      return ("".getBytes)
    }
    index += target.length
    //查找开始的位置
    var i = index
    var Next = true
    while (i < sour.length && Next) {
      if (sour(i) == JSON_CHAR(0) || sour(i) == EMPTY_CHAR(0) || sour(i) == JSON_MARK(0)) {
        i = i + 1
      } else {
        Next = false
      }
    }
    index = i
    //查找结束位置
    Next = true
    var tmpTuples = 0
    val startChar = sour(i)
    while (i < sour.length && Next) {
      if (startChar == JSON_START(0)) {
        //返回结构体
        if (sour(i) == JSON_START(0)) {
          tmpTuples += 1
        }
        if (sour(i) == JSON_END(0)) {
          tmpTuples -= 1
        }
        if (tmpTuples == 0) {
          Next = false
        }
        i += 1
      } else {
        if (sour(i) == JSON_END(0) || sour(i) == JSON_STOP(0) || sour(i) == JSON_MARK(0)) {
          Next = false
        } else {
          i += 1
        }
      }
    }
    val re = new Array[Byte](i - index)
    System.arraycopy(sour, index, re, 0, i - index)
    re
  }

  /**
    * 数据组装
    */
  def dataAssembleToPB(msg: Array[Byte], featureSize: Int): FaceFeature = {
    var remsg: FaceFeature = new FaceFeature()
    try {
      //(offset,recordId, cameraId, absTime)
      val data = fetcherData(msg)

      if (null != data) {
        if (data._1 >= 0 && msg.length >= data._1 + featureSize + 4) {
          val feature: Array[Byte] = new Array[Byte](msg.length - data._1 - 4)
          System.arraycopy(msg, data._1 + 4, feature, 0, msg.length - data._1 - 4 )
          remsg.setFeature(feature)
          remsg.setJlbh(data._2)
          remsg.setCameraId(data._3)
          remsg.setGatherTime(data._4)
        } else {
//          error("recordId=" + data._2 + ",cameraId=" + data._3 + ",absTime=" + data._4 + ",offset=" + data._1 + ",dataSize=" + featureSize + ", msg.length=" + msg.length + " this record has no feature ...")
//          error(new String(msg))
        }
      }
    } catch {
      case t: Throwable => {
        /*info(t.getStackTraceString)*/
        error("fetcher data exception", t);
      }
    }
    remsg
  }

  def dataForETL2PB(rowmessage: Array[Byte], featureSize: Int): FaceFeature = {
    if (rowmessage != null) {
      //数据组装
      dataAssembleToPB(rowmessage, featureSize)
    } else {
      warn("dataForETL2PB info is null")
      null
    }
  }

  def fetcherData(src: Array[Byte]): (Int, String, String, String) = {
    var offset: Int = HashAlgorithm.bytesToInt2(src,0);
    var recordId: String = ""
    var cameraId: String = ""
    var absTime: String = ""

    val jsonUtils = new ByteArrayJsonParseUtils

    // 解析第一级 recordId
    val recordId_list = jsonUtils.parseJsonBytesByKey(src, _1_1, 0, src.length)
    if (recordId_list != null && recordId_list.size() > 0) {
      val recordId_offsets = recordId_list.get(0)
      recordId = new String(src, recordId_offsets(0) + 1, recordId_offsets(1) - recordId_offsets(0) - 3)
    }

    // 解析第一级 cameraId
    val cameraId_list = jsonUtils.parseJsonBytesByKey(src, _1_2, 0, src.length)
    if (cameraId_list != null && cameraId_list.size() > 0) {
      val cameraId_offsets = cameraId_list.get(0)
      //      cameraId = new String(src, cameraId_offsets(0), cameraId_offsets(1) - cameraId_offsets(0) - 1)
      cameraId = new String(src, cameraId_offsets(0) + 1, cameraId_offsets(1) - cameraId_offsets(0) - 3)
    }

    // 解析第一级 absTime
    val absTime_list = jsonUtils.parseJsonBytesByKey(src, _1_3, 0, src.length)
    if (absTime_list != null && absTime_list.size() > 0) {
      val absTime_offsets = absTime_list.get(0)
      absTime = new String(src, absTime_offsets(0), absTime_offsets(1) - absTime_offsets(0)-1)
      absTime = absTime.replaceAll("-", "")
    }

    (offset, recordId, cameraId, absTime)
  }

}
