package netposa.realtime.blacklist.bean

import java.io.InputStream
import org.apache.commons.lang.StringUtils
import scala.xml.XML

/**
 * 卡口数据表定义
 */
class CheckpointConfBean(val inStream: InputStream) {
  //数据库连接信息
  var db_class: String = null
  var db_url: String = null
  var db_user: String = null
  var db_pwd: String = null
  //表名称定义
  var table_name: String = null
  //字段定义信息
  var checkpoint: String = null
  var orgid: String = null
  var longitude: String = null
  var latitude: String = null

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
        throw new Exception("db connection info can not be config")
      }
      //解析表名称
      table_name = StringUtils.trimToNull((conf \ "table" \ "name").text)
      if (table_name == null) {
        throw new Exception("checkpoint table name info can not be config")
      }
      //解析字段信息
      (conf \ "fields" \\ "_").map(e => {
        e.label match {
          case "checkpoint" => checkpoint = StringUtils.trimToNull((e \ "field").text)
          case "orgid" => orgid = StringUtils.trimToNull((e \ "field").text)
          case "longitude" => longitude = StringUtils.trimToNull((e \ "field").text)
          case "latitude" => latitude = StringUtils.trimToNull((e \ "field").text)
          case _ =>
        }
      })
      if (checkpoint == null || orgid == null
        || longitude == null || latitude == null) {
        throw new Exception("checkpoint table fields info can not be config")
      }
    } finally {
      try {
        inStream.close()
      } catch {
        case _: Throwable =>
      }
    } //end try finally

  } //end init method

  override def toString(): String = {
    s"connections:{db_class=${db_class},db_url=${db_url},db_user=${db_user},db_pwd=${db_pwd}}," +
      s"table:{${table_name}},fields:{checkpoint=${checkpoint},orgid=${orgid}," +
      s"longitude=${longitude},latitude=${latitude}}"
  }
}