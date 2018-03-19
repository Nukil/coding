package netposa.righttravel.analysis.utils

import com.esotericsoftware.kryo.Kryo
import org.apache.spark.serializer.KryoRegistrator
import netposa.righttravel.analysis.lib.db.OpaqInputKey
import netposa.righttravel.analysis.lib.db.OpaqInputValue
import netposa.righttravel.analysis.lib.db.RightTravelOutputValue

class AnalysisRegistrator extends KryoRegistrator {
  
  override def registerClasses(kryo:Kryo) {
     kryo.register(classOf[OpaqInputKey])
     kryo.register(classOf[OpaqInputValue])
     kryo.register(classOf[RightTravelOutputValue])
  }

}