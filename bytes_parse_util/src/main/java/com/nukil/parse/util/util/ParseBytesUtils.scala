package com.nukil.parse.util.util

import java.nio.charset.Charset

import com.nukil.parse.util.bean._
import org.apache.log4j.Logger

class ParseBytesUtils extends Serializable {
    val logger: Logger = Logger.getLogger(this.getClass)
    //===============需要解析的所有字段 声明==============================
    val JSON_START: Array[Byte] = "{".getBytes
    val JSON_END: Array[Byte] = "}".getBytes
    val JSON_STOP: Array[Byte] = ",".getBytes
    val EMPTY_CHAR: Array[Byte] = " ".getBytes
    val END_ARR: Array[Byte] = "]".getBytes
    val JSON_CHAR: Array[Byte] = ":".getBytes
    val JSON_MARK: Array[Byte] = "\"".getBytes

    val charset: Charset = Charset.forName("UTF-8")

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
            return "".getBytes
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
      * 人脸数据组装
      */
    private def faceDataAssembleToPB(msg: Array[Byte], featureSize: Int, params: java.util.List[FaceParams]): FaceBean = {
        val data: (Int, FaceBean) = faceFetcherData(msg, params)
        if (data._2.getrCode() != StatusCode.ERROR_ANALYZE) {
            try {
                if (null != data) {
                    if (data._1 >= 0 && msg.length >= data._1 + featureSize + 4) {
                        val feature: Array[Byte] = new Array[Byte](msg.length - data._1 - 4)
                        System.arraycopy(msg, data._1 + 4, feature, 0, msg.length - data._1 - 4)
                        data._2.setFeature(feature)
                        data._2.setrCode(StatusCode.OK)
                    } else {
                        data._2.setrCode(StatusCode.ERROR_ANALYZE)
                        data._2.setrMessage("feature size error, msg size is %d, offset is %d, input feature size is %d".format(msg.length, data._1, featureSize))
                    }
                }
            } catch {
                case e: Throwable => {
                    data._2.setrCode(StatusCode.ERROR_OTHER)
                    data._2.setrMessage(e.getLocalizedMessage)
                }
            }
        }
        data._2
    }

    def faceDataForETL2PB(rowmessage: Array[Byte], featureSize: Int, params: java.util.List[FaceParams]): FaceBean = {
        if (rowmessage != null) {
            //数据组装
            faceDataAssembleToPB(rowmessage, featureSize, params)
        } else {
            logger.warn("source message is null")
            null
        }
    }

    private def faceFetcherData(src: Array[Byte], params: java.util.List[FaceParams]): (Int, FaceBean) = {
        var offset: Int = 0
        var faceBean: FaceBean = new FaceBean
        val jsonUtils = new ByteArrayJsonParseUtils

        try {
            offset = HashAlgorithm.bytesToInt2(src, 0)

            // 解析第一级 recordId
            if (params.contains(FaceParams.RECORD_ID)) {
                val recordId_list = jsonUtils.parseJsonBytesByKey(src, FaceParams.RECORD_ID.getValue.getBytes(charset), 0, src.length)
                if (recordId_list != null && recordId_list.size() > 0) {
                    val recordId_offsets = recordId_list.get(0)
                    faceBean.setRecordId(new String(src, recordId_offsets(0) + 1, recordId_offsets(1) - recordId_offsets(0) - 3))
                }
            }

            // 解析第一级 absTime
            if (params.contains(FaceParams.ABS_TIME)) {
                val absTime_list = jsonUtils.parseJsonBytesByKey(src, FaceParams.ABS_TIME.getValue.getBytes(charset), 0, src.length)
                if (absTime_list != null && absTime_list.size() > 0) {
                    val absTime_offsets = absTime_list.get(0)
                    faceBean.setAbsTime(new String(src, absTime_offsets(0), absTime_offsets(1) - absTime_offsets(0) - 1).toLong)
                }
            }

            // 解析第一级 pushTime
            if (params.contains(FaceParams.PUSH_TIME)) {
                val pushTime_list = jsonUtils.parseJsonBytesByKey(src, FaceParams.PUSH_TIME.getValue.getBytes(charset), 0, src.length)
                if (pushTime_list != null && pushTime_list.size() > 0) {
                    val pushTime_offsets = pushTime_list.get(0)
                    faceBean.setPushTime(new String(src, pushTime_offsets(0), pushTime_offsets(1) - pushTime_offsets(0) - 1).toLong)
                }
            }

            // 解析第一级 sourceType
            if (params.contains(FaceParams.SOURCE_TYPE)) {
                val SouceType_list = jsonUtils.parseJsonBytesByKey(src, FaceParams.SOURCE_TYPE.getValue.getBytes(charset), 0, src.length)
                if (SouceType_list != null && SouceType_list.size() > 0) {
                    val SourceType_offsets = SouceType_list.get(0)
                    faceBean.setSourceType(new String(src, SourceType_offsets(0), SourceType_offsets(1) - SourceType_offsets(0) - 1).toInt)
                }
            }

            // 解析第一级 sourceId
            if (params.contains(FaceParams.SOURCE_ID)) {
                val cameraId_list = jsonUtils.parseJsonBytesByKey(src, FaceParams.SOURCE_ID.getValue.getBytes(charset), 0, src.length)
                if (cameraId_list != null && cameraId_list.size() > 0) {
                    val cameraId_offsets = cameraId_list.get(0)
                    faceBean.setSourceId(new String(src, cameraId_offsets(0) + 1, cameraId_offsets(1) - cameraId_offsets(0) - 3))
                }
            }

            // 解析第一级 featuresInfo
            if (params.contains(FaceParams.FEATURES_INFO)) {
                val featuresInfo_list = jsonUtils.parseJsonBytesByKey(src, FaceParams.FEATURES_INFO.getValue.getBytes(charset), 0, src.length)
                if (featuresInfo_list != null && featuresInfo_list.size() > 0) {
                    val featuresInfo_offsets = featuresInfo_list.get(0)
                    faceBean.setFeaturesInfo(new String(src, featuresInfo_offsets(0) + 1, featuresInfo_offsets(1) - featuresInfo_offsets(0) - 3))
                }
            }
        } catch {
            case e: Exception => {
                faceBean.setrCode(StatusCode.ERROR_ANALYZE)
                faceBean.setrMessage(e.getLocalizedMessage)
            }
        }
        (offset, faceBean)
    }

    /**
      * 人体数据组装
      */
    private def humanDataAssembleToPB(msg: Array[Byte], featureSize: Int, params: java.util.List[HumanParams]): HumanBean = {
        val data: (Int, HumanBean) = humanFetcherData(msg, params)
        if (data._2.getrCode() != StatusCode.ERROR_ANALYZE) {
            try {
                if (null != data) {
                    if (data._1 >= 0 && msg.length >= data._1 + featureSize + 4) {
                        val feature: Array[Byte] = new Array[Byte](msg.length - data._1 - 4)
                        System.arraycopy(msg, data._1 + 4, feature, 0, msg.length - data._1 - 4)
                        data._2.setFeature(feature)
                        data._2.setrCode(StatusCode.OK)
                    } else {
                        data._2.setrCode(StatusCode.ERROR_ANALYZE)
                        data._2.setrMessage("feature size error, msg size is %d, offset is %d, input feature size is %d".format(msg.length, data._1, featureSize))
                    }
                }
            } catch {
                case e: Throwable => {
                    data._2.setrCode(StatusCode.ERROR_OTHER)
                    data._2.setrMessage(e.getLocalizedMessage)
                }
            }
        }
        data._2
    }

    def humanDataForETL2PB(rowmessage: Array[Byte], featureSize: Int, params: java.util.List[HumanParams]): HumanBean = {
        if (rowmessage != null) {
            //数据组装
            humanDataAssembleToPB(rowmessage, featureSize, params)
        } else {
            logger.warn("source message is null")
            null
        }
    }

    private def humanFetcherData(src: Array[Byte], params: java.util.List[HumanParams]): (Int, HumanBean) = {
        var offset: Int = 0
        var humanBean: HumanBean = new HumanBean
        val jsonUtils = new ByteArrayJsonParseUtils

        try {
            offset = HashAlgorithm.bytesToInt2(src, 0)

            // 解析第一级 recordId
            if (params.contains(HumanParams.RECORD_ID)) {
                val recordId_list = jsonUtils.parseJsonBytesByKey(src, HumanParams.RECORD_ID.getValue.getBytes(charset), 0, src.length)
                if (recordId_list != null && recordId_list.size() > 0) {
                    val recordId_offsets = recordId_list.get(0)
                    humanBean.setRecordId(new String(src, recordId_offsets(0) + 1, recordId_offsets(1) - recordId_offsets(0) - 3))
                }
            }

            // 解析第一级 absTime
            if (params.contains(HumanParams.ABS_TIME)) {
                val absTime_list = jsonUtils.parseJsonBytesByKey(src, HumanParams.ABS_TIME.getValue.getBytes(charset), 0, src.length)
                if (absTime_list != null && absTime_list.size() > 0) {
                    val absTime_offsets = absTime_list.get(0)
                    humanBean.setAbsTime(new String(src, absTime_offsets(0), absTime_offsets(1) - absTime_offsets(0) - 1).toLong)
                }
            }

            // 解析第一级 pushTime
            if (params.contains(HumanParams.PUSH_TIME)) {
                val pushTime_list = jsonUtils.parseJsonBytesByKey(src, HumanParams.PUSH_TIME.getValue.getBytes(charset), 0, src.length)
                if (pushTime_list != null && pushTime_list.size() > 0) {
                    val pushTime_offsets = pushTime_list.get(0)
                    humanBean.setPushTime(new String(src, pushTime_offsets(0), pushTime_offsets(1) - pushTime_offsets(0) - 1).toLong)
                }
            }

            // 解析第一级 sourceType
            if (params.contains(HumanParams.SOURCE_TYPE)) {
                val SouceType_list = jsonUtils.parseJsonBytesByKey(src, HumanParams.SOURCE_TYPE.getValue.getBytes(charset), 0, src.length)
                if (SouceType_list != null && SouceType_list.size() > 0) {
                    val SourceType_offsets = SouceType_list.get(0)
                    humanBean.setSourceType(new String(src, SourceType_offsets(0), SourceType_offsets(1) - SourceType_offsets(0) - 1).toInt)
                }
            }

            // 解析第一级 sourceId
            if (params.contains(HumanParams.SOURCE_ID)) {
                val cameraId_list = jsonUtils.parseJsonBytesByKey(src, HumanParams.SOURCE_ID.getValue.getBytes(charset), 0, src.length)
                if (cameraId_list != null && cameraId_list.size() > 0) {
                    val cameraId_offsets = cameraId_list.get(0)
                    humanBean.setSourceId(new String(src, cameraId_offsets(0) + 1, cameraId_offsets(1) - cameraId_offsets(0) - 3))
                }
            }

            // 解析第一级 featuresInfo
            if (params.contains(HumanParams.FEATURES_INFO)) {
                val featuresInfo_list = jsonUtils.parseJsonBytesByKey(src, HumanParams.FEATURES_INFO.getValue.getBytes(charset), 0, src.length)
                if (featuresInfo_list != null && featuresInfo_list.size() > 0) {
                    val featuresInfo_offsets = featuresInfo_list.get(0)
                    humanBean.setFeaturesInfo(new String(src, featuresInfo_offsets(0) + 1, featuresInfo_offsets(1) - featuresInfo_offsets(0) - 3))
                }
            }
        } catch {
            case e: Exception => {
                humanBean.setrCode(StatusCode.ERROR_ANALYZE)
                humanBean.setrMessage(e.getLocalizedMessage)
            }
        }
        (offset, humanBean)
    }
}
