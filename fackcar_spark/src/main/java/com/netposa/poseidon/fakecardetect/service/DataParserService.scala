package com.netposa.poseidon.fakecardetect.service

import java.util.Calendar

import com.alibaba.fastjson.{JSON, JSONObject}
import com.netposa.poseidon.fakecardetect.bean.InputMsg
import com.netposa.poseidon.fakecardetect.main.FakeCarDetectMainServer.LOG
import com.netposa.poseidon.fakecardetect.utils.VehicleAllInfo
import org.apache.kafka.clients.consumer.ConsumerRecord
import sun.misc.BASE64Decoder

class DataParserService {
  def dataParser(msg:Array[Byte]):InputMsg={
//    msg.offset()
    val validData:InputMsg = new InputMsg
    val decoder = new BASE64Decoder
    //proto反序列化
    val allinfo: VehicleAllInfo.vehicleAllInfoMsg = try {
      VehicleAllInfo.vehicleAllInfoMsg.parseFrom(msg)
    } catch {
      case t: Throwable => {
        LOG.info("parser kafka message info error ,info is %s".format(t.getStackTraceString))
        null
      }
    }
    //原始数据
    val yssjInfo:JSONObject =try{
      JSON.parseObject(allinfo.getYssj.toStringUtf8)
    }catch {
      case t:Throwable =>{
        LOG.info("Yssj to json error, info is %s".format(t.getMessage))
        null
      }
    }
//    println("==================原始"+yssjInfo.toJSONString)

    //二次识别
    val rcgInfo:JSONObject = try{
      JSON.parseObject(allinfo.getRcg.toStringUtf8)
    }catch {
      case t:Throwable =>{
        LOG.info("rcg to json error, info is %s".format(t.getMessage))
        null
      }
    }
//    println("==================二次"+rcgInfo.toJSONString)
    //获得记录编号
    try{
      validData.setJlbh(allinfo.getJlbh.toStringUtf8)
    }catch {
      case t:Throwable =>{
        LOG.info("jlbh to String error, info is %s".format(t.getMessage))
      }
    }
    //解析号牌相关
    try{
      //二次识别号牌相关信息
      val rcgPlatInfo = rcgInfo.getJSONObject("Plate")
      if (rcgPlatInfo!=null){
        //原始数据号牌相关信息
        val orgPlateInfo = yssjInfo.getJSONObject("Plate")
        //rcg号牌号码
        validData.setRcgPlate(rcgPlatInfo.getString("PlateText"))
        //hpys
        validData.setHpys(rcgPlatInfo.getString("PlateColor"))
        //PlateConfidence
        validData.setPlateConf(rcgPlatInfo.getDouble("Confidence").toFloat)
        //yshp
        validData.setOrgHphm(orgPlateInfo.getString("PlateText"))
        //cllx
        try{
          validData.setCllx(rcgInfo.getString("VehicleClass"))
        }catch {
          case e:Exception=>{
            LOG.info("Parser clxx error,info is %s".format(e.getMessage))
          }
        }
        //csys
        try{
          validData.setCsys(rcgInfo.getString("VehicleColor"))
        }catch {
          case e:Exception=>{
            LOG.info("Parser csys error,info is %s".format(e.getMessage))
          }
        }
        //clpp
        try{
          validData.setClpp(rcgInfo.getString("VehicleBrand"))
        }catch {
          case e:Exception=>{
            LOG.info("Parser clpp error,info is %s".format(e.getMessage))
          }
        }
        //clzpp
        try{
          validData.setClzpp(rcgInfo.getString("VehicleSubBrand"))
        }catch {
          case e:Exception=>{
            LOG.info("Parser clzpp error,info is %s".format(e.getMessage))
          }
        }
        //clnk
        try{
          validData.setClnk(rcgInfo.getString("VehicleYear"))
        }catch {
          case e:Exception=>{
            LOG.info("Parser clnk error,info is %s".format(e.getMessage))
          }
        }
        //品牌置信度
        try{
          validData.setClppConf(rcgInfo.getDouble("VehicleBrandConfidence").toFloat)
        }catch {
          case e:Exception=>{
            LOG.info("Parser clppConfidence error,info is %s".format(e.getMessage))
          }
        }
        //车辆宽高比
        try{
          //车辆坐标信息
          val rcgVehiclePos = rcgInfo.getJSONObject("VehiclePosition")
          //车辆左上角X,左上Y，宽width，高height
          val vehicle_left_x = rcgVehiclePos.getInteger("X")
          val vehicle_left_y = rcgVehiclePos.getInteger("Y")
          val vehicle_width = rcgVehiclePos.getInteger("Width")
          val vehicle_height = rcgVehiclePos.getInteger("Height")
          if(vehicle_width>0 && vehicle_height>0){
            validData.setAspectRatio(vehicle_width.toFloat / vehicle_height.toFloat)
          }
        }catch {
          case e: Exception => {
//            LOG.info("Parser aspect ratio error,info is %s".format(e.getMessage))
          }
        }
        //headrear
        try{
          validData.setHeadRear(rcgInfo.getString("VehicleHeadend"))
        }catch {
          case e:Exception=>{
            LOG.info("Parser head rear error,info is %s".format(e.getMessage))
          }
        }
        //feature data
        try{
          validData.setFeature(decoder.decodeBuffer(rcgInfo.getString("Feature")))
        }catch {
          case e:Exception=>{
            LOG.info("Parser feature data error,info is %s".format(e.getMessage))
          }
        }
        //经过时间
        try{
          validData.setJgsj(rcgInfo.getLong("PassTime"))
        }catch {
          case e:Exception=>{
            LOG.info("Parser jgsj error,info is %s".format(e.getMessage))
          }
        }
        //经过时间小时
        try{
          val calend = Calendar.getInstance
          calend.setTimeInMillis(rcgInfo.getLong("PassTime"))
          val hour:Int = calend.get(Calendar.HOUR_OF_DAY)
          validData.setJgxs(hour)
        }catch {
          case e:Exception=>{
            LOG.info("Parser jgsj Hour error,info is %s".format(e.getMessage))
          }
        }
        //kkbh
        try{
          validData.setKkbh(rcgInfo.getString("TollgateNo"))
        }catch {
          case e:Exception=>{
            LOG.info("Parser kkbh error,info is %s".format(e.getMessage))
          }
        }
        //xzqh
        try{
          validData.setXzqh(rcgInfo.getString("OrgId"))
        }catch {
          case e:Exception=>{
            LOG.info("Parser xzqh error,info is %s".format(e.getMessage))
          }
        }
        //Confidence
        try{
          validData.setConf(rcgInfo.getDouble("WholeConfidence").toFloat)
        }catch {
          case e:Exception=>{
            LOG.info("Parser whole Conffidence error,info is %s".format(e.getMessage))
          }
        }
      }else{
        LOG.error("The error jlbh :"+ validData.getJlbh())
      }
    }catch {
      case t:Throwable =>{
        LOG.info("Parser Info error, info is %s".format(t.getMessage))
      }
    }
    validData
  }
}
