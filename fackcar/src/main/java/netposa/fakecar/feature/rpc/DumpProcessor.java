package netposa.fakecar.feature.rpc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 内存数据持久化到内存的处理类
 * @author guojiawei
 */
public class DumpProcessor {
	private static final Logger LOG = LoggerFactory
		      .getLogger(DumpProcessor.class);
	
	//持久化线程数
	private static final int thread_num = 8;
	
	private static String save_dir_path = "fakecardump/";
	//套牌处理类
	private FeatureCalculatorServiceImpl fakecarServerHandler;
	
	public DumpProcessor(FeatureCalculatorServiceImpl fakecarServerHandler) {
		this.fakecarServerHandler = fakecarServerHandler;
		if(StringUtils.isNotBlank(this.fakecarServerHandler.dumpDirPath)) {
			save_dir_path = this.fakecarServerHandler.dumpDirPath;
		}
		initDir();
	}
	/**
	 * 启动处理
	 */
	public void startDump() throws Exception {
		if(fakecarServerHandler == null) {
			throw new Exception("fakecar handler is null，DumpProcessor start failure");
		}
		LOG.info("dump processor start......");
		(new Timer()).schedule(new TimerTask() {
				@Override
		        public void run() {
					LOG.info("dump task run......");
					try {
						Map<KeyRecord, ValueRecord>[] vehicles = fakecarServerHandler.vehicles;
						int size = fakecarServerHandler.hash_size;
						if(size <= 0) {
							return;
						}
						int start = 0;
						int stepSize = size / thread_num;
						for(int i=0;i<thread_num;i++) {
							if(start >= size) {
								break;
							}
							int tmpIdx = start;
							int end = tmpIdx + stepSize;
							if(end >= size) {
								end = size - 1;
							}
							dump(tmpIdx, end, vehicles);
							start = (end + 1);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
		        }
		}, fakecarServerHandler.dumpPeriodTime, fakecarServerHandler.dumpPeriodTime);
	}
	
	/**
	 * 序列化map
	 * 目前采取全量覆盖的方式写文件
	 * @param start
	 * @param end
	 * @param vehicles
	 */
	private void dump(final int start, final int end, final Map<KeyRecord, ValueRecord>[] vehicles) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				for(int i=start;i<=end; i++) {
					if(!fakecarServerHandler.modifyFlagSet.contains(i)) {
						continue;
					}
					LOG.info("begin dump bulk [" + i + "]");
					try {
						Map<KeyRecord, ValueRecord> vMap = vehicles[i];
						if(vMap == null || vMap.isEmpty()) {
							LOG.info("bulk [" + i + "] is empty");
							continue;
						}
						String fileName = "vehicleInfo_" + i;
						String tempFileName = fileName + ".tmp";
						File tmpFile = new File(save_dir_path + tempFileName);
						FileOutputStream fos = null;
					    FileChannel fc_out = null;
					    
					    ByteArrayOutputStream bo = null;
					    ObjectOutputStream oo = null;
						try {
							bo = new ByteArrayOutputStream();
					        oo = new ObjectOutputStream(bo);
					        oo.writeObject(vMap);
					        byte[] bts = bo.toByteArray();
					  
							fos = new FileOutputStream(tmpFile);
							fc_out = fos.getChannel();
							ByteBuffer buf = ByteBuffer.wrap(bts);
							fc_out.write(buf);
							fos.flush();
							fc_out.force(true);
						} finally {
							if (null != fc_out) {
								fc_out.close();
							}
							if (null != fos) {
								fos.close();
							}
							if(oo != null) {
								oo.close();
							}
							if(bo != null) {
								bo.close();
							}
						}
						if(tmpFile.length() > 0) {
							File oldFile = new File(save_dir_path + fileName);
							if(oldFile.exists()) {
								oldFile.delete();
							}
							tmpFile.renameTo(oldFile);
						} else {
							if(tmpFile.exists()) {
								tmpFile.delete();
							}
						}
						
						fakecarServerHandler.modifyFlagSet.remove(i);
					} catch(Exception e) {
						e.printStackTrace();
						LOG.error("dump bulk [" + i + "] error", e);
					}
					
					LOG.info("end dump bulk [" + i + "]");
				}
			}
		}).start();
	}
	
	/**
	 * 从文件加载数据
	 */
	public void load() {
		File dir = new File(save_dir_path);
		File[] files = dir.listFiles();
		if(files != null && files.length > 0) {
			//CountDownLatch finishSignal = new CountDownLatch(thread_num);
			int size = files.length;
			int start = 0;
			int stepSize = size / thread_num;
			if(size % thread_num > 0) {
				stepSize += 1;
			}
			//int currentThreadNum = 0;
			for(int i=0;i<thread_num;i++) {
				if(start >= size) {
					break;
				}
				int tmpIdx = start;
				int end = tmpIdx + stepSize;
				if(end >= size) {
					end = size - 1;
				}
				//currentThreadNum ++;
				readFile(tmpIdx, end, files);
				start = (end + 1);
			}
			
			/*//将多余的信号量消费掉
			for(int i=currentThreadNum; i<thread_num; i++) {
				finishSignal.countDown();
			}
			//等待执行完毕
			try {
				finishSignal.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
		}
	}
	
	/**
	 * 从文件反序列化，并生成MAP
	 * @param start
	 * @param end
	 * @param files
	 * @param finishSignal
	 */
	@SuppressWarnings("unchecked")
	private void readFile(final int start, final int end, final File[] files) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				for(int i=start; i<=end; i++) {
					File file = files[i];
					if(file.getName().endsWith(".tmp")) {
						file.delete();
						continue;
					}
					try {
						if(file == null || file.length() <= 0) {
							continue;
						}
						FileInputStream fis = null;
						FileChannel channel = null;
						
						ByteArrayInputStream bi = null;
					    ObjectInputStream oi = null;
						try {
							fis = new FileInputStream(file);
							channel = fis.getChannel();
							ByteBuffer buffer=ByteBuffer.allocate((int)channel.size());
							channel.read(buffer);
							
							bi = new ByteArrayInputStream(buffer.array());
							oi = new ObjectInputStream(bi);
							
							Object obj = oi.readObject();
							if(obj != null) {
								String fileName = file.getName();
								int index = Integer.valueOf(fileName.split("_")[1]);
								Map<KeyRecord, ValueRecord> tmpMap = (Map<KeyRecord, ValueRecord>)obj;
								if(fakecarServerHandler.vehicles[index] == null) {
									fakecarServerHandler.vehicles[index] = tmpMap;
								} else {
									fakecarServerHandler.vehicles[index].putAll(tmpMap);
								}
							}
						} catch(Exception e) {
							e.printStackTrace();
						} finally {
							if(oi != null) {
								oi.close();
							}
							if(bi != null) {
								bi.close();
							}
							if(channel != null) {
								channel.close();
							}
							if(fis != null) {
								fis.close();
							}
						}
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
				
				//finishSignal.countDown();
			}
		}).start();
		
	}
	
	/**
	 * 创建文件存放目录
	 */
	private void initDir() {
		File dir = new File(save_dir_path);
		if(dir.exists()) {
			return;
		}
		dir.mkdirs();
	}
	
	public static void main(String[] args) throws Exception {
		File dir = new File(save_dir_path);
		dir.mkdirs();
		
		
		String fileName = "vehicleInfo_1";
		String tempFileName = fileName + ".tmp";
		File tmpFile = new File(save_dir_path + tempFileName);
		FileOutputStream fos = null;
	    FileChannel fc_out = null;
	    
	    ByteArrayOutputStream bo = null;
	    ObjectOutputStream oo = null;
	    Map<String, String> m = new HashMap<String, String>();
	    for(int i=0;i<10000;i++) {
	    	m.put("guojiawei" + i, "guojiawei" + i);
	    }
		try {
			bo = new ByteArrayOutputStream();
	        oo = new ObjectOutputStream(bo);
	        oo.writeObject(m);
	        byte[] bts = bo.toByteArray();
	  
			fos = new FileOutputStream(tmpFile);
			fc_out = fos.getChannel();
			ByteBuffer buf = ByteBuffer.wrap(bts);
			fc_out.write(buf);
			fos.flush();
			fc_out.force(true);
		} finally {
			if (null != fc_out) {
				fc_out.close();
			}
			if (null != fos) {
				fos.close();
			}
			if(bo != null) {
				bo.close();
			}
			if(oo != null) {
				oo.close();
			}
		}
		File oldFile = new File(save_dir_path + fileName);
		if(tmpFile.length() > 0) {
			if(oldFile.exists()) {
				oldFile.delete();
			}
			tmpFile.renameTo(oldFile);
		} else {
			if(tmpFile.exists()) {
				tmpFile.delete();
			}
		}
		
		FileInputStream fis = null;
		FileChannel channel = null;
		
		ByteArrayInputStream bi = null;
	    ObjectInputStream oi = null;
		try {
			fis = new FileInputStream(oldFile);
			channel = fis.getChannel();
			ByteBuffer buffer=ByteBuffer.allocate((int)channel.size());
			channel.read(buffer);
			
			bi = new ByteArrayInputStream(buffer.array());
			oi = new ObjectInputStream(bi);
			
			Object obj = oi.readObject();
			Map mm = (Map<String, String>)obj;
			System.out.println(mm.size());
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if(oi != null) {
				oi.close();
			}
			if(bi != null) {
				bi.close();
			}
			if(channel != null) {
				channel.close();
			}
			if(fis != null) {
				fis.close();
			}
		}
	}
}
