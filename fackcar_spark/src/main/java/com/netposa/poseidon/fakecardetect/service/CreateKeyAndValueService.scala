package com.netposa.poseidon.fakecardetect.service

import com.netposa.poseidon.fakecardetect.bean.{InputMsg, ResultRecord, ValueRecord}
import com.netposa.poseidon.fakecardetect.main.FakeCarDetectMainServer.propConf

import scala.collection.mutable.ArrayBuffer

class CreateKeyAndValueService {

  def create(cleanedData:(InputMsg,Boolean)):ResultRecord={
    var key:String = null
    var value:ValueRecord = null
//    var bufferResult:ResultRecord = new ResultRecord
    if(cleanedData._2){
      //车头车尾，二次识别号牌号码，号牌颜色组成key
      val data = cleanedData._1
      if(data.getRcgHphm().equals(propConf.exclude_plate_startwiths)){
        null
      }else{
        key =data.getRcgHphm()+"-"+data.getHpys()+"-"+data.getHeadRear()
        //组成value
        value = new ValueRecord
        //车辆品牌
        value.setVehicle_logo(data.getClpp())
        //车辆子品牌
        value.setVehicle_child_logo(data.getClzpp())
        //车辆年款
        value.setVehicle_style(data.getClnk())
        //车辆类型
        value.setVehicle_type(data.getCllx())
        //车辆颜色
        value.setVehicle_color(data.getCsys())
        //号牌颜色
        value.setLicence_plate_color(data.getHpys())
        //原始号牌
        value.setOld_licence_plate(data.getOrgHphm())
        //记录编号
        value.setRecord_id(data.getJlbh())
        //特征数据
        value.setVehicle_feature(data.getFeature())
        //卡口编号
        value.setKkbh(data.getKkbh())
        //行政区划
        value.setXzqh(data.getXzqh())
        //车头车尾
        value.setHeadrear(data.getHeadRear())
        //经过时间
        value.setJgsj(data.getJgsj())
        //组合resultrecord
        val resultValue = new ResultRecord(key,value,false)
//        bufferResult += resultValue
        resultValue
      }
    }else{
       null
    }
  }
}
//case class ResultRecord_s(str: String, vrecord: ValueRecord,bool: Boolean)
