package com.netposa.poseidon.fakecardetect.bean

import java.util.Properties
import com.netposa.poseidon.fakecardetect.main.FakeCarDetectMainServer.LOG

class PropConfig() {
  var exclude_plate_startwiths:String = "0000"
  var local_plate_startwith:String = null
  var logo_confidence:Float = -1
  var whole_confidence:Float = -1
  var plate_confidence:Float = -1
  var night_clean_switch:Boolean = true
  var night_start:Int = 19
  var night_end:Int = 6
  var plate_color_clean:Int = -1
  var aspect_ratio_min:Int = -1
  var aspect_ratio_max: Int = -1
  var logo_clean:Array[String] = Array[String]()
  var sub_logo_clean:Array[String] = Array[String]()
  var plate_similarity_threshold:Float = -1
  var compare_strategy:Int = 2
  var feature_similar_threshold:Int = 80
  var compare_vehicle_year = true
  var compare_vehicle_type = true
  var compare_vehicle_color = true
  var feature_bytes_size = 16
  var feature_version_size = 16
  var feature_size = 256
  var algorithm_type= "glst"
  var dumpPeriodTime = 60000l
  var checkpointDir = "hdfs://node208:8020/spark/checkpoint_666/"
  def initConfig(prop:Properties):Unit={
    try{
      exclude_plate_startwiths = prop.getProperty("exclude_plate_startwith","0000")
      local_plate_startwith = prop.getProperty("local_plate_startwith","-1")
      logo_confidence = prop.getProperty("logo_confidence","-1").toFloat
      whole_confidence = prop.getProperty("whole_confidence","-1").toFloat
      plate_confidence = prop.getProperty("plate_confidence","-1").toFloat
      night_clean_switch = prop.getProperty("night_clean_switch","true").toBoolean
      night_start = prop.getProperty("night_start","19").toInt
      night_end = prop.getProperty("night_end","6").toInt
      plate_color_clean = prop.getProperty("plate_color_clean","-1").toInt
      aspect_ratio_min = prop.getProperty("aspect_ratio_min","-1").toInt
      aspect_ratio_max = prop.getProperty("aspect_ratio_max","-1").toInt
      logo_clean  = prop.getProperty("logo_clean","-1").split(",")
      sub_logo_clean = prop.getProperty("sub_logo_clean","-1").split(",")
      plate_similarity_threshold = prop.getProperty("plate_similarity_threshold","-1").toFloat
      compare_strategy = prop.getProperty("compare_strategy","2").toInt
      feature_similar_threshold = prop.getProperty("feature_similar_threshold","80").toInt
      compare_vehicle_year = prop.getProperty("compare_vehicle_year","true").toBoolean
      compare_vehicle_type = prop.getProperty("compare_vehicle_type","true").toBoolean
      compare_vehicle_color = prop.getProperty("compare_vehicle_color","true").toBoolean
      feature_bytes_size =  prop.getProperty("feature_bytes_size","16").toInt
      feature_version_size =  prop.getProperty("feature_version_size","16").toInt
      feature_size =  prop.getProperty("feature_size","256").toInt
      algorithm_type =  prop.getProperty("algorithm_type","glst")
      dumpPeriodTime= prop.getProperty("dumpPeriodTime","60000").toLong
      checkpointDir = prop.getProperty("checkpointDir","hdfs://node208:8020/spark/checkpoint_666/")
    }catch {
      case e:Exception=>{
        LOG.info("Load proper error please check server.properties,info is %s".format(e.getMessage))
      }
    }


  }

}
