package com.netposa.poseidon.fakecardetect.bean

class InputMsg extends Serializable{
  //记录编号
  private var jlbh:String = null
  //二次识别号牌
  private var licence_plate:String  = null
  //原始号牌
  private var old_licence_plate:String = null
  //号牌颜色
  private var licence_plate_color:String =null
  //号牌置信度
  private var plate_confidence:Float = 0f
  //车辆类型
  private var vehicle_type:String = null
  //车身颜色
  private var vehicle_color:String = null
  //车辆品牌
  private var vehicle_logo:String = null
  //品牌置信度
  private var logo_confidence:Float = 0f
  //车辆子品牌
  private var vehicle_child_logo:String = null
  //车辆年款
  private var vehicle_style:String = null
//  //坐标信息
//  private var vehicle_left_x:Int = null
//  private var vehicle_left_y:Int = null
//  private var vehicle_width:Int = null
//  private var vehicle_height:Int = null
  //车辆宽高比值
  private var aspect_ratio:Float = 0f
  //车头车尾
  private var head_rear:String = null
  //特征数据
  private var vehicle_feature:Array[Byte] = null
  //经过时间
  private var traffic_time:Long = 0l
  //经过小时
  private var traffi_hour:Int = -1
  //卡口编号
  private var traffic_kkbh:String = null
  //行政区划
  private var traffic_xzqh:String = null
  //整体置信度
  private var confidence_level:Float = 0f

  def setJlbh(jlbh:String)={
    this.jlbh = jlbh
  }
  def getJlbh():String={
    return jlbh
  }
  def setRcgPlate(plate:String)={
    this.licence_plate = plate
  }
  def getRcgHphm():String={
    return licence_plate
  }
  def setOrgHphm(yshp:String)={
    this.old_licence_plate = yshp
  }
  def getOrgHphm():String={
    return old_licence_plate
  }
  def setHpys(hpys:String)={
    this.licence_plate_color = hpys
  }
  def getHpys():String={
    return licence_plate_color
  }
  def setPlateConf(conf:Float)={
    this.plate_confidence = conf
  }
  def getPlateConf():Float={
    return plate_confidence
  }
  def setCllx(cllx:String)={
    this.vehicle_type = cllx
  }
  def getCllx():String={
    return vehicle_type
  }
  def setCsys(csys:String)={
    this.vehicle_color=csys
  }
  def getCsys():String={
    return vehicle_color
  }
  def setClpp(clpp:String)={
    this.vehicle_logo=clpp
  }
  def getClpp():String={
    return vehicle_logo
  }
  def setClppConf(clppConf:Float)={
    this.logo_confidence = clppConf
  }
  def getClppConf():Float={
    return logo_confidence
  }
  def setClzpp(clzpp:String)={
    this.vehicle_child_logo= clzpp
  }
  def getClzpp():String={
    return vehicle_child_logo
  }
  def setClnk(clnk:String)={
    this.vehicle_style = clnk
  }
  def getClnk():String={
    return vehicle_style
  }
//  def setX(x:Int)={
//    this.vehicle_left_x = x
//  }
//  def getX():Int={
//    return vehicle_left_x
//  }
//  def setY(y:Int)={
//    this.vehicle_left_y =y
//  }
//  def getY():Int={
//    return vehicle_left_y
//  }
//  def setW(w:Int)={
//    this.vehicle_width = w
//  }
//  def getW():Int={
//    return vehicle_width
//  }
//  def setH(h:Int)={
//    this.vehicle_height = h
//  }
//  def getH():Int={
//    return vehicle_height
//  }
  def setAspectRatio(ratio:Float)={
    this.aspect_ratio = ratio
  }
  def getAspectRatio():Float={
    return  aspect_ratio
  }
  def setHeadRear(headRear:String)={
    this.head_rear = headRear
  }
  def getHeadRear():String={
    return head_rear
  }
  def setFeature(feature:Array[Byte])={
    this.vehicle_feature = feature
  }
  def getFeature():Array[Byte]={
    return vehicle_feature
  }
  def setJgsj(jgsj:Long)={
    this.traffic_time = jgsj
  }
  def getJgsj():Long={
    return traffic_time
  }
  def setJgxs(jgxs:Int)={
    this.traffi_hour = jgxs
  }
  def getJgxs():Int={
    return traffi_hour
  }
  def setKkbh(kkbh:String)={
    this.traffic_kkbh
  }
  def getKkbh():String={
    return traffic_kkbh
  }
  def setXzqh(xzqh:String)={
    this.traffic_xzqh = xzqh
  }
  def getXzqh():String={
    return traffic_xzqh
  }
  def setConf(conf:Float)={
    this.confidence_level = conf
  }
  def getConf():Float={
    return confidence_level
  }
  def getAll():String={
    return ("JLBH:"+getJlbh()+"\n"+"HEADREAR:"+getHeadRear()+"\n"+"XZQH:"+getXzqh()+"\n"+"KKBH:"+getKkbh()+"\n"+"YSHP:"+getOrgHphm()+"\n"+"HPYS:"+getHpys()+"\n"
      +"CSYS:"+getCsys()+"\n"+"CLLX:"+getCllx()+"\n"+"CLNK:"+getClnk()+"\n"+"CLZPP:"+getClzpp()+"\n"+"HPHM:"+getRcgHphm()+"\n"+"ASPECTRATIO:"+getAspectRatio().toString)
  }

  override def toString = s"InputMsg($jlbh, $licence_plate, $old_licence_plate, $licence_plate_color, $plate_confidence, $vehicle_type, $vehicle_color, $vehicle_logo, $logo_confidence, $vehicle_child_logo, $vehicle_style, $aspect_ratio, $head_rear, $vehicle_feature, $traffic_time, $traffi_hour, $traffic_kkbh, $traffic_xzqh, $confidence_level, $getJlbh, $getRcgHphm, $getOrgHphm, $getHpys, $getPlateConf, $getCllx, $getCsys, $getClpp, $getClppConf, $getClzpp, $getClnk, $getAspectRatio, $getHeadRear, $getFeature, $getJgsj, $getJgxs, $getKkbh, $getXzqh, $getConf, $getAll)"
}
