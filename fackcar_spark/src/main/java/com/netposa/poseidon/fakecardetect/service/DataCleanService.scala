package com.netposa.poseidon.fakecardetect.service

import com.netposa.poseidon.fakecardetect.bean.InputMsg
import com.netposa.poseidon.fakecardetect.main.FakeCarDetectMainServer.{childLogoDic, logoDic, propConf}

class DataCleanService {

  def clean(inputMsg: InputMsg):(InputMsg,Boolean)={
    //二次识别车牌不存在
    if(inputMsg.getRcgHphm()==null || inputMsg.getRcgHphm().isEmpty){
      return (inputMsg,false)
    }
    //号牌颜色为空
    if(inputMsg.getHpys()==null || inputMsg.getHpys().isEmpty){
      return (inputMsg ,false)
    }
    //车头车尾为空
    if(inputMsg.getHeadRear() == null || inputMsg.getHeadRear().isEmpty){
      return (inputMsg ,false)
    }
    //子品牌为空
    if(inputMsg.getClzpp()== null || inputMsg.getClzpp().isEmpty){
      return (inputMsg,false)
    }
    //特征为空
    if(inputMsg.getFeature()==null || inputMsg.getFeature().length==0){
      return (inputMsg,false)
    }
//    println("rcg------------before------>"+inputMsg.getRcgHphm())
    //0000过滤
    if(inputMsg.getRcgHphm().equals(propConf.exclude_plate_startwiths)){
      return (inputMsg,false)
    }
//    println("rcg------------after------>"+inputMsg.getRcgHphm())
    //本地车牌过滤
    if(propConf.local_plate_startwith!="-1"){
      if(!inputMsg.getRcgHphm().contains(propConf.local_plate_startwith)){
        return (inputMsg,false)
      }
    }
    //品牌置信度过滤
    if(propConf.logo_confidence > 0 && inputMsg.getClppConf() > 0){
      if(propConf.logo_confidence > inputMsg.getClppConf()){
        return (inputMsg,false)
     }
    }
    //整体置信度过滤
    if(propConf.whole_confidence >0 && inputMsg.getConf() >0){
      if(propConf.whole_confidence > inputMsg.getConf()){
        return (inputMsg ,false)
      }
    }

    //车牌置信度过滤
    if(propConf.plate_confidence >0 && inputMsg.getPlateConf()>0){
      if(propConf.plate_confidence > inputMsg.getPlateConf()){
        return (inputMsg,false)
      }
    }
    //夜间数据过滤
    if(propConf.night_clean_switch){
      if(inputMsg.getJgxs()>0){
        if(inputMsg.getJgxs()>propConf.night_start || inputMsg.getJgxs() < propConf.night_end){
          return (inputMsg , false)
        }
      }
    }
    //特殊车牌过滤，采用正则表达式

    //-----------------------

    //车牌颜色过滤
    if(propConf.plate_color_clean != -1){
      if(propConf.plate_color_clean == inputMsg.getHpys().toInt){
        return  (inputMsg, false)
      }
    }
    //车头车尾未识别过滤
    if(!inputMsg.getHeadRear().equalsIgnoreCase("head") && !inputMsg.getHeadRear().equalsIgnoreCase("end")){
      return (inputMsg,false)
    }
    //图像完整性过滤
    if(propConf.aspect_ratio_max != -1 || propConf.aspect_ratio_min != -1){
      if(inputMsg.getAspectRatio()<propConf.aspect_ratio_min || inputMsg.getAspectRatio() > propConf.aspect_ratio_max){
        return (inputMsg ,false)
      }
    }

    //车辆品牌过滤
    if(propConf.logo_clean(0)!="-1"){
      propConf.logo_clean.foreach(l =>{
        if(logoDic.get(l).isEmpty){
          //比较品牌名称
          if(logoDic.get(inputMsg.getClpp()).get == l){
            return (inputMsg ,false)
          }
        }else{
          //比较字典
          if(inputMsg.getClpp() == l){
            return (inputMsg,false)
          }
        }
      })

    }
    //车辆子品牌过滤
    if(propConf.sub_logo_clean(0)!="-1"){
      propConf.sub_logo_clean.foreach(sl=>{
        if(childLogoDic.get(sl).isEmpty){
          //比较子品牌名
          if(childLogoDic.get(inputMsg.getClzpp()).get == sl){
            return (inputMsg ,false)
          }
        }else{
          //比较字典
          if(inputMsg.getClzpp() == sl){
            return (inputMsg ,false)
          }
        }
      })
    }
    //过滤结束
    (inputMsg,true)
  }

}
