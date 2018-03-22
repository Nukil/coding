package com.netposa.poseidon.fakecardetect.service

import com.netposa.poseidon.fakecardetect.bean.ResultRecord

import scala.collection.mutable.ArrayBuffer

class DataReduceService {

  def excutedResult(result1:ArrayBuffer[ResultRecord],result2:ArrayBuffer[ResultRecord]):ArrayBuffer[ResultRecord]={
    //聚合操作，根据Key分开计算,考虑入库的可能有多个，同一个key更新的就一条
    val finalRes:ArrayBuffer[ResultRecord] = ArrayBuffer[ResultRecord]()
    var temp:ResultRecord =null
    result1.foreach(r1=>{
//      println("r1==================>"+r1)
      if (r1!=null){
        result2.foreach(r2=>{
//          println("r2==================>"+r2)
          if (r1.getFlag()) {
            //是套牌需要入库
            finalRes += r1
          }
          if (r2!=null){
            if (r2.getFlag()) {
              //是套牌需要入库
              finalRes += r1
            }
            if (!r1.getFlag() && !r2.getFlag()) {
              //r1和r2都不是套牌，选择时间节点最新的数据
              if (r1.getFirstValue().getJgsj() > r2.getFirstValue().getJgsj()) {
                temp = r1
              } else {
                temp = r2
              }
            }
          }
        })
      }
    })
    finalRes+=temp
  }
}
