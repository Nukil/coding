package com.netposa.poseidon.face.service;

import com.netposa.poseidon.face.bean.CacheFaceFeature;
import com.netposa.poseidon.face.util.HbaseUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.compute.ComputeJobAdapter;
import org.apache.ignite.configuration.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class LoadFaceDataJob extends ComputeJobAdapter implements Serializable {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = LoggerFactory.getLogger(LoadFaceDataJob.class);
	private static SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd");
	private static CacheConfiguration<String, CacheFaceFeature> CACHE_CFG = new CacheConfiguration<>();
	private static  IgniteCache<String, String> igniteCache = Ignition.ignite("Netposa").cache( "ZK_FACE_GLOBAL");
	private static final String ZK_QUORUM = igniteCache.get("ZK_QUORUM");
	private static final String CLIENT_PORT = igniteCache.get("CLIENT_PORT");
	private static final IgniteCache<String, Properties> SERVER_CONF = Ignition.ignite("Netposa").cache( "Netposa_FACE_GLOBAL");
	private static byte[] FAMILY = "cf".getBytes();
	private static byte[] CAMERAID = "cameraId".getBytes();
	private static byte[] JLBH = "logNum".getBytes();
	private static byte[] FEATURE = "feature".getBytes();
	private static byte[] GATHERTIME = "gatherTime".getBytes();
	private String hostName;
	private String startKey;
	private String IGNITE_NAME;
	static {
		// 设置数据缓存策略
        CACHE_CFG.setCacheMode(CacheMode.PARTITIONED);
		// 设置动态缓存,新加入的数据会自动分散在各个server
        CACHE_CFG.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        CACHE_CFG.setMemoryPolicyName("MeM_RANDOM_2_LRU");
		CACHE_CFG.setRebalanceBatchSize(Integer.parseInt(SERVER_CONF.get("SERVER").getProperty("rebalance.batchSize.bytes","524288")));
		HbaseUtil.hConfiguration.set("hbase.zookeeper.quorum",ZK_QUORUM);
		HbaseUtil.hConfiguration.set("hbase.zookeeper.property.clientPort",CLIENT_PORT);

	}

	public LoadFaceDataJob(String hostName, String startKey, String igniteName) {
		this.hostName = hostName;
		this.startKey = startKey;
		this.IGNITE_NAME = igniteName;
	}

	@Override
	public Integer execute() throws IgniteException {
		int size_sum = 0;
        IgniteCache<String, Properties> cache = Ignition.ignite(IGNITE_NAME).cache(IGNITE_NAME + "_FACE_GLOBAL");
        Properties prop = cache.get("SERVER");
		Table table=null;
		ResultScanner scanner =null;
		try {
			String localhost = InetAddress.getLocalHost().getCanonicalHostName();
			long start = new SimpleDateFormat("yyyy-MM-dd").parse(prop.getProperty("start.time")).getTime();
			if (hostName.equals(localhost)) {
				LOG.info("load face data start,startKey =>[" + startKey + "]! localhost is :" + localhost);
				Map<String, CacheFaceFeature> flushMap = new HashMap<>();
				long time1 = System.currentTimeMillis();
				Connection conn = HbaseUtil.getConn();
				table = conn.getTable(TableName.valueOf(prop.getProperty("face.table.name")));
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DATE, (-1) * Integer.parseInt(prop.getProperty("duration.time")));
				long startTime = cal.getTimeInMillis() >= start ? cal.getTimeInMillis() : start;
				cal.setTimeInMillis(System.currentTimeMillis());
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                cal.add(Calendar.DATE, 1);
				while (cal.getTimeInMillis() > startTime) {
					int size = 0;
					long time2 = System.currentTimeMillis();
					String endRow = startKey + cal.getTimeInMillis();
					cal.add(Calendar.DATE, -1);
					String current_date = SDF.format(new java.util.Date(cal.getTimeInMillis()));
                    CACHE_CFG.setName(IGNITE_NAME + "_FACE_" + current_date);
					String startRow = startKey + cal.getTimeInMillis();
					Scan scan = new Scan();
					scan.setStartRow(startRow.getBytes());
					scan.setStopRow(endRow.getBytes());
					scan.setCaching(10000);
					scanner = table.getScanner(scan);
					int flag = 0;
                    IgniteCache<String, CacheFaceFeature> igniteCache = null;
					Ignition.ignite(IGNITE_NAME).getOrCreateCache(CACHE_CFG);
                    for (Result result : scanner) {
                        if (result != null
                                && result.getValue(FAMILY, JLBH) != null
                                && result.getValue(FAMILY, GATHERTIME) != null
                                && result.getValue(FAMILY, CAMERAID) != null
                                && result.getValue(FAMILY, FEATURE) != null) {
                            size++;
/*                            if (flag == 0) {
                                igniteCache = Ignition.ignite(IGNITE_NAME).getOrCreateCache(CACHE_CFG);
                                flag = 1;
                            }*/

/*                            igniteCache.put(new String(result.getValue(FAMILY, JLBH)),
                                    new CacheFaceFeature(result.getValue(FAMILY, GATHERTIME),
                                            result.getValue(FAMILY, CAMERAID),
                                            result.getValue(FAMILY, FEATURE)));*/
                            flushMap.put(new String(result.getValue(FAMILY, JLBH)),
									new CacheFaceFeature(result.getValue(FAMILY, GATHERTIME),
											result.getValue(FAMILY, CAMERAID),
											result.getValue(FAMILY, FEATURE)));

							if(flushMap.size()>500){
								Ignition.ignite(IGNITE_NAME).getOrCreateCache(CACHE_CFG).putAll(flushMap);
								flushMap.clear();
							}

                        }
                    }
                    if(flushMap.size()>0){
						Ignition.ignite(IGNITE_NAME).getOrCreateCache(CACHE_CFG).putAll(flushMap);
                    	flushMap.clear();
					}
                    size_sum += size;
					long time3 = System.currentTimeMillis();
					LOG.info("current_date,startRow,endRow =>> [" + CACHE_CFG.getName() + "," + startRow + "," + endRow + "] ,=>load " + size +
							" data and put cache used time  ====>>> " + (time3 - time2) + "ms!");
				}
				LOG.info("===LOCALHOST=[" + localhost + "],load " + size_sum
						+ " data and put cache used time  ====>>> "
						+ (System.currentTimeMillis() - time1) + "ms!");
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}finally {
			try{
				if (scanner != null) {
					scanner.close();
				}

				if(table !=null){
					table.close();
				}
			}catch (IOException e){
				LOG.error(e.getMessage(),e);
			}
		}
		return size_sum;
	}
}
