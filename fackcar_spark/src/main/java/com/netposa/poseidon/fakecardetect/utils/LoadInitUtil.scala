package com.netposa.poseidon.fakecardetect.utils

import java.io.{BufferedReader, InputStreamReader}
import java.util.Properties

import com.netposa.poseidon.fakecardetect.main.FakeCarDetectMainServer.{LOG,server_proper}
import com.netposa.poseidon.fakecardetect.bean.{KeyRecord,ValueRecord}
import org.apache.commons.lang.StringUtils

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
object LoadInitUtil {
  //编码格式
  val charSet:String = "UTF-8"
  /**
    * 加载配置文件返回Map对象
    *
    * @param path
    */
  def initProp2Map(path:String):Map[String ,String] = {
    var propMap = Map[String,String]()
    val prop = new Properties()
    val inStream = getClass.getClassLoader.getResourceAsStream(path)
    try{
      LOG.info(s"laod config [${path}]")
      prop.load(inStream)
      propMap=prop.map(v=>{
        LOG.info(s"key is [${v._1}] value is [${v._2}]")
        v._1->v._2
      }).toMap
    }catch {
      case e:Exception =>
        LOG.error("Load propfile error, info is %s".format(e.getMessage))
    }
    propMap
  }
  /**
    * 加载配置文件返回Properties对象
    *
    * @param proname
    */
  def initProps(proname:String): Properties = {
    //加载配置信息
    val props = new Properties()
    val inStream = new InputStreamReader(LoadInitUtil.getClass.getClassLoader.getResourceAsStream(proname),"utf-8")
    try {
      props.load(inStream)
    } finally {
      inStream.close()
    }
    props
  }

  /**
    * 加载车辆子品牌字典表
    */
  def loadVehicleSubBrand():Map[String,String]={
    var vehicleSubBrandMap = Map[String,String]()
    var br:BufferedReader = null
    try{
      br = new BufferedReader(new InputStreamReader(this.getClass.getClassLoader.getResourceAsStream("VehicleSubBrand.txt")))
      var line:String = null
      line = br.readLine()
      while(line !=null){
        try{
          var args:Array[String] = line.split(",")
          var name:String = args(2)
          if(name.contains("（")){
          name = name.substring(0,name.indexOf("（"))
          }
          vehicleSubBrandMap += (args(1)->name)
          line = br.readLine()
        }catch {
          case e:Exception =>
            LOG.error("read VehicleSubBrand.txt error info is %s".format(e.getMessage))
        }
      }
      vehicleSubBrandMap
    }catch {
      case e:Exception =>
        LOG.error("load VehicleSubBrand error", e)
        null
    }finally {
      if(br!=null){
        try{
          br.close()
        }catch {
          case e:Exception =>
            LOG.error("Close BufferedReader Error!! info is %s".format(e.getMessage))
        }
      }
    }
  }
  /**
    * 加载车辆品牌字典表
    */
  def loadVehicleBrand():Map[String,String]={
    var vehicleBrandMap = Map[String,String]()
    var br:BufferedReader = null
    try{
      br = new BufferedReader(new InputStreamReader(this.getClass.getClassLoader.getResourceAsStream("VehicleBrand.txt")))
      var line:String = null
      line = br.readLine()
      while( line!=null){
        try{
          var args:Array[String] = line.split(",")
          var name:String = args(1)
          if(name.contains("（")){
            name = name.substring(0,name.indexOf("（"))
          }
          vehicleBrandMap += (args(0)->name)
          line=br.readLine()
        }catch {
          case e:Exception =>
            LOG.error("read VehicleBrand.txt error info is %s".format(e.getMessage))
        }
      }
      vehicleBrandMap
    }catch {
      case e:Exception =>
        LOG.error("load VehicleBrand error", e)
        null
    }finally {
      if(br!=null){
        try{
          br.close()
        }catch {
          case e:Exception =>
            LOG.error("Close BufferedReader Error!! info is %s".format(e.getMessage))
        }
      }
    }
  }

  /**
    * 加载车辆子品牌组过滤字典
    */
//  def loadSubBrandMutex():Map[String,String]={
//    var br:BufferedReader = null
//    try{
//      br = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("ChildBrandMutex.txt")))
//      var line:String = null
//      while (br.readLine()!= null){
//        try{
//          line = br.readLine()
//          var args = line.split("-")
//          var childLogo1 = args(0)
//          var childLogo2 = args(1)
//          childLogo1 = childLogo1.substring(0, childLogo1.indexOf("("))
//          childLogo2 = childLogo2.substring(0, childLogo2.indexOf("("))
//          var logoBts:Array[Array[Byte]] = new Array[Array[Byte]](2)
//          logoBts(0) = childLogo1.getBytes(charSet)
//          logoBts(1) = childLogo2.getBytes(charSet)
//          var key = getSubBrandMutexKey(childLogo1, childLogo2)
//          if(key!=null){
//            childLogoMutexMap+=(key ->logoBts)
//          }
//          line=null
//        }catch {
//          case e:Exception =>
//            LOG.error("read ChildBrandMutex error is %s".format(e.getMessage))
//        }
//      }
//    }catch {
//      case e:Exception=>
//      LOG.error("load ChildBrandMutex error is %s".format(e.getMessage))
//    }finally {
//      if (br != null) try
//        br.close
//      catch {
//        case e: Exception =>
//          LOG.error("Close bufferReader error info is %s".format(e.getMessage))
//      }
//    }
//  }
  def getSubBrandMutexKey(logo1:String,logo2:String):String={
    val h1:Int = logo1.toInt
    val h2:Int = logo2.toInt
    if(h1>h2){
      return logo1+"_"+logo2
    }else if (h1<h2){
      return logo2 + "_" + logo1
    }
     null
  }
}
