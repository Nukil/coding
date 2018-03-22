package com.netposa.poseidon.fakecardetect.service

import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap

import com.netposa.poseidon.fakecardetect.bean.{ResultRecord, ValueRecord}
import com.netposa.poseidon.fakecardetect.main.FakeCarDetectMainServer.{LOG, propConf}
import com.netposa.poseidon.fakecardetect.utils.FeatureVertifyUtil

import scala.collection.mutable.ArrayBuffer

class CompareService {
  val charset = Charset.forName("UTF-8")

  def compare(newmsg:(String,ResultRecord),oldmsg:(String,ResultRecord)):(String,ArrayBuffer[ResultRecord] )={
    //进行套牌对比的函数，返回对比完的结果以及，一个标志位，True表示套牌车最终需要入库，不会更新Map，False表示非套牌，需要更新全局Map
    if (newmsg._2==null ||newmsg._1==null ){
//      LOG.error("ReusltRecord is null,return")
      return (null,null)
    }else{
      val key = newmsg._1
      val value = newmsg._2.getFirstValue()
      val bufferResult:ArrayBuffer[ResultRecord] = ArrayBuffer[ResultRecord]()
//      //判断全局Map中有无本条信息系
      if( value != null){
//        if(allInfoMap.containsKey(msg._1)){
//          //已经包含一条数据，进行套牌对比
          val oldValue:ValueRecord = oldmsg._2.getFirstValue()
          //根据配置文件选择不同策略
          propConf.compare_strategy match {
            case 0 =>
              //先比结构化数据，不满足再比特征
              if(!compareStruct(key,oldValue,value) && !compareFeatureBuffer(key,oldValue, value)){
                //结构化数据不同，特征也不同，是套牌车,需要入库
                LOG.info("compare strategy is 0 and current data is Fakecar record and the record is"+key.toString)
                return (key ,bufferResult+=new ResultRecord(key,oldValue,value,true))
              }else{
                //不是套牌车，是同一辆，需要更新Map
                return (key ,bufferResult+=new ResultRecord(key,value,false) )
              }
            case 1 =>
              //结构化数据不同与特征不同满足一项
              if(!compareStruct(key,oldValue,value) || !compareFeatureBuffer(key,oldValue, value)){
                //结构化数据不同，或者特征也不同，是套牌车,需要入库
                LOG.info("compare strategy is 1 and current data is Fakecar record and the record is"+key.toString)
                return (key,bufferResult+=new ResultRecord(key,oldValue,value,true))
              }else{
                //不是套牌车，是同一辆，需要更新Map
                return (key ,bufferResult+=new ResultRecord(key,value,false))
              }
            case 2 =>
              //只对比结构化数据
              if(!compareStruct(key,oldValue,value)){
                //结构化数据不同是套牌车,需要入库
                LOG.info("compare strategy is 2 and current data is Fakecar record and the record is"+key.toString)

                val s = new ResultRecord(key,oldValue,value,true)
                return (key,bufferResult+=new ResultRecord(key,oldValue,value,true))
              }else{
                //不是套牌车，是同一辆，需要更新Map
                return (key, bufferResult+=new ResultRecord(key,value,null,false))
              }
            case 3 =>
              //只对比特征数据
              if(!compareFeatureBuffer(key,oldValue,value)){
                //特征数据不同是套牌车,需要入库
                LOG.info("compare strategy is 3 and current data is Fakecar record and the record is"+key.toString)
                return (key, bufferResult+=new ResultRecord(key,oldValue,value,true))
              }else{
                //不是套牌车，是同一辆，需要更新Map
                return (key, bufferResult+=new ResultRecord(key,value,false))
              }
            case _ =>
          }
        }
//    else{
//          //是新数据，没有对应的Key，加入Map
//          if(propConf.plate_similarity_threshold >= 0 && value.getOld_licence_plate()!= null){
//            //对比车牌相似度
//            val keyArr:Array[String] = key.split("-")
//            val similarity:Float = levenshtein(keyArr(0).getBytes(charset),value.getOld_licence_plate().getBytes(charset))
//            if(similarity<propConf.plate_similarity_threshold){
//              //小于阈值不加入Map
//              LOG.info("plate similarity less than threshold ,similarity value is %f".format(similarity))

//              //相似度大于阈值,需要更新，返回结果
//              LOG.info("plate similarity equal or over than threshold ,similarity value is %f".format(similarity))
//              return (//            }else{key, bufferResult+=new ResultRecord(key,value,false))
//            }
//          }else{
//            //不对比车牌相似度，需要更新，返回结果
//            allInfoMap.put(key,value)
//            LOG.info("Add a new vehicle to map in plate_similarity_compare, key is %s ".format(key.toString)+"and now mapSize is %s".format(allInfoMap.size()))
//            return (key, bufferResult+=new ResultRecord(key,value,false))
//          }
//        }
//      }
    }
    (null,null)
  }

  /**
    * 比较结构化信息,返回true表示结构化信息相同,flase表示结构化信息不相同
    * @param oldValue
    * @param newValue
    * @return Boolean
    */
  def compareStruct(key:String,oldValue:ValueRecord,newValue:ValueRecord): Boolean ={
    var flag = true
    //比较车辆品牌是否相同
    flag = oldValue.getVehicle_logo().equalsIgnoreCase(newValue.getVehicle_logo())
    if(flag){
      //比较车辆子品牌是否相同
      flag = oldValue.getVehicle_child_logo().equalsIgnoreCase(newValue.getVehicle_child_logo())
    }
    //根据配置对比年款
    if(propConf.compare_vehicle_year && flag){
      flag = oldValue.getVehicle_style().equalsIgnoreCase(newValue.getVehicle_style())
    }
    //根据配置对比车辆类型
    if(propConf.compare_vehicle_type && flag){
      flag = oldValue.getVehicle_type().equalsIgnoreCase(newValue.getVehicle_type())
    }
    //根据配置对比车辆颜色
    if(propConf.compare_vehicle_color && flag){
      flag = oldValue.getVehicle_color().equalsIgnoreCase(newValue.getVehicle_color())
    }
    flag
  }
  /**
    * 比对两条记录的特征是否相似,相似度的阀值通过fakecar.feature.max.distance配置,默认值38<br/>
    * 返回true表示特征相似,返回false表示特征距离超过阀值
    *
    * @param oldValue
    * @param newValue
    * @return
    */
  def compareFeatureBuffer(key:String,  oldValue:ValueRecord, newValue:ValueRecord):Boolean={
    val begin_time = System.nanoTime
    val dissimilarity_count = FeatureVertifyUtil.getDistanceDic(propConf.feature_similar_threshold)
    val feature1:Array[Byte] = oldValue.getVehicle_feature()
    val feature2:Array[Byte] = newValue.getVehicle_feature()
    var flag:Boolean = true

    if(feature1 == feature2){
      return true
    }
    if(feature1 == null || feature2 == null ){
      return false
    }
    //进行距离计算,不同的厂商采用不同的比对算法
    var distance:Float = 0
    if (propConf.algorithm_type.equals("glst")){
       distance = FeatureVertifyUtil.Compute_reid_score(feature1,feature2)
    }else{
      if(propConf.feature_similar_threshold>55){
          distance = FeatureVertifyUtil.CalcHammingDist(feature1, feature2, dissimilarity_count)
      }else{
          distance = FeatureVertifyUtil.CalcHammingDist(feature1, feature2)
      }
    }
    flag
  }
}

//case class ResultRecord_n(str: String, frecord: ValueRecord, srecord: ValueRecord ,bool: Boolean)
