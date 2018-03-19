package netposa.realtime.blacklist.bean

import java.io.InputStream
import org.apache.commons.lang.StringUtils
import scala.xml.XML
/**
 * 布控表定义信息
 */
class ControlConfBean(val inStream: InputStream) {
  //数据库连接信息
  var db_class: String = null
  var db_url: String = null
  var db_user: String = null
  var db_pwd: String = null
  //表名称定义
  var table_name: String = null
  //表字段定义
  var id: String = null
  var platenumber: String = null
  var platecolor: String = null
  var vehiclebrand: String = null
  var vehiclecolor: String = null
  var vehicletype: String = null
  var orgid: String = null
  var monitorid: String = null
  var begintime: String = null
  var endtime: String = null
  var alarmstatus: String = null
  var alarmtype: String = null
  var controllevel: String = null
  var controluser: String = null

  init

  private def init() {
    try {
      val conf = XML.load(inStream)
      //解析数据库连接信息
      (conf \ "connection" \\ "_").map(e => {
        e.label match {
          case "driverClass" => db_class = StringUtils.trimToNull(e.text)
          case "url" => db_url = StringUtils.trimToNull(e.text)
          case "user" => db_user = StringUtils.trimToNull(e.text)
          case "password" => db_pwd = {
            val tmp = StringUtils.trimToNull(e.text)
            if ("null".equalsIgnoreCase(tmp)) null else tmp
          }
          case _ =>
        }
      })
      if (db_class == null || db_url == null || db_user == null) {
        throw new Exception("vim control db connection info can not be config")
      }
      //解析表名称
      table_name = StringUtils.trimToNull((conf \ "table" \ "name").text)
      if (table_name == null) {
        throw new Exception("vim control  table name info can not be config")
      }
      //解析表字段信息
      (conf \ "fields" \\ "_").map(e => {
        e.label match {
          case "id" => id = StringUtils.trimToNull((e \ "field").text)
          case "platenumber" => platenumber = StringUtils.trimToNull((e \ "field").text)
          case "platecolor" => platecolor = StringUtils.trimToNull((e \ "field").text)
          case "vehiclebrand" => vehiclebrand = StringUtils.trimToNull((e \ "field").text)
          case "vehiclecolor" => vehiclecolor = StringUtils.trimToNull((e \ "field").text)
          case "vehicletype" => vehicletype = StringUtils.trimToNull((e \ "field").text)
          case "orgid" => orgid = StringUtils.trimToNull((e \ "field").text)
          case "monitorid" => monitorid = StringUtils.trimToNull((e \ "field").text)
          case "begintime" => begintime = StringUtils.trimToNull((e \ "field").text)
          case "endtime" => endtime = StringUtils.trimToNull((e \ "field").text)
          case "alarmstatus" => alarmstatus = StringUtils.trimToNull((e \ "field").text)
          case "alarmtype" => alarmtype = StringUtils.trimToNull((e \ "field").text)
          case "controllevel" => controllevel = StringUtils.trimToNull((e \ "field").text)
          case "controluser" => controluser = StringUtils.trimToNull((e \ "field").text)
          case _ =>
        }
      })
      if (id == null || platenumber == null || platecolor == null
        || vehiclebrand == null || vehiclecolor == null
        || vehicletype == null || (orgid == null && monitorid == null) || begintime == null
        || endtime == null || alarmstatus == null
        || alarmtype == null || controllevel == null || controluser == null) {
        throw new Exception("vim control table fields info can not be config")
      }
    } finally {
      try {
        inStream.close()
      } catch {
        case _: Throwable =>
      }
    } //end try finally
  }

  override def toString(): String = {
    s"connections:{db_class=${db_class},db_url=${db_url},db_user=${db_user},db_pwd=${db_pwd}}," +
      s"table:{${table_name}},fields:{id=${id},platenumber=${platenumber},platecolor=${platecolor}," +
      s"vehiclebrand=${vehiclebrand},vehiclecolor=${vehiclecolor},vehicletype=${vehicletype}," +
      s"orgid=${orgid},monitorid=${monitorid},begintime=${begintime},endtime=${endtime},alarmstatus=${alarmstatus}," +
      s"alarmtype=${alarmtype},controllevel=${controllevel},controluser=${controluser}}"
  }

}
