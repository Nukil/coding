package org.apache.spark.streaming.rdd

import com.esotericsoftware.kryo.io.Input
import com.netposa.poseidon.fakecardetect.bean.InputMsg
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.rdd.RDD
import org.apache.spark.serializer.KryoSerializer
import org.apache.spark.streaming.State
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.reflect.ClassTag

object CheckPoint {
  def getStateRDD(sparkContext: SparkContext,checkpointDirectory:String):RDD[(String,InputMsg)]= {
    val hdfsData = objectFileKryo[(String, State[InputMsg])](checkpointDirectory, sparkContext.getConf)
    val statRDD = sparkContext.parallelize(hdfsData, 10)
      .map(v =>
        if (v._2.isInstanceOf[State[InputMsg]]) {
          v._1 -> (v._2.asInstanceOf[State[InputMsg]].get(), v._3)
        } else null
      ).filter(_ != null)
      .reduceByKey((o, n) => {
        if (o._2 > n._2) {
          o
        } else n
      }).map(v => v._1 -> v._2._1)
    statRDD
  }
  private def objectFileKryo[T](path: String, sc: SparkConf)(implicit ct: ClassTag[T]) = {
    val kryoSerializer = new KryoSerializer(sc)
    val hadoopConf = new org.apache.hadoop.conf.Configuration()
    hadoopConf.set("hadoop.proxyuser.root.hosts", "*")
    hadoopConf.set("hadoop.proxyuser.root.groups", "*")

    val hdfs = org.apache.hadoop.fs.FileSystem.get(hadoopConf)

    val s = new org.apache.hadoop.fs.Path(path)
    val paths = getlistPath(hdfs, s).filter(_.getName.contains("part")) //.filter(p=>p)//getPath(hdfs, new org.apache.hadoop.fs.Path(path))
    val d = paths.flatMap { p => {
      val r = hdfs.open(p)

      var by = ArrayBuffer[Byte]()

      while (r.available() > 0) {

        val b = r.readByte()

        by += (b)

      }

      val kryo = kryoSerializer.newKryo()

      val input = new Input()

      input.setBuffer(by.toArray)

      val array = ArrayBuffer[(String, State[InputMsg], Long)]()

      while (input.available() > 0) {
        val data = kryo.readClassAndObject(input)
        if (data.isInstanceOf[org.apache.spark.streaming.rdd.MapWithStateRDDRecord[String, State[InputMsg], mutable.Seq[T]]]) {
          val stat = data.asInstanceOf[org.apache.spark.streaming.rdd.MapWithStateRDDRecord[String, State[InputMsg], mutable.Seq[T]]]
          stat.stateMap.getAll().foreach(v => {
            array += v
          })
        }
      }
      array
    }
    }
    //删除历史checkpoint
    hdfs.delete(s,true)
    d
  }

  def getlistPath(fs:FileSystem,path:Path):List[Path]={
    val paths = ListBuffer[Path]()
    if(!fs.isDirectory(path)) return paths.toList
    getFiles(fs:FileSystem,path:Path)
    def getFiles(fs:FileSystem,path:Path):Unit={
      val f = fs.listStatus(path)
      f.foreach { file => {
        if(file.isDirectory()){
          getFiles(fs,file.getPath)
        }else{
          paths += file.getPath
        }
      }
      }}
    paths.toList
  }
}
