package com.netposa.poseidon.face.service

import com.netposa.poseidon.face.rpc.{FaceTrackingResult, SearchImgResult}

import scala.collection.JavaConversions._

object FaceTrackingService {
    def getResult(results: java.util.List[SearchImgResult]): java.util.List[FaceTrackingResult] = {
        val resultList = new java.util.ArrayList[FaceTrackingResult]
        val tmp = results.groupBy {
            x => x.getCameraId
        }
        println(tmp.toString())
        tmp.foreach(f => {
            val faceTrackingResult = new FaceTrackingResult
            faceTrackingResult.setCameraId(f._1)
            faceTrackingResult.setFaceImgs(f._2)
            faceTrackingResult.setOccurrencesCount(f._2.size)
            resultList.add(faceTrackingResult)
        })
        resultList
    }
}