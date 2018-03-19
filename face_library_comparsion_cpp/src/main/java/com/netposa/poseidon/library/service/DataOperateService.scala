package com.netposa.poseidon.library.service

import com.netposa.HbaseUtil
import com.netposa.poseidon.library.rpc.outrpc._
import com.netposa.poseidon.library.util.HashAlgorithm
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.{Delete, Put, Table}
import org.apache.hadoop.hbase.util.Bytes
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

/**
  * Created by 杨鹏飞 on 2017/7/4.
  * Description:
  */
class DataOperateService {
    val FAMILY = Bytes.toBytes("cf")
    val EXT = Bytes.toBytes("ext")
    val log = LoggerFactory.getLogger(classOf[DataOperateService])

    //数据接入
    def data_Operate(storageRequest: StorageRequest): StorageResponse = {

        storageRequest.`type`.getValue match {
            case 1 => op_insert(storageRequest.storageInfos.toList)

            case 2 => op_insert(storageRequest.storageInfos.toList)

            case 3 => op_delete(storageRequest.storageInfos.toList)

            case 4 => new StorageResponse(StatusCode.ERROR_PARAM, "不支持查询操作！")

            case _ => new StorageResponse(StatusCode.ERROR_PARAM, "参数错误请检查！")
        }
    }


    private def op_insert(listinfo: List[StorageInfo]): StorageResponse = {
        //初始化Hbase 链接对象
        val conn = HbaseUtil.getConn
        val cols = listinfo.map(v => (v.libraryId -> v)).groupBy(_._1)
        //log.info("listinfo:............"+listinfo)
        //log.info("cols:............"+cols)
        var reSponse: StorageResponse = null
        try {
            cols.foreach(col => {
                val tableName = TableName.valueOf(col._1)
                val regionNum = HbaseUtil.getConn.getRegionLocator(tableName).getAllRegionLocations.size()
                if (!conn.getAdmin.tableExists(tableName)) {
                    return new StorageResponse(StatusCode.ERROR_NOT_EXIST_LIBRARY, s"检索库[${col._1}] 在Hbase中不存 ，请先执行建库操作！")
                }
                val table: Table = conn.getTable(tableName)
                val putList: ListBuffer[Put] = ListBuffer[Put]()
                col._2.foreach(storInfo => {
                    //log.info("storInfo:............"+storInfo)
                    val put: Put = new Put(Bytes.toBytes(HashAlgorithm.hash(storInfo._2.id,regionNum)))
                    //put ext
                    put.addColumn(FAMILY, EXT, Bytes.toBytes(storInfo._2.ext))
                    storInfo._2.imgInfos.foreach(feature => {
                        put.addColumn(FAMILY, Bytes.toBytes(feature.getImgId), feature.getFeature)
                    })
                    putList += (put)
                })
                table.put(putList)
                table.close()
            })
            if (reSponse != null) reSponse else new StorageResponse(StatusCode.OK, "INSERT SUCCESS!")
        } catch {
            case e: Exception =>
                new StorageResponse(StatusCode.ERROR_OTHER, e.getMessage)
        } finally {
            if (conn != null) conn.close()
//            if(reSponse !=null) reSponse else  new StorageResponse(StatusCode.OK,"INSERT SUCCESS!")
        }
    }

    import scala.collection.JavaConversions._

    //op_delete
    private def op_delete(listinfo: List[StorageInfo]): StorageResponse = {
        val conn = HbaseUtil.getConn
        var reRespon: StorageResponse = null
        try {
            val libs = listinfo.map(v => (v.libraryId -> v)).groupBy(_._1)
            libs.foreach(col => {
                // val del_cols = col._2.map(_._2.id)
                //
                val del_cols = col._2.map(per => per._2.id -> per._2.imgInfos.toList)
                //删除Hbase 数据
                val hb_del = op_delete_hbase(col._1, del_cols)
                if (hb_del.rCode.getValue != 1 && reRespon == null) {
                    reRespon = hb_del
                }
            })
        } catch {
            case e: Exception =>
                log.error(e.getMessage, e)
                new StorageResponse(StatusCode.ERROR_OTHER, e.getMessage)
        } finally {
            if (!conn.isClosed) conn.close()
        }

        def op_delete_hbase(tableName: String, del_persons: List[(String, List[ImgInfo])]): StorageResponse = {
            val table = conn.getTable(TableName.valueOf(tableName))
            if (!conn.getAdmin.tableExists(table.getName)) {
                new StorageResponse(StatusCode.ERROR_NOT_EXIST_LIBRARY, s"检索库[${tableName}] 在Hbase中不存 ，请先执行建库操作！")
            }
            val delList = ListBuffer[Delete]()
            del_persons.foreach(per => {
                if (per._2 != null && per._2.size > 0) {
                    per._2.foreach(fea => {
                        val del = new Delete(Bytes.toBytes(HashAlgorithm.hash(per._1, HbaseUtil.getConn.getRegionLocator(TableName.valueOf(tableName)).getAllRegionLocations.size())))
                        // del.addColumn(FAMILY, Bytes.toBytes(fea.getImgId))
                        del.addColumn(FAMILY, Bytes.toBytes(fea.getImgId))
                        //del.addColumn(EXT,Bytes.toBytes(fea.getImgId))
                        delList += del
                    })
                } else {
                    delList += new Delete(Bytes.toBytes(HashAlgorithm.hash(per._1, HbaseUtil.getConn.getRegionLocator(TableName.valueOf(tableName)).getAllRegionLocations.size())))
                }
            })
            table.delete(delList)
            table.close()
            new StorageResponse(StatusCode.OK, s"${tableName} DELETE SUCCESS!")
        }

        if (reRespon != null) reRespon else new StorageResponse(StatusCode.OK, s"DELETE SUCCESS!")
    }
}