package com.netposa.poseidon.face.init

import java.util.Properties

import scala.collection.JavaConversions.asScalaSet
import org.apache.commons.lang.StringUtils
import org.slf4j.{Logger, LoggerFactory}
import com.netposa.poseidon.face.init.LoadPropers.LOG


/**
  * 加载配置文件为java.util.Properties对象
  * 将该类修改为单例模式
  */
class LoadPropers private() {
    def load(): Properties = {
        val class_loader = this.getClass.getClassLoader
        LOG.info("load server.properties config file by class path")
        val tmp_props = new Properties()
        val server_conf_stream = class_loader.getResourceAsStream("face-server.properties")
        try {
            tmp_props.load(server_conf_stream)
            val tmp = tmp_props.keySet().map(key => {
                val value = StringUtils.trimToNull(tmp_props.getProperty(key.toString))
                LOG.info("config server properties key is %s value is %s".format(key.toString, value))
                (key.toString, value)
            }).toMap

            val server_props = new Properties()
            tmp.foreach(row => {
                if (row._1 != null && row._2 != null) {
                    server_props.put(row._1, row._2)
                }
            })
            server_props
        } finally {
            try {
                server_conf_stream.close()
            } catch {
                case _: Throwable =>
            }
        }
    }
    def load(fileName: String): Properties = {
        val class_loader = this.getClass.getClassLoader
        LOG.info("load server.properties config file by class path")
        val tmp_props = new Properties()
        val server_conf_stream = class_loader.getResourceAsStream(fileName)
        try {
            tmp_props.load(server_conf_stream)
            val tmp = tmp_props.keySet().map(key => {
                val value = StringUtils.trimToNull(tmp_props.getProperty(key.toString))
                LOG.info("config server properties key is %s value is %s".format(key.toString, value))
                (key.toString, value)
            }).toMap

            val server_props = new Properties()
            tmp.foreach(row => {
                if (row._1 != null && row._2 != null) {
                    server_props.put(row._1, row._2)
                }
            })
            server_props
        } finally {
            try {
                server_conf_stream.close()
            } catch {
                case _: Throwable =>
            }
        }
    }
}

//修改成单例
object LoadPropers {
    val LOG: Logger = LoggerFactory.getLogger(LoadPropers.getClass)
    var properties: Properties = _

    def getProperties(): Properties = {
        if (properties == null) {
            properties = new LoadPropers().load()
        }
        properties
    }
    def getProperties(fileName: String): Properties = {
        new LoadPropers().load(fileName + ".properties")
    }
}