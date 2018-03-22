package netposa.firstincity.analysis.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import netposa.firstincity.analysis.utils.AnalysisConstants;
import netposa.firstincity.analysis.utils.AnalysisUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PreWriteLogs {

	private static Log LOG = LogFactory.getLog(PreWriteLogs.class);

	private static SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");

	private Calendar startDate;
	private Calendar endDate;

	private static String opaqUrl;
	private static String opaqUser;
	private static String opaqPassword;
	private static int maxTasks = 24;
	
	private static ExecutorService pools = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	
	private static File wlog_dir;

	public PreWriteLogs(String currentDate, Properties properties) {
		
		String base_data_dir = StringUtils.trimToEmpty(properties.getProperty("data.dir","./data"));
		wlog_dir = new File(base_data_dir + "/logs");

		try {
			this.endDate = Calendar.getInstance();
			this.endDate.setTime(SDF.parse(currentDate));
		} catch (ParseException e) {
			LOG.error(e.getMessage());
		}
		
		String tasks = properties.getProperty("task.execute.max.size","24");
		if (null != tasks && tasks.length()>0) {
			maxTasks = Integer.valueOf(tasks);
		}

		int maxDelayDays = 0;
		for (String delayDay : properties.getProperty("compute.delay.day","1,14").split(",")) {
			try {
				int day = Integer.parseInt(delayDay);
				if (day > maxDelayDays) {
					maxDelayDays = day;
				}
			} catch (NumberFormatException e) {
				LOG.info("compute.delay.day is not a int number , " + e.getMessage());
			}
		}

		int maxRuleDays = 0;
		for (String ruleDays : properties.getProperty("filter.rule.days", "30,90,180").split(",")) {
			try {
				int day = Integer.parseInt(ruleDays);
				if (day > maxRuleDays) {
					maxRuleDays = day;
				}
			} catch (NumberFormatException e) {
				LOG.info("filter.rule.days is not a int number , " + e.getMessage());
			}
		}

		try {
			startDate = Calendar.getInstance();
			startDate.setTime(SDF.parse(currentDate));
			startDate.add(Calendar.DAY_OF_MONTH, 0 - maxDelayDays - maxRuleDays);
		} catch (ParseException e) {
			LOG.error(e.getMessage());
		}

		opaqUrl = properties.getProperty("db.opaq.url", "");
		opaqUser = properties.getProperty("db.opaq.user", "");
		opaqPassword = properties.getProperty("db.opaq.password", "");

	}
	
	public void updateLog(final String updateTime, Properties properties) {
		
		if (null != opaqUrl && opaqUrl.length() > 0) {
			writeLog(updateTime,true, properties);
		}else{
			LOG.error("db.opaq.url is null .");
		}
	}

	public void writeLog(Properties properties) {

		if (null != opaqUrl && opaqUrl.length() > 0) {
			while (startDate.before(endDate)) {
				writeLog(SDF.format(startDate.getTime()),false, properties);
				startDate.add(Calendar.DAY_OF_MONTH, 1);
			}
		}else{
			LOG.error("db.opaq.url is null .");
		}

	}
	
	public void writeLog(final String date, boolean deleteOldFile, final Properties properties) {
		
		if(null != date && date.length()>0){
			
			File logfFile = new File(wlog_dir,date.replaceAll("-", ""));
			
			try {
				if(logfFile.exists()){
					if(deleteOldFile){
						logfFile.delete();
					}else{
						return;
					}
				}
				if(!logfFile.getParentFile().exists()){
					logfFile.getParentFile().mkdirs();
				}
				logfFile.createNewFile();
			} catch (Exception e) {
				LOG.error(e.getMessage());
			}
			
			ArrayList<Future<Set<String>>> executes = new ArrayList<Future<Set<String>>>();
			
			try {
				final List<String> splits = AnalysisUtil.getSplits(date, maxTasks);
				for(int i=1; i<splits.size(); i++) {
					final int index = i;
					Future<Set<String>> future = pools.submit(new Callable<Set<String>>() {
						@Override
						public Set<String> call() throws Exception {
							return getDataFromOPAQ(splits.get(index-1),splits.get(index), properties);
						}
					});
					executes.add(future);
				}
			} catch (IOException e) {
				LOG.error(e.getMessage());
			} catch (InterruptedException e) {
				LOG.error(e.getMessage());
			}
			
			Set<String> set = new HashSet<>();
			for (Future<Set<String>> future : executes) {
				try {
					set.addAll(future.get());
				} catch (Exception e) {
					LOG.error(e.getMessage());
				}
			}
			executes.clear();
			executes = null;
			if(null!=set&& set.size()>0){
				writeLogFile(logfFile, set);
			}
			set.clear();
			set = null;
		}
		System.gc();
	}

	private Set<String> getDataFromOPAQ(String start,String end, Properties properties) {
		
		Set<String> set = new HashSet<>();
		String table_name = properties.getProperty("db.opaq.tablename");
		try {
			Connection connection = getConnection(properties);
			
			String sql = "select hphm,hpys from " + table_name + " where jgsj>='"+start+"' and jgsj<='"+end+"' and hphm not in ('00000000','无车牌','非机动车')";
			
			Statement st = connection.createStatement();
			
			ResultSet rs = st.executeQuery(sql);
			if(rs != null){
				while (rs.next()) {
					if(null!=rs.getString("hphm")&&null!=rs.getString("hphm")){
						set.add(rs.getString("hphm")+rs.getString("hpys"));
					}
				}
			}
			LOG.info(sql);
			rs.close();
			st.close();
			connection.close();
		} catch (Exception e) {
			LOG.error(e.getMessage());
			try {
				Thread.sleep(30*1000);
			} catch (InterruptedException e1) {
				LOG.error(e1.getMessage());
			}
			return getDataFromOPAQ(start, end, properties);
		}

		return set;
	}

	private void writeLogFile(File logfFile, Set<String> set) {
		
		FileOutputStream log = null;
		try {
			if(!logfFile.exists()){
				logfFile.createNewFile();
			}
			log = new FileOutputStream(logfFile, true);
			byte[] val = new byte[15];
			for (String value : set) {
				if(null != value && value.length()>0){
					byte[] val_ = value.getBytes();
					if(val_.length<=14){
						val[0] = (byte) val_.length;
						System.arraycopy(val_, 0, val, 1, val_.length);
						log.write(val);
					}else {
						LOG.error("---------------------- value="+value);
					}
				}
			}
			set.clear();
			log.flush();
			log.close();
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}finally {
		  if(log != null){
			  try {
				log.flush();
				log.close();
			} catch (IOException e) {
				LOG.error(e.getMessage());
			}
		  }
		}
		
	}

	private Connection getConnection(Properties properties) {
		Connection connection = null;
		try {
			//String opaqDriver = properties.getProperty("db.opaq.driver");
			Class.forName(properties.getProperty("db.opaq.driver"));
			//LOG.info("++++++++++++" + opaqDriver + "+++++++++++++++");
			if (opaqUser == null) {
				connection = DriverManager.getConnection(opaqUrl);
			} else {
				connection = DriverManager.getConnection(opaqUrl, opaqUser, opaqPassword);
			}
		} catch (Exception e) {
			LOG.info("analysis.jdbc.url="+opaqUrl+" , analysis.jdbc.username="+opaqUser+" , analysis.jdbc.password="+opaqPassword);
			LOG.info("Get OPAQ Connection fail , message is : " + e.getMessage());
			try {
				if(null!=connection) connection.close();
			} catch (SQLException e2) {
				LOG.info("OPAQ Connection close fail , message is : " + e2.getMessage());
			}
			connection = null;
			try {
				Thread.sleep(30*1000);
			} catch (InterruptedException e1) {
				LOG.error(e1.getMessage());
			}
			connection = getConnection(properties);
		}
		return connection;
	}
	
}
