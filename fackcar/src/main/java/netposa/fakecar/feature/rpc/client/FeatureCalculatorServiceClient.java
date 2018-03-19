package netposa.fakecar.feature.rpc.client;

import netposa.fakecar.feature.rpc.FeatureCalculatorService;
import netposa.fakecar.feature.rpc.InputRecord;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

/**
 * 客户端连接的java代码示例
 * @author hongs.yang
 *
 */
public class FeatureCalculatorServiceClient {
	private int timeout = 10000;
    private boolean framed = true;
    private boolean isCompact = true;
	
	public void run() throws Exception {
		 TTransport transport = new TSocket("192.168.60.162", 30050, timeout);
		 if (framed) {
			 transport = new TFramedTransport(transport);
		 }
		 TProtocol protocol = null;
		 if (isCompact) {
			 protocol = new TCompactProtocol(transport);
		 }else {
			 protocol = new TBinaryProtocol(transport);
		 }
		 FeatureCalculatorService.Iface client = 
				 new FeatureCalculatorService.Client(protocol);
		 long atime = System.nanoTime();
		 long btime = System.nanoTime();
		 transport.open();
		 for(int i=0; i<10000000; i++) {
			//1
			 InputRecord record = new InputRecord();
			 double[] f = new double[]{40.487732,32.778660,17.804218,0.000000,28.112400,0.000000,29.879078,37.057022,47.890903,54.297771,68.545486,6.096817,56.846298,24.537605,57.848228,32.582806,41.983589,57.302536,17.329865,4.522082,8.187728,28.454716,19.540409,32.237686,24.881680,0.000000,79.366928,53.191250,18.846058,8.191640,12.496756,60.111111,0.000000,29.041550,27.580170,22.316216,58.350037,18.276962,0.000000,56.976265,19.857492,42.470482,28.996092,0.000000,30.164967,38.694881,46.825550,24.473234,10.185288,30.550896,32.099880,0.000000,45.517982,19.358770,69.711914,35.238438,43.958908,38.805771,18.320313,28.766251,13.960146,25.511322,36.089306,65.161720,51.993839,10.700895,30.213032,52.969940,92.782021,26.705763,0.779787,17.601749,11.324945,26.863956,19.232491,0.000000,5.891032,15.302195,19.014622,0.000000,0.000000,0.000000,33.121475,0.000000,37.477196,64.548340,6.626931,0.000000,51.271290,38.808071,12.512976,55.766586,33.334984,26.949638,41.519638,26.685951,29.362923,34.792107,39.258549,26.124821,23.887445,12.441330,0.000000,35.754898,38.415924,33.146698,60.800163,40.272583,39.228458,29.681549,29.847946,27.631855,0.000000,63.804596,17.536949,23.221373,7.241757,13.041298,34.650555,56.271172,24.872709,34.282494,31.125969,56.804001,10.694308,29.604500,35.452343,5.607203,1.971632,0.000000,62.960857,54.660248,53.322800,32.239494,19.352364,10.476544,57.566341,25.697870,16.334621,35.576115,0.210735,30.121262,39.597885,46.697262,0.000000,38.479511,4.677407,43.668697,87.026344,0.000000,0.000000,8.746257,47.433937,61.257347,0.000000,0.000000,63.734253,0.000000,70.681267,0.598699,57.610714,67.970741,60.369934,39.337078,0.000000,22.790710,16.261681,7.291266,41.844131,32.677299,0.000000,0.000000,28.218157,51.055824,27.174957,2.981392,51.542530,17.483927,61.227543,29.683123,76.487389,36.802708,6.098651,57.428791,15.939428,40.936462,55.536480,65.508049,32.408791,29.519915,63.140701,11.235653,45.861012,18.618908,58.947914,60.843472,58.607132,30.921585,0.000000,42.555950,24.911913,0.000000,0.000000,83.972763,10.091413,25.330345,0.000000,62.946564,0.000000,0.000000,27.642544,43.204876,17.592104,21.130054,20.648636,21.066801,12.654335,15.147860,36.835312,0.000000,11.065906,17.514805,0.000000,0.000000,0.379770,44.983055,39.646851,19.947582,58.193214,66.582451,50.377617,44.537041,0.000000,0.000000,0.000000,41.829025,52.665081,5.551296,8.891097,5.753998,41.960571,25.201443,56.716618,36.911865,41.393917,7.183185,79.368546,43.705727,0.218338,28.218227,16.953634,27.433868,27.962118,78.657005,25.337187,40.152775};
			 //每个属性必须有值(不能为null),如果属性本身为null值,输入new byte[0]
			 record.setLicence_plate(("苏D7Q367").getBytes());
			 record.setLicence_plate_color("1".getBytes());
			 record.setVehicle_logo("大众".getBytes());
			 record.setVehicle_child_logo("途安".getBytes());
			 record.setVehicle_style("2010|2011".getBytes());
			 record.setVehicle_color("1".getBytes());
			 record.setVehicle_type("1".getBytes());
			 record.setRecord_id("1".getBytes());
			 record.setSource_type((byte)0);//公安卡口数据
			 //车辆特征,256长度 float值
			 for(int x=0; x<256; x++) {
				 record.addToVehicle_feature(f[x]);
			 }
			 client.sendRecord(record);
			 //Thread.sleep(10*1000);
			 //2
			 record = new InputRecord();
			 f = new double[]{40.487732,32.778660,17.804218,0.000000,28.112400,0.000000,29.879078,37.057022,47.890903,54.297771,68.545486,6.096817,56.846298,24.537605,57.848228,32.582806,41.983589,57.302536,17.329865,4.522082,8.187728,28.454716,19.540409,32.237686,24.881680,0.000000,79.366928,53.191250,18.846058,8.191640,12.496756,60.111111,0.000000,29.041550,27.580170,22.316216,58.350037,18.276962,0.000000,56.976265,19.857492,42.470482,28.996092,0.000000,30.164967,38.694881,46.825550,24.473234,10.185288,30.550896,32.099880,0.000000,45.517982,19.358770,69.711914,35.238438,43.958908,38.805771,18.320313,28.766251,13.960146,25.511322,36.089306,65.161720,51.993839,10.700895,30.213032,52.969940,92.782021,26.705763,0.779787,17.601749,11.324945,26.863956,19.232491,0.000000,5.891032,15.302195,19.014622,0.000000,0.000000,0.000000,33.121475,0.000000,37.477196,64.548340,6.626931,0.000000,51.271290,38.808071,12.512976,55.766586,33.334984,26.949638,41.519638,26.685951,29.362923,34.792107,39.258549,26.124821,23.887445,12.441330,0.000000,35.754898,38.415924,33.146698,60.800163,40.272583,39.228458,29.681549,29.847946,27.631855,0.000000,63.804596,17.536949,23.221373,7.241757,13.041298,34.650555,56.271172,24.872709,34.282494,31.125969,56.804001,10.694308,29.604500,35.452343,5.607203,1.971632,0.000000,62.960857,54.660248,53.322800,32.239494,19.352364,10.476544,57.566341,25.697870,16.334621,35.576115,0.210735,30.121262,39.597885,46.697262,0.000000,38.479511,4.677407,43.668697,87.026344,0.000000,0.000000,8.746257,47.433937,61.257347,0.000000,0.000000,63.734253,0.000000,70.681267,0.598699,57.610714,67.970741,60.369934,39.337078,0.000000,22.790710,16.261681,7.291266,41.844131,32.677299,0.000000,0.000000,28.218157,51.055824,27.174957,2.981392,51.542530,17.483927,61.227543,29.683123,76.487389,36.802708,6.098651,57.428791,15.939428,40.936462,55.536480,65.508049,32.408791,29.519915,63.140701,11.235653,45.861012,18.618908,58.947914,60.843472,58.607132,30.921585,0.000000,42.555950,24.911913,0.000000,0.000000,83.972763,10.091413,25.330345,0.000000,62.946564,0.000000,0.000000,27.642544,43.204876,17.592104,21.130054,20.648636,21.066801,12.654335,15.147860,36.835312,0.000000,11.065906,17.514805,0.000000,0.000000,0.379770,44.983055,39.646851,19.947582,58.193214,66.582451,50.377617,44.537041,0.000000,0.000000,0.000000,41.829025,52.665081,5.551296,8.891097,5.753998,41.960571,25.201443,56.716618,36.911865,41.393917,7.183185,79.368546,43.705727,0.218338,28.218227,16.953634,27.433868,27.962118,78.657005,25.337187,40.152775};
			 //每个属性必须有值(不能为null),如果属性本身为null值,输入new byte[0]
			 record.setLicence_plate(("苏D7Q367").getBytes());
			 record.setLicence_plate_color("1".getBytes());
			 record.setVehicle_logo("大众1".getBytes());
			 record.setVehicle_child_logo("途安".getBytes());
			 record.setVehicle_style("2010".getBytes());
			 record.setVehicle_color("1".getBytes());
			 record.setVehicle_type("1".getBytes());
			 record.setRecord_id("2".getBytes());
			 record.setSource_type((byte)0);//公安卡口数据
			 //车辆特征,256长度 float值
			 for(int x=0; x<256; x++) {
				 record.addToVehicle_feature(f[x]);
			 }
			 client.sendRecord(record);
			 //Thread.sleep(30*1000);
			 /*//3
			 record = new InputRecord();
			 f = new double[]{31.254444,0.000000,4.374449,0.000000,13.695219,0.000000,48.445080,39.792046,31.572762,38.532177,29.949333,0.000000,25.791811,37.700047,53.847401,12.144184,53.163231,58.379627,0.000000,8.454540,19.143152,43.839619,70.514732,4.755627,0.000000,0.000000,63.012444,28.289934,0.000000,0.000000,0.000000,35.627991,0.000000,14.158718,13.810973,51.622467,34.270226,29.003223,0.000000,53.676758,0.000000,27.487942,13.347861,0.000000,9.340202,44.156498,50.431576,34.566639,0.000000,28.978107,0.000000,0.000000,37.031708,14.774349,34.118690,38.416935,15.260597,20.664345,6.042610,9.785458,0.000000,39.090000,53.105030,66.508072,36.490402,1.834722,0.000000,54.339760,87.645180,0.000000,28.603838,14.719928,0.000000,25.199802,7.386030,0.000000,0.000000,0.000000,0.000000,37.709984,0.000000,13.788403,51.795128,0.000000,41.859898,6.690822,0.000000,11.126029,49.098400,18.252445,14.101622,24.303791,27.453360,15.803066,0.000000,28.640701,2.704940,7.028251,11.970508,0.000000,0.000000,0.000000,0.000000,22.014570,20.146294,32.509705,49.323147,0.000000,7.977479,18.982964,21.950718,0.000000,0.000000,52.555489,50.890728,0.000000,0.000000,0.000000,20.111647,36.514545,0.000000,15.833835,15.281458,50.671333,27.468925,4.688677,34.517952,0.000000,3.671541,0.000000,2.106874,19.408546,45.087090,10.675320,45.921761,0.993135,55.907200,0.000000,14.785926,32.669079,0.000000,40.843334,13.108384,39.757412,0.000000,11.807986,0.000000,31.328251,83.637978,16.532394,0.000000,0.000000,0.000000,57.527252,0.000000,18.553083,64.014740,0.000000,69.259712,0.000000,65.657051,69.319885,55.582024,36.703835,0.000000,8.384792,4.634372,2.174384,0.000000,14.738708,9.388652,44.207966,26.263557,64.423485,24.019001,10.820671,29.913136,0.000000,45.897118,0.420857,52.788811,0.000000,0.000000,53.981674,0.000000,18.585636,43.066216,66.764580,0.842216,20.846897,53.238800,17.890371,20.073616,11.914294,44.583534,57.098381,28.253244,40.996990,0.000000,21.397530,0.000000,0.000000,0.000000,82.159653,0.000000,36.277065,0.000000,2.532249,2.301823,0.000000,0.000000,45.840023,6.031516,26.491940,0.000000,0.000000,0.000000,0.000000,27.029514,5.242744,4.504765,2.552170,0.000000,0.000000,0.000000,55.529945,7.248770,0.363437,30.856478,67.648659,25.069580,33.550816,0.000000,0.000000,0.000000,37.565063,56.679707,0.000000,0.000000,0.000000,32.620152,19.215271,23.126255,0.000000,0.000000,0.000000,71.702431,37.480774,0.000000,3.042268,44.724731,7.289563,43.399750,63.946033,0.000000,37.148170};
			 //每个属性必须有值(不能为null),如果属性本身为null值,输入new byte[0]
			 record.setLicence_plate(("苏D7Q367").getBytes());
			 record.setLicence_plate_color("1".getBytes());
			 record.setVehicle_logo("大众".getBytes());
			 record.setVehicle_child_logo("途安".getBytes());
			 record.setVehicle_style("2010".getBytes());
			 record.setVehicle_color("1".getBytes());
			 record.setVehicle_type("1".getBytes());
			 record.setRecord_id("3".getBytes());
			 record.setSource_type((byte)0);//公安卡口数据
			 //车辆特征,256长度 float值
			 for(int x=0; x<256; x++) {
				 record.addToVehicle_feature(f[x]);
			 }
			 client.sendRecord(record);
			//4,要被清洗掉的数据,数据来源为交警,电警
			 record = new InputRecord();
			 f = new double[]{31.254444,0.000000,4.374449,0.000000,13.695219,0.000000,48.445080,39.792046,31.572762,38.532177,29.949333,0.000000,25.791811,37.700047,53.847401,12.144184,53.163231,58.379627,0.000000,8.454540,19.143152,43.839619,70.514732,4.755627,0.000000,0.000000,63.012444,28.289934,0.000000,0.000000,0.000000,35.627991,0.000000,14.158718,13.810973,51.622467,34.270226,29.003223,0.000000,53.676758,0.000000,27.487942,13.347861,0.000000,9.340202,44.156498,50.431576,34.566639,0.000000,28.978107,0.000000,0.000000,37.031708,14.774349,34.118690,38.416935,15.260597,20.664345,6.042610,9.785458,0.000000,39.090000,53.105030,66.508072,36.490402,1.834722,0.000000,54.339760,87.645180,0.000000,28.603838,14.719928,0.000000,25.199802,7.386030,0.000000,0.000000,0.000000,0.000000,37.709984,0.000000,13.788403,51.795128,0.000000,41.859898,6.690822,0.000000,11.126029,49.098400,18.252445,14.101622,24.303791,27.453360,15.803066,0.000000,28.640701,2.704940,7.028251,11.970508,0.000000,0.000000,0.000000,0.000000,22.014570,20.146294,32.509705,49.323147,0.000000,7.977479,18.982964,21.950718,0.000000,0.000000,52.555489,50.890728,0.000000,0.000000,0.000000,20.111647,36.514545,0.000000,15.833835,15.281458,50.671333,27.468925,4.688677,34.517952,0.000000,3.671541,0.000000,2.106874,19.408546,45.087090,10.675320,45.921761,0.993135,55.907200,0.000000,14.785926,32.669079,0.000000,40.843334,13.108384,39.757412,0.000000,11.807986,0.000000,31.328251,83.637978,16.532394,0.000000,0.000000,0.000000,57.527252,0.000000,18.553083,64.014740,0.000000,69.259712,0.000000,65.657051,69.319885,55.582024,36.703835,0.000000,8.384792,4.634372,2.174384,0.000000,14.738708,9.388652,44.207966,26.263557,64.423485,24.019001,10.820671,29.913136,0.000000,45.897118,0.420857,52.788811,0.000000,0.000000,53.981674,0.000000,18.585636,43.066216,66.764580,0.842216,20.846897,53.238800,17.890371,20.073616,11.914294,44.583534,57.098381,28.253244,40.996990,0.000000,21.397530,0.000000,0.000000,0.000000,82.159653,0.000000,36.277065,0.000000,2.532249,2.301823,0.000000,0.000000,45.840023,6.031516,26.491940,0.000000,0.000000,0.000000,0.000000,27.029514,5.242744,4.504765,2.552170,0.000000,0.000000,0.000000,55.529945,7.248770,0.363437,30.856478,67.648659,25.069580,33.550816,0.000000,0.000000,0.000000,37.565063,56.679707,0.000000,0.000000,0.000000,32.620152,19.215271,23.126255,0.000000,0.000000,0.000000,71.702431,37.480774,0.000000,3.042268,44.724731,7.289563,43.399750,63.946033,0.000000,37.148170};
			 //每个属性必须有值(不能为null),如果属性本身为null值,输入new byte[0]
			 record.setLicence_plate(("苏D7Q367").getBytes());
			 record.setLicence_plate_color("1".getBytes());
			 record.setVehicle_logo("大众1".getBytes());
			 record.setVehicle_child_logo("途安".getBytes());
			 record.setVehicle_style("2010".getBytes());
			 record.setVehicle_color("1".getBytes());
			 record.setVehicle_type("1".getBytes());
			 record.setRecord_id("3".getBytes());
			 record.setSource_type((byte)1);//交警电警卡口数据
			 //车辆特征,256长度 float值
			 for(int x=0; x<256; x++) {
				 record.addToVehicle_feature(f[x]);
			 }
			 client.sendRecord(record);*/
			 if (i % 10000 == 0) {
				 System.out.println(i + "   " + (System.nanoTime() - btime));
				 btime = System.nanoTime();
			 }
		 }
		 System.out.println("execute time is " + (System.nanoTime()-atime));
		 transport.close();
	}
	
	public static void main(String[] args) {
		try {
			new FeatureCalculatorServiceClient().run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
