package com.netposa.poseidon.fakecardetect.service

import java.text.SimpleDateFormat

import com.netposa.poseidon.fakecardetect.bean.ResultRecord
import com.netposa.poseidon.fakecardetect.main.FakeCarDetectMainServer.{LOG, propConf, server_proper}
import com.netposa.poseidon.fakecardetect.utils.ConnectionPoolUtil

import scala.collection.mutable.ArrayBuffer
object Send2DBService {
  //数据库连接设置
  val mysqlConnInstance = ConnectionPoolUtil.getInstance()
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  private def tryAgain(sqlStr:StringBuffer,size:Int, time:Long): Unit ={
    var connection : java.sql.Connection = null
    var statement : java.sql.Statement = null
    try{
      connection = mysqlConnInstance.getConnection
      statement = connection.createStatement()////
      statement.execute(sqlStr.substring(0, sqlStr.length() - 1))
      if(false){
        LOG.info("The inserted  size is [%d] ,times is [%d] ms"
          .format(size, System.currentTimeMillis() - time))
      }
    }catch{
      case e:Exception=>
        error("write failed try again!!".format(e.getMessage))
        tryAgain(sqlStr,size,time)
    }finally {
      if (statement != null) {
        statement.close()
      }
      if (connection != null) {
        connection.close()
      }
    }

  }


  def sendFakeCar2DB(result:Array[ResultRecord]):Unit={
    val sqlValue = new StringBuffer()
    sqlValue.append("INSERT ignore INTO "+ConnectionPoolUtil.tbname+" "+ConnectionPoolUtil.sql++" VALUES")
    result.foreach(res=>{
      val firstValue = res.getFirstValue()
      if (res.getSecondValue()!=null) {
        val secondValue = res.getSecondValue()
        val key = res.getKey()
        val keyArr = key.split("-")
        sqlValue.append("(")
        //HPHM_：车牌号字段名称,字段不能为空
        sqlValue.append(
          if (keyArr(0) != null) {
            "'" + keyArr(0) + "'" + ","
          } else {
            "%s,".format(null)
          }
        )
        //FHPYS_：第一条车牌颜色字段名称,字段不能为空
        sqlValue.append(
          if (firstValue.getLicence_plate_color() != null) {
            "'" + new String(firstValue.getLicence_plate_color()) + "'" + ","
          } else {
            "%s,".format(null)
          }
        )
        //SHPYS_：车牌颜色字段名称,字段不能为空
        sqlValue.append(
          if (secondValue.getLicence_plate_color() != null) {
            "'" + new String(secondValue.getLicence_plate_color()) + "'" + ","
          } else {
            "%s,".format(null)
          }
        )
        //FCLYS_：第一条过车记录的车辆颜色字段名称
        sqlValue.append(
          if (firstValue.getVehicle_color() != null) {
            "'" + new String(firstValue.getVehicle_color()) + "'" + ","
          } else {
            "%s,".format(null)
          }
        )
        //    SCLYS_：第二条过车记录的车辆颜色字段名称
        sqlValue.append(
          if (secondValue.getVehicle_color() != null) {
            "'" + new String(secondValue.getVehicle_color()) + "'" + ","
          } else {
            "%s,".format(null)
          }
        )
        //    FCLLX_：第一条过车记录的车辆类型字段名称
        sqlValue.append(
          if (firstValue.getVehicle_type() != null) {
            "'" + new String(firstValue.getVehicle_type()) + "'" + ","
          } else {
            "%s,".format(null)
          }
        )
        //    SCLLX_：第二条过车记录的车辆类型字段名称
        sqlValue.append(
          if (secondValue.getVehicle_type() != null) {
            "'" + new String(secondValue.getVehicle_type()) + "'" + ","
          } else {
            "%s,".format(null)
          }
        )
        //    FCLPP_：第一条过车记录的车辆品牌字段名称
        sqlValue.append(
          if (firstValue.getVehicle_logo() != null) {
            "'" + new String(firstValue.getVehicle_logo()) + "'" + ","
          } else {
            "%s,".format(null)
          }
        )
        //    SCLPP_：第二条过车记录的车辆品牌字段名称
        sqlValue.append(
          if (secondValue.getVehicle_logo() != null) {
            "'" + new String(secondValue.getVehicle_logo()) + "'" + ","
          } else {
            "%s,".format(null)
          }
        )
        //    FCLZPP_：第一条过车记录的车辆子品牌字段名称
        sqlValue.append(
          if (firstValue.getVehicle_child_logo() != null) {
            "'" + new String(firstValue.getVehicle_child_logo()) + "'" + ","
          } else {
            "%s,".format(null)
          }
        )
        //    SCLZPP_：第二条过车记录的车辆子品牌字段名称
        sqlValue.append(
          if (secondValue.getVehicle_child_logo() != null) {
            "'" + new String(secondValue.getVehicle_child_logo()) + "'" + ","
          } else {
            "%s,".format(null)
          }
        )
        //    FCLNK_：第一条过车记录的车辆年款字段名称
        sqlValue.append(
          if (firstValue.getVehicle_style() != null) {
            "'" + new String(firstValue.getVehicle_style()) + "'" + ","
          } else {
            "%s,".format(null)
          }
        )
        //    SCLNK_：第二条过车记录的车辆品牌字段名称
        sqlValue.append(
          if (secondValue.getVehicle_style() != null) {
            "'" + new String(secondValue.getVehicle_style()) + "'" + ","
          } else {
            "%s,".format(null)
          }
        )
        //    FGCBH_：第一条过车记录的记录编码字段名称
        sqlValue.append(
          if (firstValue.getRecord_id() != null) {
            "'" + new String(firstValue.getRecord_id()) + "'" + ","
          } else {
            "%s,".format(null)
          }
        )
        //    SGCBH_：第二条过车记录的记录编码字段名称
        sqlValue.append(
          if (secondValue.getRecord_id() != null) {
            "'" + new String(secondValue.getRecord_id()) + "'" + ","
          } else {
            "%s,".format(null)
          }
        )
        //    FAKEPLATETYPE：计算出来的两条记录的套牌类型值存储字段名称
        sqlValue.append(
          "'" + "0" + "'" + ","
        )
        //    INSERTTIME_：数据写入时间字段
        sqlValue.append(
          "'" + dateFormat.format(System.currentTimeMillis()) + "'" + ","
        )
        //    FKKBH_：第一条过车记录的卡口ID
        sqlValue.append(
          if (firstValue.getKkbh() != null) {
            "'" + new String(firstValue.getKkbh()) + "'" + ","
          } else {
            "%s,".format(null)
          }
        )
        //    FXZQH_：第一条过车记录的组织机构ID
        sqlValue.append(
          if (firstValue.getXzqh() != null) {
            "'" + new String(firstValue.getXzqh()) + "'" + ","
          } else {
            "%s,".format(null)
          }
        )
        //SKKBH_：第二条过车记录的卡口ID
        sqlValue.append(
          if (secondValue.getKkbh() != null) {
            "'" + new String(secondValue.getKkbh()) + "'" + ","
          } else {
            "%s,".format(null)
          }
        )
        //SXZQH_：第二条过车记录的组织机构ID
        sqlValue.append(
          if (secondValue.getXzqh() != null) {
            "'" + new String(secondValue.getXzqh()) + "'" + ")"
          } else {
            "%s,".format(null)
          }
        )
        sqlValue.append(",")
      }
    })

    //入库操作
    var connection : java.sql.Connection = null
    var statement : java.sql.Statement = null
    try{
      connection = mysqlConnInstance.getConnection
      statement = connection.createStatement()
//      System.out.println(sqlValue.toString.substring(0,sqlValue.length()-1))
      statement.execute(sqlValue.toString.substring(0,sqlValue.length()-1))
//      if(false){
//        LOG.info("The inserted fakecar recordID is "+ new String(firstValue.getRecord_id())+"and"+ new String(secondValue.getRecord_id()))
//      }
    }catch {
      case e:Exception=>
        LOG.info("Insert to mysql Failed!! try again!! ,info is %s" .format(e.getMessage))
    }finally {
      if (statement != null) {
        statement.close()
      }
      if (connection != null) {
        connection.close()
      }
    }
  }
}
