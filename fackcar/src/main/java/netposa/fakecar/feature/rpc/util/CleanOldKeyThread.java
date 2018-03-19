package netposa.fakecar.feature.rpc.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import netposa.fakecar.feature.rpc.FeatureCalculatorServiceImpl;
import netposa.fakecar.feature.rpc.KeyRecord;
import netposa.fakecar.feature.rpc.ValueRecord;

/**
 * 定期清除长时间没有活动的过车记录数据
 * @author hongs.yang
 *
 */
public class CleanOldKeyThread extends Thread {
	private static final Logger LOG = LoggerFactory.getLogger(CleanOldKeyThread.class);
	
	private FeatureCalculatorServiceImpl handler;
	private int cleanDay;
	
	public CleanOldKeyThread(FeatureCalculatorServiceImpl handler, int cleanDay) {
		this.handler = handler;
		this.cleanDay = cleanDay;
	}

	@Override
	public void run() {
		LOG.info("clean old key thread run begin ........");
		long now_time = System.currentTimeMillis();
		int index = 0;
		for(Map<KeyRecord, ValueRecord> map : handler.vehicles) {
			if (map == null || map.size() < 1) {
				continue;
			}
			Iterator<Entry<KeyRecord, ValueRecord>> itor = 
					map.entrySet().iterator();
			
			
			while(itor.hasNext()) {
				Entry<KeyRecord, ValueRecord> entry = itor.next();
				if (now_time >= (cleanDay + entry.getValue().getInitTime())) {
					LOG.info("delete old key is " + 
				            entry.getKey().toString() + ", this record init time is " + 
							entry.getValue().getInitTime());
					itor.remove();
					
					handler.modifyFlagSet.add(index);
				}
			}
			
			index ++;
		}
		LOG.info("clean old key thread run end   ........");
	}
	
	

}
