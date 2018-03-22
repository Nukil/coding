package com.netposa.poseidon.fakecardetect.bean

class ResultRecord(inKey:String,inFirstValue:ValueRecord,inSecondValue:ValueRecord,inFlag:Boolean) extends Serializable{

  private var key:String = inKey
  private var firstValue:ValueRecord = inFirstValue
  private var secondValue:ValueRecord = inSecondValue
  private var flag:Boolean = inFlag//true表示是套牌车需要入库，False表示不是套牌需要更新全局信息
  def this(inFirstValue:ValueRecord,inFlag:Boolean){
    this(null,inFirstValue,null,inFlag)
  }
  def this(inKey:String,inFirstValue:ValueRecord,inFlag:Boolean){
    this(inKey,inFirstValue,null,inFlag)
  }
  def getKey():String={
    return key
  }
  def getFirstValue():ValueRecord = {
    return firstValue
  }
  def getSecondValue():ValueRecord ={
    return secondValue
  }
  def getFlag():Boolean={
    return flag
  }



}
