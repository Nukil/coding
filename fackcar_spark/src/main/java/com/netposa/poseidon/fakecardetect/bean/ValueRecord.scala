package com.netposa.poseidon.fakecardetect.bean

class ValueRecord extends Serializable{
  private var vehicle_color:String = null
  private var vehicle_type:String = null
  private var vehicle_logo:String = null
  private var vehicle_child_logo:String = null
  private var vehicle_style:String = null
  private var vehicle_feature:Array[Byte] = null
  private var record_id:String = null
  private var licence_plate_color:String = null
  private var old_licence_plate:String = null
  private var kkbh:String = null
  private var xzqh:String = null
  private var headrear:String = null
  private var jgsj:Long = -1
  private val initTime = System.currentTimeMillis()

  def getVehicle_color():String={return vehicle_color}

  def setVehicle_color(vehicle_color:String):Unit={this.vehicle_color = vehicle_color}

  def getVehicle_type():String={return  vehicle_type}

  def setVehicle_type(vehicle_type:String):Unit={this.vehicle_type = vehicle_type}

  def getVehicle_logo():String={return vehicle_logo}

  def setVehicle_logo(vehicle_logo:String):Unit={this.vehicle_logo = vehicle_logo}

  def getVehicle_child_logo():String={return vehicle_child_logo}

  def setVehicle_child_logo(vehicle_child_logo:String):Unit={this.vehicle_child_logo = vehicle_child_logo}

  def getVehicle_style():String={return vehicle_style}

  def setVehicle_style(vehicle_style:String):Unit={this.vehicle_style = vehicle_style}

  def getVehicle_feature():Array[Byte]={return  vehicle_feature}

  def setVehicle_feature(vehicle_feature:Array[Byte]):Unit={this.vehicle_feature = vehicle_feature}

  def getRecord_id():String={return record_id}

  def setRecord_id(record_id:String):Unit={this.record_id = record_id}

  def getLicence_plate_color():String={return licence_plate_color}

  def setLicence_plate_color(licence_plate_color:String):Unit={this.licence_plate_color = licence_plate_color}

  def getOld_licence_plate():String={return old_licence_plate}

  def setOld_licence_plate(old_licence_plate:String):Unit={this.old_licence_plate = old_licence_plate}

  def getInitTime():Long={return initTime}

  def getKkbh():String={return  kkbh}

  def setKkbh(kkbh:String):Unit={this.kkbh = kkbh}

  def getXzqh():String={return xzqh}

  def setXzqh(xzqh:String):Unit={this.xzqh = xzqh}

  def setHeadrear(headrear:String):Unit={this.headrear = headrear}

  def getHeadrear():String={return headrear}

  def setJgsj(jgsj:Long):Unit={this.jgsj = jgsj}

  def getJgsj():Long={return jgsj}

  override def toString = s"ValueRecord($vehicle_color, $vehicle_type, $vehicle_logo, $vehicle_child_logo, $vehicle_style, $vehicle_feature, $record_id, $licence_plate_color, $old_licence_plate, $kkbh, $xzqh, $headrear, $jgsj, $initTime, $getVehicle_color, $getVehicle_type, $getVehicle_logo, $getVehicle_child_logo, $getVehicle_style, $getVehicle_feature, $getRecord_id, $getLicence_plate_color, $getOld_licence_plate, $getInitTime, $getKkbh, $getXzqh, $getHeadrear, $getJgsj)"
}
