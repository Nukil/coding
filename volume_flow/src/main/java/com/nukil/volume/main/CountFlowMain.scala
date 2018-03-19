package com.nukil.volume.main

import java.io.File
import java.util.Properties

import com.nukil.volume.service.{SaveData2Mysql, WriteFile2Disk}
import com.nukil.volume.util.LoadPropers
import org.slf4j.{Logger, LoggerFactory}

object CountFlowMain {

    val LOG: Logger = LoggerFactory.getLogger(CountFlowMain.getClass)
    val PROPS: Properties = LoadPropers.getProperties()

    def main(args: Array[String]): Unit = {
        moveHistoryDataFile()
        val fetchMessageThread: FetchMessageThread = new FetchMessageThread(PROPS)
        val saveData2Mysql: SaveData2Mysql = SaveData2Mysql.getInstance()
        val writeFile2Disk: WriteFile2Disk = WriteFile2Disk.getInstance()

        writeFile2Disk.start()
        saveData2Mysql.start()
        fetchMessageThread.start()

        Runtime.getRuntime.addShutdownHook(new Thread {
            override def run(): Unit = {
                saveData2Mysql.shutDown()
                writeFile2Disk.shutDown()
                fetchMessageThread.shutDown()
            }
        })

        while (true) {
            try {
                Thread.sleep(3600000)
            } catch {
                case _: Throwable =>
            }
        }
    }
    def moveHistoryDataFile(): Unit = {
        try {
            val srcDirPath = PROPS.getProperty("save.data.file.path", "./data/")
            val destDirPath = PROPS.getProperty("save.history.data.file.path", "./history_data/")
            val srcFileDir = new File(srcDirPath)
            val destFileDir = new File(destDirPath)
            if (!destFileDir.exists()) {
                LOG.info("create history data file dir")
                destFileDir.mkdirs()
            }
            if (srcFileDir.exists()) {
                val fileList = srcFileDir.listFiles()
                fileList.foreach(file => {
                    val destFile = new File(destDirPath + file.getName)
                    if (!destFile.exists()) {
                        destFile.createNewFile()
                        file.delete()
                        LOG.info("move file 【%s】".format(file.getName))
                    }
                })
            }
        } catch {
            case e: Exception =>
                LOG.error(e.getMessage, e)
        }
    }
}
