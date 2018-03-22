package netposa.firstincity.analysis.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import netposa.firstincity.analysis.utils.AnalysisConstants;
import netposa.firstincity.bloom.util.ByteBloomFilter;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BloomFilterUtils {
	
	private static final Logger LOG = LoggerFactory.getLogger(BloomFilterUtils.class);
	
	private static final double errorRate = 0.000001;
	private static final int foldFactor = 7;
	private static final int hashType = 2;
	
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");
	private static long DAY = 24 * 60 * 60 * 1000;
	
	//判断首次入城规则的ID与日期配置值
	private Map<String,Integer> rules = new HashMap<String,Integer>();
	private Map<Integer,String> rules_ids = new HashMap<Integer,String>();
	
	//filter.rule 作用于对经过车辆判断是否首次入城
	private Map<Integer,ByteBloomFilter> rule_filters = new HashMap<Integer,ByteBloomFilter>();

	private static File wlog_dir;
	private static int max_keys;
	
	private static ExecutorService pools = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	
	public BloomFilterUtils() {}
	
	public BloomFilterUtils(Properties props,final String search_date) {
		
		LOG.info("Bloom Filter init start ...");
		
		int delete_wlog_max_days = 0;
		
		try {
			
			//data and wlog config
			LOG.info("load data.dir is " + props.getProperty("data.dir"));
			String base_data_dir = StringUtils.trimToEmpty(props.getProperty("data.dir","./data"));
			
			//data_dir = new File(base_data_dir + "/" + WLogUtils.DATA_DIR);
			wlog_dir = new File(base_data_dir + "/logs");
			
			//get filter max key config
			LOG.info("load filter.max.keys is " + props.getProperty("filter.max.keys"));
			max_keys = Integer.parseInt(StringUtils.trimToEmpty(props.getProperty("filter.max.keys","10000000")));
			
			//filter rule config
			LOG.info("load filter.rule.ids is " + props.getProperty("filter.rule.ids"));
			String tmp = StringUtils.trimToEmpty(props.getProperty("filter.rule.ids","1,3,6"));
			String[] arr = tmp.split(",");
			ArrayList<String> ids_list = new ArrayList<String>();
			for(String id : arr) {
				id = StringUtils.trimToNull(id);
				if (null != id) {
					ids_list.add(id);
				}
			}
			
			LOG.info("load filter.rule.days is " + props.getProperty("filter.rule.days"));
			tmp = StringUtils.trimToEmpty(props.getProperty("filter.rule.days","30,90,180"));
			arr = tmp.split(",");
			ArrayList<Integer> days_list = new ArrayList<Integer>();
			for(String day : arr) {
				day = StringUtils.trimToNull(day);
				if (null != day) {
					int day_ = Integer.parseInt(day);
					days_list.add(day_);
					if(day_>delete_wlog_max_days){
						delete_wlog_max_days = day_;
					}
				}
			}
			if (ids_list.size() != days_list.size()) {
				LOG.error("filter.rule ids size can not equals days.");
			}
			int size = ids_list.size();
			for(int i=0; i<size; i++) {
				rules.put(ids_list.get(i),days_list.get(i));
				rules_ids.put(days_list.get(i), ids_list.get(i));
			}
			
		}finally {}
		
		
		HashMap<Integer,Future<ByteBloomFilter>> map = new HashMap<Integer,Future<ByteBloomFilter>>();
		for(final int rule : rules.values()) {
			Future<ByteBloomFilter> filter = pools.submit(new Callable<ByteBloomFilter>() {
				@Override
				public ByteBloomFilter call() throws Exception {
					return initByteBloomFilter(search_date, rule);
				}
				
			});
			map.put(rule, filter);
		}
		
		for (Entry<Integer, Future<ByteBloomFilter>> entry: map.entrySet()) {
			try {
				rule_filters.put(entry.getKey(), entry.getValue().get());
			} catch (InterruptedException e) {
				LOG.error(e.getMessage());
			} catch (ExecutionException e) {
				LOG.error(e.getMessage());
			}
		}
		
		deleteWlog(search_date, delete_wlog_max_days);
		
		LOG.info("Bloom Filter init end ...");
	}
	
	private void deleteWlog(String currentDate, int delayDate){
		
		try {
			if(wlog_dir.exists()){
				
				Date current = SDF.parse(currentDate);
				
				File[] files = wlog_dir.listFiles();
				if(null !=files && files.length>0){
					for (File f : files) {
						if(f.isFile() && f.lastModified()<current.getTime()-delayDate*DAY){
							f.delete();
							LOG.info("delete old log file " + f.getName());
						}
					}
				}
				
			}
			
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
		
	}
	
	private ByteBloomFilter initByteBloomFilter(String currentDate, int delayDate) {
		
		ByteBloomFilter filter = new ByteBloomFilter(max_keys, errorRate, hashType, foldFactor);
		
		// 分配内存空间
		filter.allocBloom();
		
		try {
			SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");
			Date current = SDF.parse(currentDate);
			
			Calendar start = Calendar.getInstance();
			start.setTime(current);
			start.add(Calendar.DAY_OF_MONTH, 0 - delayDate);
			
			Calendar end = Calendar.getInstance();
			end.setTime(current);
			
			while (start.before(end)) {
				
				File wlog_file = new File(wlog_dir, SDF.format(start.getTime()).replace("-", ""));
				
				//LOG.info("delayDate="+delayDate+" , currentDate="+currentDate + " , wlog_file=" + wlog_file.getAbsolutePath());
				
				if (wlog_file.exists()) {
					
					FileInputStream wlogStream = null;
					try {
						wlogStream = new FileInputStream(wlog_file);
						byte[] load_buffer = new byte[15*1024*100];
						int read_length = 0;
						int offset = 0;
						while ((read_length=wlogStream.read(load_buffer)) != -1) {
							offset = 0;
							for (int i = 0; i < read_length/15; i++) {
								if (!filter.contains(load_buffer, offset+1, load_buffer[offset], null)) {
									filter.add(load_buffer, offset+1, load_buffer[offset]);
								}
								offset += 15;
							}
						}
					} finally {
						if (null != wlogStream) {
							try {
								wlogStream.close();
							} catch (Exception e) {
							}
						}
						
					}
					
				}
				
				start.add(Calendar.DAY_OF_MONTH, 1);
			}
			
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
		
		return filter;
	}

	public Map<String, Integer> getRules() {
		return rules;
	}

	public Map<Integer, String> getRulesIds() {
		return rules_ids;
	}

	public Map<Integer, ByteBloomFilter> getRuleFilters() {
		return rule_filters;
	}
	
	public static void main(String[] args) {
		
		/*Properties props = new Properties();
		try {
			InputStream inStream = BloomFilterUtils.class.getClassLoader().getResourceAsStream(AnalysisConstants.PROP_CONF_NAME());
			props.load(inStream);
			inStream.close();
			
			BloomFilterUtils utils = new BloomFilterUtils(props, "2016-07-07");
			Map<Integer, ByteBloomFilter> filters = utils.getRuleFilters();
			filters.get("");
			
		} catch(Exception e){
			
		}finally {}*/
		
		/*double errorRate = 0.000001;
		int foldFactor = 7;
		int hashType = 2;
		int max_keys = 10000000;
		
		ByteBloomFilter filter = new ByteBloomFilter(max_keys, errorRate, hashType, foldFactor);
		
		// 分配内存空间
		filter.allocBloom();
		
		String hphm = "苏D9008X";
		for (int i = 0; i < 10; i++) {
			String hpys = ""+i;
			byte[] buf = (hphm+hpys).getBytes();
			
			if(!filter.contains(buf, 0, buf.length, null)){
				filter.add(buf);
			}
		}
		
		System.out.println(filter.contains((hphm+"0").getBytes(), 0, (hphm+"1").getBytes().length, null));;*/
		
		Properties props = new Properties();
		try {
			InputStream inStream = BloomFilterUtils.class.getClassLoader().getResourceAsStream(AnalysisConstants.PROP_CONF_NAME());
			props.load(inStream);
			inStream.close();
		} catch(Exception e){
			
		}finally {}
		
		BloomFilterUtils utils = new BloomFilterUtils(props, "2016-03-10");
		Map<Integer, ByteBloomFilter> filters = utils.getRuleFilters();
		Map<Integer, String> rulesIds = utils.getRulesIds();
		String hphm = "苏L0D683";
		String hpys = "2";
		for (Entry<Integer, String> entry : rulesIds.entrySet()) {
			ByteBloomFilter filter = filters.get(entry.getKey());
			System.out.println(filter.contains((hphm+hpys).getBytes(), 0, (hphm+hpys).getBytes().length, null));
			System.out.println("key=" + entry.getKey() + " , value=" + entry.getValue());
		}
	}
	
}
