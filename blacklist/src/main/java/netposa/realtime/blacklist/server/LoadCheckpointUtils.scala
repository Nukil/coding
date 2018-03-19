package netposa.realtime.blacklist.server

import netposa.realtime.blacklist.bean.CheckpointConfBean
import java.sql.DriverManager
import java.sql.Connection
import java.sql.Statement
import kafka.utils.Logging
import netposa.realtime.kafka.utils.ByteUtils
import java.util.Arrays
import scala.collection.mutable.ListBuffer

/**
  * 加载卡口元数据表的工具
  */
class LoadCheckpointUtils(val cf: CheckpointConfBean, val debug: Boolean) extends Logging {
    val sql = initSql

    /**
      * 读取卡口元数据表中的数据
      */
    def fetchMetadata(statement: Statement): Map[Checkpoint, (Array[Byte], Double, Double)] = {
        info("begin execute sql query [%s]".format(sql))
        val rs = statement.executeQuery(sql)
        val buffer = new ListBuffer[(Checkpoint, (Array[Byte], Double, Double))]()
        try {
            while (rs.next()) {
                val key = new Checkpoint(rs.getBytes(1))
                val orgid = rs.getBytes(2)
                val longitude = if (rs.getDouble(3) == 0 || "".equals(rs.getDouble(3))) 0 else rs.getDouble(3)
                val latitude = if (rs.getDouble(4) == 0 || "".equals(rs.getDouble(4))) 0 else rs.getDouble(4)
                val value = (orgid, longitude, latitude)
                val row = (key, value)
                buffer += row
                if (debug) {
                    val debug = "id=%s,orgid=%s".format(rs.getString(1), rs.getString(2))
                    //          info(debug)
                }
            }

            info("execute sql query successed, query size is %d".format(buffer.size))
        } finally {
            rs.close()
        }
        buffer.toMap
    }

    /**
      * 得到一个与卡口元数据表连接的connection
      */
    def initConnection(): Connection = {
        Class.forName(cf.db_class)
        val connection = cf.db_pwd match {
            case null => {
                info("no password set, connection only to url")
                DriverManager.getConnection(cf.db_url, cf.db_user, null)
            }
            case _ => {
                info("yes password set, connection to url and user pwd")
                DriverManager.getConnection(cf.db_url, cf.db_user, cf.db_pwd)
            }
        }
        connection
    }


    def closeStatement(statement: Statement) {
        try {
            statement match {
                case null =>
                case _ => statement.close()
            }
        } catch {
            case e: Throwable => {
                warn("close statement error => %s".format(e.getMessage()), e)
            }
        }
    }

    def closeConnection(connection: Connection) {
        try {
            connection match {
                case null =>
                case _ => connection.close()
            }
        } catch {
            case e: Throwable => {
                warn("close connection error => %s".format(e.getMessage()), e)
            }
        }
    }


    private def initSql(): String = {
        val buf = "SELECT %s,%s,%s,%s FROM %s".format(cf.checkpoint, cf.orgid, cf.longitude, cf.latitude, cf.table_name)
        buf
    }


}

case class Checkpoint(val bytes: Array[Byte]) {
    override def toString(): String =
        "checkpoint=%s".format(new String(bytes))

    override def equals(any: Any): Boolean = {
        any match {
            case that: Checkpoint => ByteUtils.equals(bytes, that.bytes)
            case _ => false
        }
    }

    override def hashCode(): Int = Arrays.hashCode(bytes)
}