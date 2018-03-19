package netposa.fakecar.feature.rpc.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import netposa.fakecar.feature.rpc.FeatureCalculatorService;
import netposa.fakecar.feature.rpc.InputRecordBuffer;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

public class FeatureV2CalculatorClient {
  private int timeout = 20000;// 连接超时时间
  private boolean framed = true;// 默认值,不做修改
  private boolean isCompact = true;// 默认值,不做修改

  public void run() throws Exception {
    TTransport transport = new TSocket("192.168.241.129", 30050, timeout);
    if (framed) {
      transport = new TFramedTransport(transport);
    }

    TProtocol protocol = null;
    if (isCompact) {
      protocol = new TCompactProtocol(transport);
    } else {
      protocol = new TBinaryProtocol(transport);
    }
    FeatureCalculatorService.Iface client = new FeatureCalculatorService.Client(
        protocol);
    // 应用启动,打开SOCKET的长连接
    transport.open();

    Thread.sleep(2000);
    // 1
    InputRecordBuffer record = new InputRecordBuffer();
    // 每个属性必须有值(不能为null),如果属性本身为null值,输入new byte[0]
    record.setLicence_plate(("苏D7Q367").getBytes());// 车牌号
    record.setLicence_plate_color("1".getBytes());// 车牌颜色
    record.setVehicle_logo("大众".getBytes());// 车辆品牌
    record.setVehicle_child_logo("途安".getBytes());// 车辆子品牌
    record.setVehicle_style("2010|2011".getBytes());// 车辆年款
    record.setVehicle_color("1".getBytes());// 车辆颜色
    record.setVehicle_type("1".getBytes());// 车辆类型
    record.setRecord_id("1".getBytes());// 记录编码
    record.setSource_type((byte) 0);// 卡口类型0=公安卡口数据,1=电警卡口
    record.setConfidence_level(99);// 二次识别的置信度,如果输入-1表示没有置信度,此值是一个0-100之间的float数
    record.setTraffic_time("20150525172637".getBytes());
    record.setOld_licence_plate("苏D7Q367".getBytes());
    record.setPlate_confidence(80);
    // 加载特征文件到实例的BUFFER中
    File file_1 = new File("orders/1.dat");
    System.out.println(file_1.getCanonicalPath());
    long file_size = file_1.length();
    byte[] buffer = new byte[(int) file_size];
    InputStream inStream = new FileInputStream(file_1);
    inStream.read(buffer);
    record.setVehicle_feature(buffer);// 特征buffer
    inStream.close();
    // 发送数据到计算服务器
    client.sendRecordBuffer(record);

    Thread.sleep(4000);

    // 2
    InputRecordBuffer record_2 = new InputRecordBuffer();
    // 每个属性必须有值(不能为null),如果属性本身为null值,输入new byte[0]
    record_2.setLicence_plate(("苏D7Q367").getBytes());
    record_2.setLicence_plate_color("1".getBytes());
    record_2.setVehicle_logo("大众".getBytes());
    record_2.setVehicle_child_logo("途安1".getBytes());
    record_2.setVehicle_style("2010|2011".getBytes());
    record_2.setVehicle_color("1".getBytes());
    record_2.setVehicle_type("1".getBytes());
    record_2.setRecord_id("1".getBytes());
    record_2.setSource_type((byte) 0);// 公安卡口数据
    record_2.setConfidence_level(99);// 二次识别的置信度,如果输入-1表示没有置信度,此值是一个0-100之间的float数
    record_2.setTraffic_time("20150525182637".getBytes());
    record_2.setOld_licence_plate("苏D7Q367".getBytes());
    record_2.setPlate_confidence(80);
    // 加载特征文件到实例的BUFFER中
    File file_2 = new File("orders/苏D6U963-1.jpg.dat");
    System.out.println(file_2.getCanonicalPath());
    long file_size_2 = file_2.length();
    byte[] buffer_2 = new byte[(int) file_size_2];
    InputStream inStream_2 = new FileInputStream(file_2);
    inStream_2.read(buffer_2);
    record_2.setVehicle_feature(buffer_2);
    inStream_2.close();
    // 发送数据到计算服务器
    client.sendRecordBuffer(record_2);

    Thread.sleep(10000);

    // 应用结束,关闭SOCKET的长连接
    transport.close();
  }

  // 得到传入的多个参数中最小的一个值
  private static int min(int... is) {
    int min = Integer.MAX_VALUE;
    for (int i : is) {
      if (min > i) {
        min = i;
      }
    }
    return min;
  }
  
  public static float levenshtein(byte[] arr1, byte[] arr2) {
    // 计算两个字符串的长度。
    int len1 = arr1.length;
    int len2 = arr2.length;
    // 建立上面说的数组，比字符长度大一个空间
    int[][] dif = new int[len1 + 1][len2 + 1];
    // 赋初值，步骤B。
    for (int a = 0; a <= len1; a++) {
      dif[a][0] = a;
    }
    for (int a = 0; a <= len2; a++) {
      dif[0][a] = a;
    }
    // 计算两个字符是否一样，计算左上的值
    int temp;
    for (int i = 1; i <= len1; i++) {
      for (int j = 1; j <= len2; j++) {
        if (arr1[i-1] == arr2[j-1]) {
          temp = 0;
        } else {
          temp = 1;
        }
        // 取三个值中最小的
        dif[i][j] = min(dif[i - 1][j - 1] + temp, dif[i][j - 1] + 1,
            dif[i - 1][j] + 1);
      }
    }
    
    // 计算相似度
    int max_length = Math.max(arr1.length, arr2.length);
    float similarity = (1 - (float) dif[len1][len2] / max_length) * 100;
    return similarity;
  }

  public static float levenshtein(String str1, String str2) {
    // 计算两个字符串的长度。
    int len1 = str1.length();
    int len2 = str2.length();
    // 建立上面说的数组，比字符长度大一个空间
    int[][] dif = new int[len1 + 1][len2 + 1];
    // 赋初值，步骤B。
    for (int a = 0; a <= len1; a++) {
      dif[a][0] = a;
    }
    for (int a = 0; a <= len2; a++) {
      dif[0][a] = a;
    }

    // 计算两个字符是否一样，计算左上的值
    int temp;
    for (int i = 1; i <= len1; i++) {
      for (int j = 1; j <= len2; j++) {
        if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
          temp = 0;
        } else {
          temp = 1;
        }
        // 取三个值中最小的
        dif[i][j] = min(dif[i - 1][j - 1] + temp, dif[i][j - 1] + 1,
            dif[i - 1][j] + 1);
      }
    }

    //System.out.println("字符串\"" + str1 + "\"与\"" + str2 + "\"的比较");
    // 取数组右下角的值，同样不同位置代表不同字符串的比较
    //System.out.println("差异步骤：" + dif[len1][len2]);
    // 计算相似度
    float similarity = 1 - (float) dif[len1][len2]
        / Math.max(str1.length(), str2.length());
    //System.out.println("相似度：" + similarity);
    return similarity;
  }

  public static void main(String[] args) {
    try {
       /*
       File file_2 = new File("orders/苏DR5877-2.jpg.dat");
       System.out.println(file_2.getCanonicalPath()); 
       long file_size_2 = file_2.length(); 
       byte[] buffer_2 = new byte[(int)file_size_2];
       InputStream inStream_2 = new FileInputStream(file_2);
       inStream_2.read(buffer_2); 
       inStream_2.close(); 
       int wight = FeatureJsonParseUtils.parseWight(buffer_2); 
       int height = FeatureJsonParseUtils.parseHeight(buffer_2); 
       System.out.println(wight + "  " + height); 
       float face_scale = FeatureJsonParseUtils.parseVehicleFacePoint(buffer_2);
       System.out.println("face scale is " + face_scale);
       byte[] time = "20150525234524".getBytes();
       Map<DateFormatDefine, int[]> date_pattern = ByteUtils.buildDatePattern("yyyyMMddHHmmss");
       System.out.println(ByteUtils.parseBytesHour(time, date_pattern));
       */
      new FeatureV2CalculatorClient().run();
      //苏DF177P苏DF177M
      //float a = levenshtein("苏DF177M".getBytes(),"苏DF177M".getBytes());
      //System.out.println(a);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
