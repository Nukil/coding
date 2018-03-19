package netposa.realtime.blacklist.server

import netposa.realtime.blacklist.bean.ControlConfBean
import netposa.realtime.kafka.utils.ByteUtils
import java.util.Arrays
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import kafka.utils.Logging
import java.util.Properties
import org.apache.commons.lang.StringUtils
import scala.collection.mutable.ListBuffer
import netposa.realtime.blacklist.bean.BlacklistBean

/**
  * 加载布控元数据信息的工具
  */
class LoadControlUtils(val cf: ControlConfBean, val props: Properties) extends Logging {
    val debug = props.getProperty("blacklist.process.debug.enable", "false").toBoolean

    val fetchAllSql = initAllSql
    val fetchByIdSql = initByIdSql
    /*
     * blacklist.platecolor.disable.status=null,-1
     * blacklist.vehiclebrand.disable.status=null,-1
     * blacklist.vehiclecolor.disable.status=null,-1
     * blacklist.vehicletype.disable.status=null,-1
     * blacklist.orgid.disable.status=null,-1
     * blacklist.monitorid.disable.status=null,-1
     */

    val enable_alarm_status = props.getProperty("blacklist.enable.status")
    val disable_platecolors: Array[String] = {
        val tmp = props.getProperty("blacklist.platecolor.disable.status", "")
                .split(',').map(f => StringUtils.trimToNull(f))
        val buffer = new ListBuffer[String]()
        tmp.foreach(f => {
            if (f != null && f.length() > 0) {
                buffer += f
            }
        })
        buffer.toArray
    }

    val disable_vehiclebrands: Array[String] = {
        val tmp = props.getProperty("blacklist.vehiclebrand.disable.status", "")
                .split(',').map(f => StringUtils.trimToNull(f))
        val buffer = new ListBuffer[String]()
        tmp.foreach(f => {
            if (f != null && f.length() > 0) {
                buffer += f
            }
        })
        buffer.toArray
    }

    val disable_vehiclecolors: Array[String] = {
        val tmp = props.getProperty("blacklist.vehiclecolor.disable.status", "")
                .split(',').map(f => StringUtils.trimToNull(f))
        val buffer = new ListBuffer[String]()
        tmp.foreach(f => {
            if (f != null && f.length() > 0) {
                buffer += f
            }
        })
        buffer.toArray
    }

    val disable_vehicletypes: Array[String] = {
        val tmp = props.getProperty("blacklist.vehicletype.disable.status", "")
                .split(',').map(f => StringUtils.trimToNull(f))
        val buffer = new ListBuffer[String]()
        tmp.foreach(f => {
            if (f != null && f.length() > 0) {
                buffer += f
            }
        })
        buffer.toArray
    }

    val disable_orgids: Array[String] = {
        val tmp = props.getProperty("blacklist.orgid.disable.status", "")
                .split(',').map(f => StringUtils.trimToNull(f))
        val buffer = new ListBuffer[String]()
        tmp.foreach(f => {
            if (f != null && f.length() > 0) {
                buffer += f
            }
        })
        buffer.toArray
    }

    val disable_monitorids: Array[String] = {
        val tmp = props.getProperty("blacklist.monitorid.disable.status", "")
                .split(',').map(f => StringUtils.trimToNull(f))
        val buffer = new ListBuffer[String]()
        tmp.foreach(f => {
            if (f != null && f.length() > 0) {
                buffer += f
            }
        })
        buffer.toArray
    }

    /**
      * 通过一个指定的ID查找ID对应的布控元数据,如果查找成功,返回值,否则返回null
      */
    def fetchMetadataById(id: String, statement: Statement): (PlateNumber, Long, BlacklistBean) = {
        val sql = buildByIdQuerySql(id)
        info("begin execute sql query [%s]".format(sql))
        val rs = statement.executeQuery(sql)
        try {
            if (rs.next()) {
                val id_long = rs.getLong(1)
                val platenumber = rs.getBytes(2)
                val platecolor_bytes = rs.getBytes(3)
                val vehiclebrand_bytes = rs.getBytes(4)
                val vehiclecolor_bytes = rs.getBytes(5)
                val vehicletype_bytes = rs.getBytes(6)
                val orgid = rs.getString(7)
                val monitorid = rs.getString(8)
                val begintime = rs.getTimestamp(9).getTime()
                val endtime = rs.getTimestamp(10).getTime()
                val alarmstatus = rs.getString(11)
                val alarmtype = rs.getString(12)
                val controllevel = rs.getString(13)
                val controluser = rs.getString(14)

                val platecolor = rs.getString(3)
                val vehiclebrand = rs.getString(4)
                val vehiclecolor = rs.getString(5)
                val vehicletype = rs.getString(6)

                val enable_alarm = if (enable_alarm_status == null) true
                else enable_alarm_status.equals(alarmstatus)
                val is_platecolor = if (platecolor == null) {
                    disable_platecolors.indexWhere(p => p.equalsIgnoreCase("null")) >= 0
                } else {
                    disable_platecolors.indexWhere(p => p.equals(platecolor)) >= 0
                }
                val is_vehiclebrand = if (vehiclebrand == null) {
                    disable_vehiclebrands.indexWhere(p => p.equalsIgnoreCase("null")) >= 0
                } else {
                    disable_vehiclebrands.indexWhere(p => p.equals(vehiclebrand)) >= 0
                }
                val is_vehiclecolor = if (vehiclecolor == null) {
                    disable_vehiclecolors.indexWhere(p => p.equalsIgnoreCase("null")) >= 0
                } else {
                    disable_vehiclecolors.indexWhere(p => p.equals(vehiclecolor)) >= 0
                }
                val is_vehicletype = if (vehicletype == null) {
                    disable_vehicletypes.indexWhere(p => p.equalsIgnoreCase("null")) >= 0
                } else {
                    disable_vehicletypes.indexWhere(p => p.equals(vehicletype)) >= 0
                }
                val is_orgid = if (orgid == null) {
                    disable_orgids.indexWhere(p => p.equalsIgnoreCase("null")) >= 0
                } else {
                    val tmp = StringUtils.trimToNull(orgid)
                    if (tmp == null) {
                        disable_orgids.indexWhere(p => p.equalsIgnoreCase("null")) >= 0
                    } else {
                        disable_orgids.indexWhere(p => p.equals(tmp)) >= 0
                    }
                }

                val is_monitorid = if (monitorid == null) {
                    disable_monitorids.indexWhere(p => p.equalsIgnoreCase("null")) >= 0
                } else {
                    val tmp = StringUtils.trimToNull(monitorid)
                    if (tmp == null) {
                        disable_monitorids.indexWhere(p => p.equalsIgnoreCase("null")) >= 0
                    } else {
                        disable_monitorids.indexWhere(p => p.equals(tmp)) >= 0
                    }
                }

                val blacklist = new BlacklistBean(id_long,
                    platenumber, platecolor_bytes, vehiclebrand_bytes,
                    vehiclecolor_bytes, vehicletype_bytes, orgid, monitorid,
                    begintime, endtime, alarmstatus, alarmtype, controllevel, controluser,
                    is_platecolor, is_vehiclebrand, is_vehiclecolor, is_vehicletype,
                    is_orgid, is_monitorid, enable_alarm)
                val key = new PlateNumber(platenumber)
                if (debug) {
                    val debug = "blacklist=>id=%s,platenumber=%s,orgids=%s,monitorids=%s"
                            .format(rs.getString(1), rs.getString(2), rs.getString(7), rs.getString(8))
                    info(debug)
                }
                info("execute sql query successed, query id is %s".format(id))
                (key, id_long, blacklist)
            } else {
                null
            }
        } finally {
            rs.close()
        }
    }

    /**
      * 读取布控元数据信息,所有
      */

    import scala.collection.mutable.Map

    def fetchAllMetadata(statement: Statement): Map[PlateNumber, Map[Long, BlacklistBean]] = {
        info("begin execute sql query [%s]".format(fetchAllSql))
        val rs = statement.executeQuery(fetchAllSql)
        var size: Int = 0
        val blacklistMap = Map[PlateNumber, Map[Long, BlacklistBean]]()
        try {
            while (rs.next()) {
                val id = rs.getLong(1)
                val platenumber = rs.getBytes(2)
                val platecolor_bytes = rs.getBytes(3)
                val vehiclebrand_bytes = rs.getBytes(4)
                val vehiclecolor_bytes = rs.getBytes(5)
                val vehicletype_bytes = rs.getBytes(6)
                val orgid = rs.getString(7)
                val monitorid = rs.getString(8)
                val begintime = rs.getTimestamp(9).getTime()
                val endtime = rs.getTimestamp(10).getTime()
                val alarmstatus = rs.getString(11)
                val alarmtype = rs.getString(12)
                val controllevel = rs.getString(13)
                val controluser = rs.getString(14)

                val platecolor = rs.getString(3)
                val vehiclebrand = rs.getString(4)
                val vehiclecolor = rs.getString(5)
                val vehicletype = rs.getString(6)

                val enable_alarm = if (enable_alarm_status == null) true
                else enable_alarm_status.equals(alarmstatus)
                val is_platecolor = if (platecolor == null) {
                    disable_platecolors.indexWhere(p => p.equalsIgnoreCase("null")) >= 0
                } else {
                    disable_platecolors.indexWhere(p => p.equals(platecolor)) >= 0
                }
                val is_vehiclebrand = if (vehiclebrand == null) {
                    disable_vehiclebrands.indexWhere(p => p.equalsIgnoreCase("null")) >= 0
                } else {
                    disable_vehiclebrands.indexWhere(p => p.equals(vehiclebrand)) >= 0
                }
                val is_vehiclecolor = if (vehiclecolor == null) {
                    disable_vehiclecolors.indexWhere(p => p.equalsIgnoreCase("null")) >= 0
                } else {
                    disable_vehiclecolors.indexWhere(p => p.equals(vehiclecolor)) >= 0
                }
                val is_vehicletype = if (vehicletype == null) {
                    disable_vehicletypes.indexWhere(p => p.equalsIgnoreCase("null")) >= 0
                } else {
                    disable_vehicletypes.indexWhere(p => p.equals(vehicletype)) >= 0
                }
                val is_orgid = if (orgid == null) {
                    disable_orgids.indexWhere(p => p.equalsIgnoreCase("null")) >= 0
                } else {
                    val tmp = StringUtils.trimToNull(orgid)
                    if (tmp == null) {
                        disable_orgids.indexWhere(p => p.equalsIgnoreCase("null")) >= 0
                    } else {
                        disable_orgids.indexWhere(p => p.equals(tmp)) >= 0
                    }
                }

                val is_monitorid = if (monitorid == null) {
                    disable_monitorids.indexWhere(p => p.equalsIgnoreCase("null")) >= 0
                } else {
                    val tmp = StringUtils.trimToNull(monitorid)
                    if (tmp == null) {
                        disable_monitorids.indexWhere(p => p.equalsIgnoreCase("null")) >= 0
                    } else {
                        disable_monitorids.indexWhere(p => p.equals(tmp)) >= 0
                    }
                }

                val blacklist = new BlacklistBean(id,
                    platenumber, platecolor_bytes, vehiclebrand_bytes,
                    vehiclecolor_bytes, vehicletype_bytes, orgid, monitorid,
                    begintime, endtime, alarmstatus, alarmtype, controllevel, controluser,
                    is_platecolor, is_vehiclebrand, is_vehiclecolor, is_vehicletype,
                    is_orgid, is_monitorid, enable_alarm)
                val key = new PlateNumber(platenumber)
                if (blacklistMap.keySet.exists(p => p.equals(key))) {
                    blacklistMap.get(key).get += id -> blacklist
                } else {
                    val values = Map[Long, BlacklistBean]()
                    values += id -> blacklist
                    blacklistMap += key -> values
                }
                if (debug) {
                    val debug = "blacklist=>id=%s,platenumber=%s,orgids=%s,monitorids=%s"
                            .format(rs.getString(1), rs.getString(2), rs.getString(7), rs.getString(8))
                    info(debug)
                }
                size += 1
            }
            info("execute sql query successed, query size is %d".format(size))
        } finally {
            rs.close()
        }
        blacklistMap
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

    def buildByIdQuerySql(id: String): String = {
        fetchByIdSql.replace("@id@", id)
    }

    private def initByIdSql(): String = {
        val buf = fetchAllSql + (" WHERE %s='@id@'".format(cf.id))
        buf
    }

    private def initAllSql(): String = {
        val buf = "SELECT %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s FROM %s"
                .format(cf.id, cf.platenumber, cf.platecolor,
                    cf.vehiclebrand, cf.vehiclecolor, cf.vehicletype,
                    cf.orgid, cf.monitorid, cf.begintime, cf.endtime,
                    cf.alarmstatus, cf.alarmtype, cf.controllevel, cf.controluser, cf.table_name)

        buf
    }

}

case class PlateNumber(val bytes: Array[Byte]) {
    override def toString(): String =
        "checkpoint=%s".format(new String(bytes))

    override def equals(any: Any): Boolean = {
        any match {
            case that: PlateNumber => ByteUtils.equals(bytes, that.bytes)
            case _ => false
        }
    }

    override def hashCode(): Int = Arrays.hashCode(bytes)

    def getPlateNumber(): String = new String(bytes)
}
