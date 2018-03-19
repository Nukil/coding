package netposa.fakecar.feature.rpc.error;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import netposa.fakecar.feature.rpc.ResultRecord;
import netposa.fakecar.feature.rpc.ValueRecord;

public class WriteErrorSqlToFile {
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
	private static final Charset charset = Charset.forName("UTF-8");

	public static void main(String[] args) {
		WriteErrorSqlToFile test = new WriteErrorSqlToFile();
		test.writeFeatureToFileTest();
		try {
			Thread.sleep(1000*70);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		test.readFeatureToFileTest();
	}
	
	public void writeSqlToFileTest(){
		String feature = "INSERT INTO VMC_CLONE_FEATURE (HPHM_,FHPYS_,SHPYS_,FGCBH_,SGCBH_,FCLPP_,SCLPP_,FCLZPP_,SCLZPP_,FCLNK_,SCLNK_,FCLLX_,SCLLX_,FCLYS_,SCLYS_,FAKEPLATETYPE_,INSERTTIME_) VALUES ('皖C01533','2','2','156144973877703088','156144973879301803','49','8','490006','80004','0','131072','K32','K33','A','A','0','2015-12-10 17:09:30')";
		String similar = "INSERT INTO SIMILAR_VEHICLE (FGCBH_,FSGCBHS_,SGCBH_,SSGCBHS_) VALUES ('156144973877703088','94144964138394985,94144964122108362,94144964114513590,94144963968168310,94144963876626639','156144973879301803','94144964138394985,94144964122108362,94144964114513590,94144963968168310,94144963876626639')";
		
		while(true){
			try {
				writeSqlToFile(feature,"feature");
				Thread.sleep(1000*1);
				writeSqlToFile(similar,"similar");
				Thread.sleep(1000*1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void getSqlFromFileTest(){
		
		List<String> list = getSqlFromFile("feature");
		if(list!=null && list.size()>0){
			for(String str : list){
				System.out.println(str);
			}
		}
		
		 list = getSqlFromFile("similar");
		if(list!=null && list.size()>0){
			for(String str : list){
				System.out.println(str);
			}
		}
		
	}
	
	/**
	 * 将错误slq 写入到文件中
	 * @param sql 
	 * @param type feature：套牌，similar：相似车
	 */
	public static void writeSqlToFile(String sql, String type){
		
		File file = new File("error" + File.separator + type + File.separator + sdf.format(new Date()));
		FileOutputStream out = null;
		
		try {
			
			if(!file.getParentFile().exists()){
				file.getParentFile().mkdirs();
			}
			
			out = new FileOutputStream(file, true);
			out.write(sql.getBytes());
			out.write("\n".getBytes());
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				if(out != null){
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * 从错误日志文件中读取错误sql
	 * 
	 * @param type feature：套牌，similar：相似车
	 */
	public static List<String> getSqlFromFile(String type){
		
		List<String> list = new ArrayList<>();
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.MINUTE, -1);
		
		long fileName = Long.valueOf(sdf.format(calendar.getTime()));
		
		File file = new File("error" + File.separator + type + File.separator);
		
		try {
			
			if(file.exists() && file.isDirectory()){
				
				File[] files = file.listFiles();
				
				for(File f : files){
					if(Long.valueOf(f.getName())<=fileName){
						FileReader reader = new FileReader(f); 
						BufferedReader br = new BufferedReader(reader);
						String str = null; 
						while((str = br.readLine()) != null) {
							list.add(str);
						}
						br.close();
						reader.close();
						System.out.println(f.getAbsolutePath());
						f.delete();
					}
				}
				
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return list;
	}
	
	public void writeFeatureToFileTest(){
		
		int idLength = 20;
		int featureSize = 288;

		ValueRecord firstValue = new ValueRecord();
		ValueRecord secondValue = new ValueRecord();
		
		byte[] firstId = "00004622294687804201".getBytes();
		byte[] secondId = "00003673454620004757".getBytes();
		
		byte firstColor = 'A';
		byte secondColor = 'F';
		
		byte[] firstLogo = "99".getBytes();
		byte[] secondLogo = "23".getBytes();
		
		byte[] firstFeature = new byte[featureSize];
		byte[] secondFeature = new byte[featureSize];
		for(int i=0;i<288;i++){
			firstFeature[i] = (byte)i;
			secondFeature[i] = (byte)i;
		}
		
		firstValue.setRecord_id(firstId);
		firstValue.setVehicle_color(new byte[]{firstColor});
		firstValue.setVehicle_logo(firstLogo);
		firstValue.setVehicle_feature_buffer(firstFeature);
		
		secondValue.setRecord_id(secondId);
		secondValue.setVehicle_color(new byte[]{secondColor});
		secondValue.setVehicle_logo(secondLogo);
		secondValue.setVehicle_feature_buffer(secondFeature);
		
		ResultRecord record = new ResultRecord(null, firstValue, secondValue);
		
		for (int i = 0; i < 100; i++) {
			writeFeatureToFile(record, idLength, featureSize);
		}
		
	}
	
	/**
	 * 
	 * @param record
	 * @param idLength
	 * @param featureSize
	 */
	public static void writeFeatureToFile(ResultRecord record, int idLength, int featureSize){
		
		byte[] buf = new byte[idLength*2+4+featureSize*2];
		byte[] temp = new byte[idLength];
		for(int i=0;i<temp.length;i++){
			temp[i] = '#';
		}
		
		ValueRecord firstValue = record.getFirstValue();
		ValueRecord secondValue = record.getSecondValue();
		
		if(firstValue==null || secondValue==null){
			return;
		}
		
		byte[] firstId = null;
		if(firstValue.getRecord_id()!=null && firstValue.getRecord_id().length>0) {
			firstId = firstValue.getRecord_id();
		}else {
			return;
		}
		byte[] secondId = null;
		if(secondValue.getRecord_id()!=null && secondValue.getRecord_id().length>0) {
			secondId = secondValue.getRecord_id();
		} else {
			return;
		}
		
		byte firstColor = 'Z';
		if(firstValue.getVehicle_color()!=null && firstValue.getVehicle_color().length>0) firstColor = firstValue.getVehicle_color()[0];
		byte firstLogo = -1;
		if(firstValue.getVehicle_logo()!=null && firstValue.getVehicle_logo().length>0) firstLogo = (byte) Integer.valueOf(new String(firstValue.getVehicle_logo(),charset)).intValue();
		byte[] firstFeature = null;
		if(firstValue.getVehicle_feature_buffer() != null && firstValue.getVehicle_feature_buffer().length>0) {
			firstFeature = firstValue.getVehicle_feature_buffer();
		}else {
			return;
		}
		
		byte secondColor = 'Z';
		if(secondValue.getVehicle_color()!=null && secondValue.getVehicle_color().length>0) secondColor = secondValue.getVehicle_color()[0];
		byte secondLogo = -1;
		if(secondValue.getVehicle_logo()!=null && secondValue.getVehicle_logo().length>0) secondLogo = (byte) Integer.valueOf(new String(secondValue.getVehicle_logo(),charset)).intValue();
		byte[] secondFeature = null;
		if(secondValue.getVehicle_feature_buffer() != null && secondValue.getVehicle_feature_buffer().length>0) {
			secondFeature = secondValue.getVehicle_feature_buffer();
		}
		
		int temp_1 = idLength-firstId.length;
		int temp_2 = idLength-secondId.length;
		
		System.arraycopy(firstId, 0, buf, 0, firstId.length);
		if(temp_1>0){
			System.arraycopy(temp, 0, buf, firstId.length, temp_1);
		}
		System.arraycopy(secondId, 0, buf, idLength, secondId.length);
		if(temp_2>0){
			System.arraycopy(temp, 0, buf, idLength+secondId.length, temp_2);
		}
		
		buf[idLength*2+0] = firstColor;
		buf[idLength*2+1] = secondColor;
		buf[idLength*2+2] = firstLogo;
		buf[idLength*2+3] = secondLogo;
		
		System.arraycopy(firstFeature, 0, buf, idLength*2+4, featureSize);
		System.arraycopy(secondFeature, 0, buf, idLength*2+4+featureSize, featureSize);
		
		File file = new File("error" + File.separator + "compare" + File.separator + sdf.format(new Date()));
		FileOutputStream out = null;
		
		try {
			
			if(!file.getParentFile().exists()){
				file.getParentFile().mkdirs();
			}
			
			out = new FileOutputStream(file, true);
			out.write(buf);
			out.flush();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				if(out != null){
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public void readFeatureToFileTest(){
		List<ResultRecord> list = readFeatureToFile(20, 288);
		System.out.println(list.size());
		for (ResultRecord record : list) {
			ValueRecord firstValue = record.getFirstValue();
			System.out.println("firstId:"+new String(firstValue.getRecord_id(),charset));
			System.out.println("firstColor:"+new String(firstValue.getVehicle_color(),charset));
			System.out.println("firstLogo:"+new String(firstValue.getVehicle_logo(),charset));
			System.out.println("firstFeature:"+firstValue.getVehicle_feature_buffer().length);
			
			ValueRecord secondValue = record.getSecondValue();
			System.out.println("secondId:"+new String(secondValue.getRecord_id(),charset));
			System.out.println("secondColor:"+new String(secondValue.getVehicle_color(),charset));
			System.out.println("secondLogo:"+new String(secondValue.getVehicle_logo(),charset));
			System.out.println("secondFeature:"+secondValue.getVehicle_feature_buffer().length);
			
			System.out.println("------------------------------------");
		}
	}
	
	public static List<ResultRecord> readFeatureToFile(int idLength, int featureSize){
		
		List<ResultRecord> list = new ArrayList<>();
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.MINUTE, -1);
		
		long fileName = Long.valueOf(sdf.format(calendar.getTime()));
		
		File file = new File("error" + File.separator + "compare" + File.separator);
		
		try {
			
			if(file.exists() && file.isDirectory()){
				byte[] buffer = new byte[idLength*2+4+featureSize*2];
				File[] files = file.listFiles();
				
				for(File f : files){
					if(Long.valueOf(f.getName())<=fileName){
						InputStream in = new FileInputStream(f);
						while(in.read(buffer) >= 0) {
							ValueRecord firstValue = new ValueRecord();
							ValueRecord secondValue = new ValueRecord();
							
							byte[] temp = new byte[idLength];
							System.arraycopy(buffer, 0, temp, 0, idLength);
							byte[] firstId = new String(temp,charset).replaceAll("#", "").getBytes();
							System.arraycopy(buffer, idLength, temp, 0, idLength);
							byte[] secondId = new String(temp,charset).replaceAll("#", "").getBytes();
							
							byte firstColor = buffer[idLength*2+0];
							byte secondColor = buffer[idLength*2+1];
							
							byte[] firstLogo = ((int)buffer[idLength*2+2] + "").getBytes();
							byte[] secondLogo = ((int)buffer[idLength*2+3] + "").getBytes();
							
							byte[] firstFeature = new byte[featureSize];
							System.arraycopy(buffer, idLength*2+4, firstFeature, 0, featureSize);
							byte[] secondFeature = new byte[featureSize];
							System.arraycopy(buffer, idLength*2+4+featureSize, secondFeature, 0, featureSize);
							
							firstValue.setRecord_id(firstId);
							firstValue.setVehicle_color(new byte[]{firstColor});
							firstValue.setVehicle_logo(firstLogo);
							firstValue.setVehicle_feature_buffer(firstFeature);
							
							secondValue.setRecord_id(secondId);
							secondValue.setVehicle_color(new byte[]{secondColor});
							secondValue.setVehicle_logo(secondLogo);
							secondValue.setVehicle_feature_buffer(secondFeature);
							
							ResultRecord record = new ResultRecord(null, firstValue, secondValue);
							list.add(record);
						}
						in.close();
						
						f.delete();
					}
				}
				
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return list;
		
	}

	
	/**
	 * for 八大库
	 * @param record
	 * @param idLength
	 * @param featureSize
	 */
	public static void writeFeatureToFile(ValueRecord record, int idLength, int featureSize){
		
		byte[] buf = new byte[idLength*2+4+featureSize*2];
		byte[] temp = new byte[idLength];
		for(int i=0;i<temp.length;i++){
			temp[i] = '#';
		}
		
		ValueRecord firstValue = record;
		
		if(firstValue==null){
			return;
		}
		
		byte[] firstId = null;
		if(firstValue.getRecord_id()!=null && firstValue.getRecord_id().length>0) {
			firstId = firstValue.getRecord_id();
		}else {
			return;
		}
		
		byte firstColor = 'Z';
		if(firstValue.getVehicle_color()!=null && firstValue.getVehicle_color().length>0) firstColor = firstValue.getVehicle_color()[0];
		byte firstLogo = -1;
		if(firstValue.getVehicle_logo()!=null && firstValue.getVehicle_logo().length>0) firstLogo = (byte) Integer.valueOf(new String(firstValue.getVehicle_logo(),charset)).intValue();
		byte[] firstFeature = null;
		if(firstValue.getVehicle_feature_buffer() != null && firstValue.getVehicle_feature_buffer().length>0) {
			firstFeature = firstValue.getVehicle_feature_buffer();
		}else {
			return;
		}
		
		int temp_1 = idLength-firstId.length;
		
		System.arraycopy(firstId, 0, buf, 0, firstId.length);
		if(temp_1>0){
			System.arraycopy(temp, 0, buf, firstId.length, temp_1);
		}
		
		buf[idLength*2+0] = firstColor;
		buf[idLength*2+2] = firstLogo;
		
		System.arraycopy(firstFeature, 0, buf, idLength*2+4, featureSize);
		
		File file = new File("error" + File.separator + "bdkcompare" + File.separator + sdf.format(new Date()));
		FileOutputStream out = null;
		
		try {
			
			if(!file.getParentFile().exists()){
				file.getParentFile().mkdirs();
			}
			
			out = new FileOutputStream(file, true);
			out.write(buf);
			out.flush();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				if(out != null){
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public static List<ValueRecord> readFeatureToFileForBdk(int idLength, int featureSize){
		
		List<ValueRecord> list = new ArrayList<ValueRecord>();
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.MINUTE, -1);
		
		long fileName = Long.valueOf(sdf.format(calendar.getTime()));
		
		File file = new File("error" + File.separator + "bdkcompare" + File.separator);
		
		try {
			
			if(file.exists() && file.isDirectory()){
				byte[] buffer = new byte[idLength*2+4+featureSize*2];
				File[] files = file.listFiles();
				
				for(File f : files){
					if(Long.valueOf(f.getName())<=fileName){
						InputStream in = new FileInputStream(f);
						while(in.read(buffer) >= 0) {
							ValueRecord firstValue = new ValueRecord();
							
							byte[] temp = new byte[idLength];
							System.arraycopy(buffer, 0, temp, 0, idLength);
							byte[] firstId = new String(temp,charset).replaceAll("#", "").getBytes();
							System.arraycopy(buffer, idLength, temp, 0, idLength);
							
							byte firstColor = buffer[idLength*2+0];
							
							byte[] firstLogo = ((int)buffer[idLength*2+2] + "").getBytes();
							
							byte[] firstFeature = new byte[featureSize];
							System.arraycopy(buffer, idLength*2+4, firstFeature, 0, featureSize);
							
							firstValue.setRecord_id(firstId);
							firstValue.setVehicle_color(new byte[]{firstColor});
							firstValue.setVehicle_logo(firstLogo);
							firstValue.setVehicle_feature_buffer(firstFeature);
							
							
							list.add(firstValue);
						}
						in.close();
						
						f.delete();
					}
				}
				
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return list;
		
	}
}
