package netposa.fakecar.feature.rpc.search.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import netposa.fakecar.feature.rpc.search.InputRecord;
import netposa.fakecar.feature.rpc.search.SimilarVehicleSearchRpcService;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

public class SimilarVehicleSearchRpcClient {
	
	private static int inLength = 8;

	public static void main(String[] args) {
		long t1 = System.currentTimeMillis();
		for (int i = 0; i < 10; i++) {
		}
		try {
			client();
		} catch (Exception e) {
			e.printStackTrace();
		}
		long t2 = System.currentTimeMillis();
		System.out.println(t2-t1);
	}
	
	public static void client(){
		try {
			
			//for(int i=1;i<=5;i++){
				
				File file_1 = new File("E:\\Users\\sunny\\bigdata\\similar_vehicle_search\\data\\" + 1 + "_0_99.dat");
				byte[] bytes_1 = readFileByLines(file_1);
				ByteBuffer byteBuffer = ByteBuffer.allocate(bytes_1.length);
				byteBuffer.put(bytes_1);
				
				InputRecord record = new InputRecord();
				record.setStartTime("20151112");
				record.setEndTime("20151119");
				record.setClpp("25,26,27,28,29");
				record.setCount(10000);
				record.setFeature(bytes_1);
				record.setDistence(5);
				record.setCsys(1);
				
				TTransport transport = new TSocket("192.168.60.162", 30056, 10000);
				transport = new TFramedTransport(transport);
				
				TProtocol protocol = new TCompactProtocol(transport);
				SimilarVehicleSearchRpcService.Client client = new SimilarVehicleSearchRpcService.Client(protocol);
				
				transport.open();
				
				String result = client.getSearchData(record);
				System.out.println(result);
				
				transport.close();
				
			//}
			
		} catch (TTransportException e) {
			e.printStackTrace();
		} catch (TException x) {
			x.printStackTrace();
		}
	}
	
	
	public static void readFile() throws IOException{
		
		ByteBuffer longBuffer = ByteBuffer.allocate(8);
		
		//别克
		File file_1 = new File("E:\\Users\\sunny\\bigdata\\similar_vehicle_search\\data\\1_0_99.dat");
		File file_5 = new File("E:\\Users\\sunny\\bigdata\\similar_vehicle_search\\data\\5_0_99.dat");
		byte[] bytes_1 = readFileByLines(file_1);
		byte[] bytes_5 = readFileByLines(file_5);
//		System.out.println(bytes_1.length);
//		System.out.println(bytes_5.length);
		
		String datafile_1 = "D:\\data\\20151105\\1\\0001";
		OutputStream out_1 = new FileOutputStream(new File(datafile_1), true);
		byte[] values_1 = new byte[bytes_1.length+inLength];
		byte[] values_5 = new byte[bytes_5.length+inLength];
		for (int i = 0; i < 100000; i++) {
			longBuffer.clear();
			longBuffer.putLong(i);
			
			System.arraycopy(longBuffer.array(), 0, values_1, 0, inLength);
			System.arraycopy(bytes_1, 0, values_1, inLength, bytes_1.length);
			
			System.arraycopy(longBuffer.array(), 0, values_5, 0, inLength);
			System.arraycopy(bytes_5, 0, values_5, inLength, bytes_5.length);
			
			out_1.write(values_1);
			out_1.write(values_5);
		}
		out_1.close();
		
		//奥迪
		File file_2 = new File("E:\\Users\\sunny\\bigdata\\similar_vehicle_search\\data\\2_0_99.dat");
		File file_3 = new File("E:\\Users\\sunny\\bigdata\\similar_vehicle_search\\data\\3_0_99.dat");
		byte[] bytes_2 = readFileByLines(file_2);
		byte[] bytes_3 = readFileByLines(file_3);
//		System.out.println(bytes_2.length);
//		System.out.println(bytes_3.length);
		String datafile_2 = "D:\\data\\20151105\\2\\0001";
		FileOutputStream out_2 = new FileOutputStream(new File(datafile_2), true);
		byte[] values_2 = new byte[bytes_2.length+inLength];
		byte[] values_3 = new byte[bytes_3.length+inLength];
		for (int i = 0; i < 100000; i++) {
			longBuffer.clear();
			longBuffer.putLong(i);
			
			System.arraycopy(longBuffer.array(), 0, values_2, 0, inLength);
			System.arraycopy(bytes_2, 0, values_2, inLength, bytes_2.length);
			
			System.arraycopy(longBuffer.array(), 0, values_3, 0, inLength);
			System.arraycopy(bytes_3, 0, values_3, inLength, bytes_3.length);
			
			out_2.write(values_2);
			out_2.write(values_3);
		}
		out_2.close();
		
		//马自达
		File file_4 = new File("E:\\Users\\sunny\\bigdata\\similar_vehicle_search\\data\\4_0_99.dat");
		byte[] bytes_4 = readFileByLines(file_4);
//		System.out.println(bytes_4.length);
		String datafile_3 = "D:\\data\\20151105\\3\\0001";
		FileOutputStream out_3 = new FileOutputStream(new File(datafile_3), true);
		byte[] values_4 = new byte[bytes_4.length+inLength];
		for (int i = 0; i < 100000; i++) {
			longBuffer.clear();
			longBuffer.putLong(i);
			
			System.arraycopy(longBuffer.array(), 0, values_4, 0, inLength);
			System.arraycopy(bytes_4, 0, values_4, inLength, bytes_4.length);
			
			out_3.write(values_4);
		}
		out_3.close();
		
	}
	
	/**
     * 以行为单位读取文件，常用于读面向行的格式化文件
     */
    private static byte[] readFileByLines(File file) {
    	
        InputStream reader = null;
        byte[] buffer = new byte[288];
        try {
            reader = new FileInputStream(file);
            // 一次读入一行，直到读入null为文件结束
            while (reader.read(buffer) >= 0) {
                // 显示行号
//              System.out.println("line " + line + ", temp " + temp);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        
        return buffer;
    }
    
}