package netposa.fakecar.feature.rpc.client;

import netposa.fakecar.feature.rpc.FeatureCalculatorService;
import netposa.fakecar.feature.rpc.InputRecordBuffer;

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
public class FeatureCalculatorServiceClientBuffer {
	
	public void run() throws Exception {
		
		TTransport transport = new TSocket("192.168.60.162", 30050, 30000);
		transport = new TFramedTransport(transport);
		TProtocol protocol = new TCompactProtocol(transport);
		 
		 FeatureCalculatorService.Iface client = new FeatureCalculatorService.Client(protocol);
		 long atime = System.nanoTime();
		 long btime = System.nanoTime();
		 transport.open();
		 for(int i=0; i<100000; i++) {
			 byte[] buf = new byte[1024];
			
			 InputRecordBuffer record = new InputRecordBuffer();
			 double[] f = new double[]{40.487732,32.778660,17.804218,0.000000,28.112400,0.000000,29.879078,37.057022,47.890903,54.297771,68.545486,6.096817,56.846298,24.537605,57.848228,32.582806,41.983589,57.302536,17.329865,4.522082,8.187728,28.454716,19.540409,32.237686,24.881680,0.000000,79.366928,53.191250,18.846058,8.191640,12.496756,60.111111,0.000000,29.041550,27.580170,22.316216,58.350037,18.276962,0.000000,56.976265,19.857492,42.470482,28.996092,0.000000,30.164967,38.694881,46.825550,24.473234,10.185288,30.550896,32.099880,0.000000,45.517982,19.358770,69.711914,35.238438,43.958908,38.805771,18.320313,28.766251,13.960146,25.511322,36.089306,65.161720,51.993839,10.700895,30.213032,52.969940,92.782021,26.705763,0.779787,17.601749,11.324945,26.863956,19.232491,0.000000,5.891032,15.302195,19.014622,0.000000,0.000000,0.000000,33.121475,0.000000,37.477196,64.548340,6.626931,0.000000,51.271290,38.808071,12.512976,55.766586,33.334984,26.949638,41.519638,26.685951,29.362923,34.792107,39.258549,26.124821,23.887445,12.441330,0.000000,35.754898,38.415924,33.146698,60.800163,40.272583,39.228458,29.681549,29.847946,27.631855,0.000000,63.804596,17.536949,23.221373,7.241757,13.041298,34.650555,56.271172,24.872709,34.282494,31.125969,56.804001,10.694308,29.604500,35.452343,5.607203,1.971632,0.000000,62.960857,54.660248,53.322800,32.239494,19.352364,10.476544,57.566341,25.697870,16.334621,35.576115,0.210735,30.121262,39.597885,46.697262,0.000000,38.479511,4.677407,43.668697,87.026344,0.000000,0.000000,8.746257,47.433937,61.257347,0.000000,0.000000,63.734253,0.000000,70.681267,0.598699,57.610714,67.970741,60.369934,39.337078,0.000000,22.790710,16.261681,7.291266,41.844131,32.677299,0.000000,0.000000,28.218157,51.055824,27.174957,2.981392,51.542530,17.483927,61.227543,29.683123,76.487389,36.802708,6.098651,57.428791,15.939428,40.936462,55.536480,65.508049,32.408791,29.519915,63.140701,11.235653,45.861012,18.618908,58.947914,60.843472,58.607132,30.921585,0.000000,42.555950,24.911913,0.000000,0.000000,83.972763,10.091413,25.330345,0.000000,62.946564,0.000000,0.000000,27.642544,43.204876,17.592104,21.130054,20.648636,21.066801,12.654335,15.147860,36.835312,0.000000,11.065906,17.514805,0.000000,0.000000,0.379770,44.983055,39.646851,19.947582,58.193214,66.582451,50.377617,44.537041,0.000000,0.000000,0.000000,41.829025,52.665081,5.551296,8.891097,5.753998,41.960571,25.201443,56.716618,36.911865,41.393917,7.183185,79.368546,43.705727,0.218338,28.218227,16.953634,27.433868,27.962118,78.657005,25.337187,40.152775};
			 for(int x=0; x<f.length; x++) {
				int t = Float.floatToRawIntBits((float)f[x]);
				int offset = x * 4;
				for (int y = 0; y < 3; y++) {
					buf[offset + y] = (byte) t;
					t >>>= 8;
				}
				buf[offset+3] = (byte) t;
			 }
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
			 //车辆特征,
			 record.setVehicle_feature(buf);
			 client.sendRecordBuffer(record);
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
			new FeatureCalculatorServiceClientBuffer().run();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
