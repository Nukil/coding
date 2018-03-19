package netposa.blacklist.rpc

import kafka.utils.Logging
import netposa.realtime.blacklist.server.LoadControlUtils
import java.sql.Connection
import java.sql.Statement
import netposa.realtime.blacklist.server.FetchMessageThread
import netposa.realtime.blacklist.bean.BlacklistBean
/**
 * 接收布控信息修改的RPC服务的接口具体实现函数
 */
class ChangeMetadataServiceHandler(val control_utils: LoadControlUtils,
  val fetchThred: FetchMessageThread)
  extends Logging with ChangeMetadataService.Iface {
  val max_attemp: Int = 6
  val retry_interval: Int = 2000
  //初始化数据库连接
  var connection: Connection = null

  /**
   * 接收布控信息更新的RPC接口实现,此函数中通过连接数据库并读取更新的数据
   * ////
   */
  override def sendRequest(request: Request): Response = {

    val response = new Response()
    var attemp: Int = 0
    while (attemp < max_attemp) {
      var statement: Statement = null
      try {
        init
        statement = connection.createStatement()
        val result = control_utils.fetchMetadataById(request.control_id, statement)
        if (result == null) {
          warn("get control metadata is null by id is %s".format(request.control_id))
        } else {
          fetchThred.blacklistMap.synchronized({
            if (fetchThred.blacklistMap.keySet.exists(p => p.equals(result._1))) {
              fetchThred.blacklistMap.get(result._1).get += result._2 -> result._3
            } else {
              import scala.collection.mutable.Map
              val values = Map[Long, BlacklistBean]()
              values += result._2 -> result._3
              fetchThred.blacklistMap += result._1 -> values
            }
          })
        }
        attemp = max_attemp
        response.flag = true
        response.err_msg = "success"
      } catch {
        case e: Throwable => {
          control_utils.closeStatement(statement)
          control_utils.closeConnection(connection)
          statement = null
          connection = null
          val msg = "get control metadata id is %s, error => %s"
            .format(request.control_id, e.getMessage())
          warn(msg, e)
          info("sleep this thread ms is %d, attemp number is %d".format(retry_interval, attemp))
          Thread.sleep(retry_interval)
          attemp += 1
          response.flag = false
          response.err_msg = msg
        }
      } finally {
        control_utils.closeStatement(statement)
        statement = null
      }
    }
    response
  }

  def shutdown() {
    control_utils.closeConnection(connection)
    connection = null
  }

  private def init() = {
    if (null == connection) {
      val tmp: Connection = try {
        control_utils.initConnection
      } catch {
        case e: Throwable =>
          warn("init control db connection error => %s".format(e.getMessage()), e)
          null
      }
      connection = tmp
    } //end if
  }
}
