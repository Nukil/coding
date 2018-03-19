package netposa.righttravel.analysis.utils

import org.apache.spark.Partitioner
import netposa.righttravel.analysis.lib.db.OpaqInputKey

class OpaqInputKeyPartitioner(partitions: Int) extends Partitioner {

  def numPartitions = partitions

  def getPartition(key: Any): Int = key match {
    case null => 0
    case _ => {
      val newkey = key.asInstanceOf[OpaqInputKey]

      val rawMod = math.abs(newkey.hashCode()) % numPartitions
      //rawMod + (if (rawMod < 0) numPartitions else 0)
      rawMod
    }
  }

}
