package com.netposa.poseidon.fakecardetect.main

import java.io.IOException
import java.util.Properties

import com.netposa.poseidon.fakecardetect.utils.LoadInitUtil

object FakeCarDetectMainClient {
  @throws[IOException]
  def main(args: Array[String]): Unit = {
    var submitArgs = ""
    val prop = LoadInitUtil.initProps("spark.properties")
    submitArgs = prop.getProperty("sparkStr") + args(1)
    val spark_home = System.getenv("SPARK_HOME")
    System.out.println(spark_home)
    val execStr = "bash -l " + spark_home + "/bin/" + "spark-class " + "org.apache.spark.deploy.SparkSubmit " + submitArgs
    System.out.println("execStr" + execStr)
    val process = Runtime.getRuntime.exec(execStr)
    var result = -1
    try
      result = process.waitFor
    catch {
      case e: InterruptedException =>
        e.printStackTrace()
    }
  }
}
