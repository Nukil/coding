package netposa.fakecar.feature.rpc.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Properties;

import netposa.fakecar.feature.rpc.FeatureCalculatorServiceImpl;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用于解析
 * @author hongs.yang
 *
 */
public class FeatureJsonParseUtils {
  private static final Logger LOG = LoggerFactory.getLogger(FeatureJsonParseUtils.class);
  
  private static final Charset charset = Charset.forName("UTF-8");
  
  private static final byte EMPTY_CHAR = ' ';
  private static final byte JSON_CHAR = ':';
  private static final byte JSON_STOP_CHAR = ',';
  private static final byte JSON_END_CHAR = '}';
  private static int buffer_json_size;
  private static boolean process_debug;
  private static final byte[] height_key = "\"ImageHeight\"".getBytes(charset);
  private static final byte[] wight_key = "\"ImageWidth\"".getBytes(charset);
  private static final byte[] feature_type_key = "\"FeatType\"".getBytes(charset);
  private static final byte[] vehicle_face_key = "\"VehicleFace\"".getBytes(charset);
  private static final byte[] bottom_key = "\"bottom\"".getBytes(charset);
  private static final byte[] left_key = "\"left\"".getBytes(charset);
  private static final byte[] right_key = "\"right\"".getBytes(charset);
  private static final byte[] top_key = "\"top\"".getBytes(charset);
  
  static {
    InputStream inStream = null;
    try {
      LOG.info("handler load config properties file by class path");
      inStream = FeatureJsonParseUtils.class.getClassLoader()
          .getResourceAsStream(FeatureCalculatorServiceImpl.conf_name);

      Properties props = new Properties();
      props.load(inStream);
      
     // fakecar.feature.buffer.json.size
      String tmp = StringUtils.trimToNull(props
          .getProperty("fakecar.feature.buffer.json.size"));
      if (null != tmp) {
        LOG.info(String.format(
            "bolt load fakecar.feature.buffer.json.size values is %s",
            tmp));
        buffer_json_size = Integer.parseInt(tmp);
      } else {
        buffer_json_size = 512;
        LOG.info("bolt load fakecar.feature.buffer.json.size values is 512 skip this config");
      }
      
      tmp = StringUtils.trimToNull(props.getProperty(
          "task.process.calculator.debug", "false"));

      if (null != tmp) {
        LOG.info(String.format(
            "bolt load task.process.calculator.debug values is %s",
            tmp));
        process_debug = Boolean.parseBoolean(tmp);
      } else {
        process_debug = false;
        LOG.info("bolt load task.process.calculator.debug values is null set default value is false");
      }
    } catch (IOException e) {
      LOG.warn("load stream error => " + e.getMessage(), e);
    }finally {
      if (null != inStream) {
        try {
          inStream.close();
        } catch (IOException e) {
          LOG.warn("close stream error => " + e.getMessage(), e);
        }
      }
    }
  }
  
  
  /**
   * 得到特征BUFFER中JSON部分车脸的位置信息,返回一个坐标值,top,bottom,left,right
   * @param array
   * @return
   */
  public static float parseVehicleFacePoint(byte[] array) {
    int index = findVehicleFaceIndex(array);
    if (index < 0) {
      return -100;
    }
    
    //==================================================================//
    
    //查找bottom的值
    int tmp_index = indexOf(array,index,bottom_key);
    if (tmp_index < 0) {
      return -100;
    }
    tmp_index = tmp_index + bottom_key.length;
    //查找值的开始位置
    for(int i=tmp_index; i<buffer_json_size; i++) {
      if (array[i] == EMPTY_CHAR || array[i] == JSON_CHAR) {
        continue;
      }
      tmp_index = i;
      break;
    }
    int length = 0;
    //查找值的结束位置
    for(int i=tmp_index; i<buffer_json_size; i++) {
      if (array[i] == EMPTY_CHAR || array[i] == JSON_STOP_CHAR 
          || array[i] == JSON_END_CHAR) {
        break;
      }
      length += 1;
    }
    int bottom = ByteUtils.parseInt(array, tmp_index, length);
    if (process_debug) {
      LOG.info("parse buffer json data VehicleFace bottom is " + bottom);
    }
    
    
    //==================================================================//
    
    //查找left的值
    tmp_index = indexOf(array,index,left_key);
    if (tmp_index < 0) {
      return -100;
    }
    tmp_index = tmp_index + left_key.length;
    //查找值的开始位置
    for(int i=tmp_index; i<buffer_json_size; i++) {
      if (array[i] == EMPTY_CHAR || array[i] == JSON_CHAR) {
        continue;
      }
      tmp_index = i;
      break;
    }
    length = 0;
    //查找值的结束位置
    for(int i=tmp_index; i<buffer_json_size; i++) {
      if (array[i] == EMPTY_CHAR || array[i] == JSON_STOP_CHAR 
          || array[i] == JSON_END_CHAR) {
        break;
      }
      length += 1;
    }
    int left = ByteUtils.parseInt(array, tmp_index, length);
    if (process_debug) {
      LOG.info("parse buffer json data VehicleFace left is " + left);
    }
    
   //==================================================================//
    
    //查找right的值
    tmp_index = indexOf(array,index,right_key);
    if (tmp_index < 0) {
      return -100;
    }
    tmp_index = tmp_index + right_key.length;
    //查找值的开始位置
    for(int i=tmp_index; i<buffer_json_size; i++) {
      if (array[i] == EMPTY_CHAR || array[i] == JSON_CHAR) {
        continue;
      }
      tmp_index = i;
      break;
    }
    length = 0;
    //查找值的结束位置
    for(int i=tmp_index; i<buffer_json_size; i++) {
      if (array[i] == EMPTY_CHAR || array[i] == JSON_STOP_CHAR 
          || array[i] == JSON_END_CHAR) {
        break;
      }
      length += 1;
    }
    int right = ByteUtils.parseInt(array, tmp_index, length);
    if (process_debug) {
      LOG.info("parse buffer json data VehicleFace right is " + right);
    }
    
   //==================================================================//
    
    //查找top的值
    tmp_index = indexOf(array,index,top_key);
    if (tmp_index < 0) {
      return -100;
    }
    tmp_index = tmp_index + top_key.length;
    //查找值的开始位置
    for(int i=tmp_index; i<buffer_json_size; i++) {
      if (array[i] == EMPTY_CHAR || array[i] == JSON_CHAR) {
        continue;
      }
      tmp_index = i;
      break;
    }
    length = 0;
    //查找值的结束位置
    for(int i=tmp_index; i<buffer_json_size; i++) {
      if (array[i] == EMPTY_CHAR || array[i] == JSON_STOP_CHAR 
          || array[i] == JSON_END_CHAR) {
        break;
      }
      length += 1;
    }
    int top = ByteUtils.parseInt(array, tmp_index, length);
    if (process_debug) {
      LOG.info("parse buffer json data VehicleFace top is " + top);
    }
    
    float height = bottom - top;
    float weight = right - left;
    
    float result = weight / height;
    
    if (process_debug) {
      LOG.info("parse buffer json data VehicleFace point weight/height scale is " + result);
    }
    
    return result;
  }
  
  /**
   * 得到特征JSON数据中的车脸部分的JSON描述的下标位置.
   * @return
   */
  private static int findVehicleFaceIndex(byte[] array) {
    int index = indexOf(array,feature_type_key);
    if (index < 0) {
      return -1;
    }
    index = index + feature_type_key.length;
    
    int length = vehicle_face_key.length;
    
    boolean flag = false;
    while(!flag) {
      //查找值的开始位置
      for(int i=index; i<buffer_json_size; i++) {
        if (array[i] == EMPTY_CHAR || array[i] == JSON_CHAR) {
          continue;
        }
        index = i;
        break;
      }
      flag = ByteUtils.compareTo(array, index, length,
          vehicle_face_key, 0, length) == 0;
      
      if (!flag) {
        index = indexOf(array,index,feature_type_key);
        if (index < 0) {
          return -1;
        }
        index = index + height_key.length;
      }
    }//结束查找,得到index
    
    index = index + vehicle_face_key.length;
    
    return index;
  }
  
  
  /**
   * 得到feature中的图像高度
   * @param array
   * @return
   */
  public static int parseHeight(byte[] array) {
    int index = indexOf(array,height_key);
    if (index < 0) {
      return -1;
    }
    index = index + height_key.length;
    //查找值的开始位置
    for(int i=index; i<buffer_json_size; i++) {
      if (array[i] == EMPTY_CHAR || array[i] == JSON_CHAR) {
        continue;
      }
      index = i;
      break;
    }
    int length = 0;
    //查找值的结束位置
    for(int i=index; i<buffer_json_size; i++) {
      if (array[i] == EMPTY_CHAR || array[i] == JSON_STOP_CHAR 
          || array[i] == JSON_END_CHAR) {
        break;
      }
      length += 1;
    }
    int height = ByteUtils.parseInt(array, index, length);
    if (process_debug) {
      LOG.info("parse buffer json data image height is " + height);
    }
    return height;
  }
  
  /**
   * 得到feature中的图像宽度
   * @param array
   * @return
   */
  public static int parseWight(byte[] array) {
    int index = indexOf(array,wight_key);
    if (index < 0) {
      return -1;
    }
    index = index + wight_key.length;
    //查找值的开始位置
    for(int i=index; i<buffer_json_size; i++) {
      if (array[i] == EMPTY_CHAR || array[i] == JSON_CHAR) {
        continue;
      }
      index = i;
      break;
    }
    int length = 0;
    //查找值的结束位置
    for(int i=index; i<buffer_json_size; i++) {
      if (array[i] == EMPTY_CHAR || array[i] == JSON_STOP_CHAR 
          || array[i] == JSON_END_CHAR) {
        break;
      }
      length += 1;
    }
    int wight = ByteUtils.parseInt(array, index, length);
    if (process_debug) {
      LOG.info("parse buffer json data image wight is " + wight);
    }
    return wight;
  }
  
  
  
  private static int indexOf(byte[] array, int offset, byte[] target) {
    if (array.length < buffer_json_size) {
      return -1;
    }
    if (target.length == 0) {
      return 0;
    }

    outer: for (int i = offset; i < buffer_json_size - target.length + 1; i++) {
      for (int j = 0; j < target.length; j++) {
        if (array[i + j] != target[j]) {
          continue outer;
        }
      }
      return i;
    }
    return -1;
  }
  
  private static int indexOf(byte[] array, byte[] target) {
    if (array.length < buffer_json_size) {
      return -1;
    }
    if (target.length == 0) {
      return 0;
    }

    outer: for (int i = 0; i < buffer_json_size - target.length + 1; i++) {
      for (int j = 0; j < target.length; j++) {
        if (array[i + j] != target[j]) {
          continue outer;
        }
      }
      return i;
    }
    return -1;
  }

}
