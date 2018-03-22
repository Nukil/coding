package com.netposa.poseidon.fakecardetect.bean

class KeyRecord (inLicencePlate:String,inLicencePlateColor:String,inHeadRear:String)extends Serializable{
  private var licence_plate:String = inLicencePlate
  private var licence_plate_color:String = inLicencePlateColor
  private var head_rear:String = inHeadRear

  def this(licencePlate:String){
    this(licencePlate,null,null)
  }

  def this(licencePlate:String,licencePlateColor:String){
    this(licencePlate,licencePlateColor,null)
  }

  def getLicence_plate():String={
    return licence_plate
  }
  def setLicence_plate(licence_plate:String):Unit={
    this.licence_plate = licence_plate
  }
  def getLicence_plate_color():String={
    return licence_plate_color
  }

  def setLicence_plate_color(licence_plate_color:String):Unit={
    this.licence_plate_color = licence_plate_color
  }

  def getHead_rear():String={
    return  head_rear
  }
  def setHead_rear(head_rear:String):Unit={
    this.head_rear = head_rear
  }

  def keyEqual(otherKey:KeyRecord):Boolean={
    var flag = false
    if(otherKey.getLicence_plate()== licence_plate
      && otherKey.getLicence_plate_color()==licence_plate_color
      && otherKey.getHead_rear() == head_rear){
      flag = true
    }else{
      flag = false
    }
    flag
  }

  override def toString: String = return  new String(licence_plate) + (if (licence_plate_color == null) "null"
  else new String(licence_plate_color))+ head_rear



}
