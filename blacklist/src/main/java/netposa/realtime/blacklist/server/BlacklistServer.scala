package netposa.realtime.blacklist.server

import java.util.Properties

import scala.collection.JavaConversions.asScalaSet

import org.apache.commons.lang.StringUtils

import kafka.utils.Logging
import netposa.blacklist.rpc.BlacklistMetadataServer
import netposa.realtime.blacklist.bean.CheckpointConfBean
import netposa.realtime.blacklist.bean.ControlConfBean
import netposa.realtime.kafka.utils.ThreadExceptionHandler

/**
  * 黑名单布控入口程序
  */
object BlacklistServer extends Logging {
    val props = initProps
    val control_conf = initControlConf
    val checkpoint_conf = initCheckpointConf

    def main(args: Array[String]): Unit = {
        //生成布控操作工具类
        val control_utils: LoadControlUtils = new LoadControlUtils(control_conf, props)
        //加载KAFKA消息监听器
        val fetchMessageThread = new FetchMessageThread(props, checkpoint_conf, control_utils)
        fetchMessageThread.stoped = false
        fetchMessageThread.setDaemon(true)
        fetchMessageThread.setUncaughtExceptionHandler(new ThreadExceptionHandler())
        fetchMessageThread.start()
        //生成RPC SERVER实例
        val rpcServer = new BlacklistMetadataServer(control_utils, fetchMessageThread, args)

        Runtime.getRuntime().addShutdownHook(new Thread() {
            override def run() = {
                fetchMessageThread.shutdown
                rpcServer.shutdown
            }
        })

        //start rpc server
        rpcServer.run

        //stop all service
        fetchMessageThread.shutdown
        rpcServer.shutdown
    }

    private def initCheckpointConf(): CheckpointConfBean = {
        val class_loader = this.getClass().getClassLoader()
        val checkpoint_conf_stream = class_loader.getResourceAsStream("checkpoint_conf.xml")
        val checkpoint_conf = new CheckpointConfBean(checkpoint_conf_stream)
        info("load checkpoint_conf.xml by class path message is %s".format(checkpoint_conf.toString))
        checkpoint_conf
    }

    private def initControlConf(): ControlConfBean = {
        val class_loader = this.getClass().getClassLoader()
        val control_conf_stream = class_loader.getResourceAsStream("control_conf.xml")
        val control_conf = new ControlConfBean(control_conf_stream)
        info("load control_conf.xml by class path message is %s".format(control_conf.toString))
        control_conf
    }

    private def initProps(): Properties = {
        val class_loader = this.getClass().getClassLoader()
        info("load server.properties config file by class path")
        val tmp_props = new Properties()
        val server_conf_stream = class_loader.getResourceAsStream("server.properties")
        try {
            tmp_props.load(server_conf_stream)
        } finally {
            try {
                server_conf_stream.close()
            } catch {
                case _: Throwable =>
            }
        }

        val tmp = tmp_props.keySet().map(key => {
            val value = StringUtils.trimToNull(tmp_props.getProperty(key.toString()))
            info("config server properties key is %s value is %s".format(key.toString(), value))
            (key.toString(), value)
        }).toMap

        val server_props = new Properties()
        tmp.foreach(row => server_props.put(row._1, row._2))
        server_props
    }

}