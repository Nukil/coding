package netposa.firstincity.analysis.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import netposa.firstincity.analysis.lib.db.FirstincityOutputValue;
import netposa.firstincity.analysis.lib.db.OpaqInputKey;
import netposa.firstincity.analysis.lib.db.OpaqInputValue;
import netposa.firstincity.analysis.utils.AnalysisUtil;
import netposa.firstincity.bloom.util.ByteBloomFilter;


import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FristincityAnalysisServer {
	
	private static Log LOG = LogFactory.getLog(FristincityAnalysisServer.class);
	
	private boolean debug = true;
	
	private boolean excludeLocalPlateEnable = true;
	private String localPlateStartWith;
	
	private String[] excludeCarnumStartWith;
	private String[] excludeCarnumEndWith;
	private String[] excludeCarnumContain;
	private String[] excludeCarnumEqual;
	private String[] excludePlateColorEqual;
	
	private static SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static int tasks = 24;
	//private static String table_name;
	private static String selectSql;
	String insertSql = "INSERT ignore INTO hadoop_firstinto (TRAFFICID_,PASSTIME_,SPEED_,ORGID_,ROADMONITORID_,CHANNELID_,PLATENUMBER_,PLATECOLOR_,PLATETYPE_,VEHICLECOLOR_,VEHICLETYPE_,VEHICLEBRAND_,VEHICLESUBBRAND_,VEHICLEMODELYEAR_,backType_) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private static ExecutorService pools = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	
	private void init(Properties props){
		selectSql = props.getProperty("db.opaq.selectSql");

		String debugStr = props.getProperty("service.process.debug.enable","true");
		if (null != debugStr && debugStr.length()>0) {
			debug = Boolean.valueOf(debugStr);
		}
		
		String maxTasks = props.getProperty("task.execute.max.size","24");
		if (null != maxTasks && maxTasks.length()>0) {
			tasks = Integer.valueOf(maxTasks);
		}
		
		String localEnable = props.getProperty("exclude.local.plate.enable","false");
		if (null != localEnable && localEnable.length()>0) {
			excludeLocalPlateEnable = Boolean.valueOf(localEnable);
		}
		
		String localPlate = props.getProperty("exclude.local.plate.startwith","");
		if (null != localPlate && localPlate.length()>0) {
			localPlateStartWith = localPlate;
		}
		
		String startwith = props.getProperty("exclude.licence_plate.startwiths","");
		if (null != startwith && startwith.length()>0) {
			excludeCarnumStartWith = startwith.split(",");
		}
		String endwith = props.getProperty("exclude.licence_plate.endwiths","");
		if (null != endwith && endwith.length()>0) {
			excludeCarnumEndWith = endwith.split(",");
		}
		String contain = props.getProperty("exclude.licence_plate.contains","");
		if (null != contain && contain.length()>0) {
			excludeCarnumContain = contain.split(",");
		}
		String equals = props.getProperty("exclude.licence_plate.equals","");
		if (null != equals && equals.length()>0) {
			excludeCarnumEqual = equals.split(",");
		}
		
		String plateColor = props.getProperty("exclude.licence_plate_color.equals","");
		if (null != plateColor && plateColor.length()>0) {
			excludePlateColorEqual = plateColor.split(",");
		}
	}
	
	public boolean fristincityAnalysis(final String searchDate, final Properties props) throws ParseException{

		init(props);
		
		ArrayList<Future<Map<OpaqInputKey, OpaqInputValue>>> executes = 
				new ArrayList<Future<Map<OpaqInputKey, OpaqInputValue>>>();
		try {
			List<String> splits = getSplits(searchDate);
			for (final String sql : splits) {
				Future<Map<OpaqInputKey, OpaqInputValue>> futures = pools.submit(new Callable<Map<OpaqInputKey,OpaqInputValue>>() {
					@Override
					public Map<OpaqInputKey, OpaqInputValue> call() throws Exception {
						return selectValues(sql,props);
					}
				});
				if(null != futures){
					executes.add(futures);
				}
			}
		} catch (IOException e) {
			LOG.error(e.getMessage());
		} catch (InterruptedException e) {
			LOG.error(e.getMessage());
		}
		
		//pools.shutdown();
		
		if(null == executes || executes.size()<1){
			if(debug) LOG.info("(executes) the data of " + searchDate + " is empty ......");
			return true;
		}
		
		Map<OpaqInputKey, OpaqInputValue> map = merge(executes);
		
		executes.clear();
		executes=null;
		if(null == map || map.size()<1){
			if(debug) LOG.info("merge(executes) the data of " + searchDate + " is empty ......");
			return true;
		}
		
		if(debug) LOG.info("Map<OpaqInputKey, OpaqInputValue> map.size()=" + map.size());
		
		List<FirstincityOutputValue> outputVal = filter(map, searchDate, props);
		
		map.clear();
		map = null;
		if(null == outputVal || outputVal.size()<1){
			if(debug) LOG.info("(outputVal) the data of " + searchDate + " is empty ......");
			return true;
		}
		
		if(debug) LOG.info("List<FirstincityOutputValue> outputVal.size()=" + outputVal.size());
		
		updateMysql(outputVal, searchDate, props);
		outputVal.clear();
		outputVal = null;
		System.gc();
		
		return true;
		
	}
	
	private void updateMysql(List<FirstincityOutputValue> outputVal, String searchDate, Properties props){
		
		Connection connection = null;
		PreparedStatement statement = null;
		
		int batchSize = 0;
		
		StringBuffer deleteSql = new StringBuffer();
		deleteSql.append("DELETE FROM hadoop_firstinto WHERE PASSTIME_ >= '" + searchDate + " 00:00:00' and PASSTIME_ <= '" + searchDate + " 23:59:59'");
		
		try {
			connection = getMysqlConnection(props);
			connection.setAutoCommit(false);
			
			try {
				if(debug) LOG.info(deleteSql.toString());
				connection.createStatement().executeUpdate(deleteSql.toString());
			} catch (Exception e) {
				LOG.error(e.getMessage());
				try {
					Thread.sleep(1000*60);
				} catch (InterruptedException e1) {
					if(debug) LOG.error(e1.getMessage());
				}
				if(debug) LOG.info("update first in city data again ...");
				updateMysql(outputVal, searchDate, props);
			}
			
			connection.commit();
			if(debug) LOG.info("delete the data of "+searchDate+" success ......");
			
			if(debug) LOG.info(insertSql);
			if(debug) LOG.info("outputVal.size()="+outputVal.size());
			statement = connection.prepareStatement(insertSql);
			
			for (FirstincityOutputValue value : outputVal) {
				statement.setString(1, value.getTrafficid());
				statement.setString(2, SDF.format(new Date(value.getPasstime())));
				statement.setInt(3, value.getSpeed());
				statement.setString(4, value.getOrgid());
				statement.setString(5, value.getRoadmonitorid());
				statement.setString(6, value.getChannelid());
				statement.setString(7, value.getPlatenumber());
				statement.setString(8, value.getPlatecolor());
				statement.setString(9, value.getPlatetype());
				statement.setString(10, value.getVehiclecolor());
				statement.setString(11, value.getVehicletype());
				statement.setString(12, value.getVehiclebrand());
				statement.setString(13, value.getVehiclesubbrand());
				statement.setString(14, value.getVehiclemodelyear());
				statement.setString(15, value.getBacktype());
				
				statement.addBatch();
				batchSize += 1;
				
				if (batchSize >= 5000) {
					statement.executeBatch();
					connection.commit();
					statement.clearBatch();
					batchSize = 0;
					if(debug) LOG.info("commit 5000 first in city data ......");
				}
			}
			
			statement.executeBatch();
			connection.commit();
			statement.clearBatch();
			
			statement.close();
			connection.close();
		} catch (SQLException e) {
			LOG.error(e.getMessage());
			try {
				Thread.sleep(1000*60);
			} catch (InterruptedException e1) {
				if(debug) LOG.error(e1.getMessage());
			}
			if(debug) LOG.info("update first in city data again ...");
			updateMysql(outputVal, searchDate, props);
		}finally{
			try {
				if(statement != null){
					statement.close();
				}
				if(connection != null){
					connection.close();
				}
			} catch (SQLException e) {
				LOG.error(e.getMessage());
			}
		}
		
	}
	
	private List<FirstincityOutputValue> filter(Map<OpaqInputKey, OpaqInputValue> map, String searchDate, Properties props){
		
		BloomFilterUtils utils = new BloomFilterUtils(props, searchDate);
		Map<Integer, ByteBloomFilter> filters = utils.getRuleFilters();
		Map<Integer, String> rulesIds = utils.getRulesIds();
		
		List<FirstincityOutputValue> outputVal = new ArrayList<>();
		
		for (Entry<OpaqInputKey, OpaqInputValue> entry : map.entrySet()) {
			byte[] hphm_hpys = entry.getKey().getPlateAndColor();
			for (Entry<Integer, String> rulesId : rulesIds.entrySet()) {
				ByteBloomFilter filter = filters.get(rulesId.getKey());
				if(null != filter){
					boolean flag = filter.contains(hphm_hpys, 0, hphm_hpys.length, null);
					if(!flag){
						outputVal.add(new FirstincityOutputValue(entry.getValue(),rulesId.getValue()));
					}
				}
			}
		}
		filters.clear();
		filters = null;
		rulesIds.clear();
		rulesIds = null;
		utils = null;
		System.gc();
		return outputVal;
	}
	
	private Map<OpaqInputKey, OpaqInputValue> selectValues(String sql, Properties props){
//		System.out.println(sql);
		Map<OpaqInputKey, OpaqInputValue> map = null;
		
		Connection connection = null;
		Statement statement = null;
		ResultSet resultset = null;
		
		try {
			connection = getOpaqConnection(props);
			statement = connection.createStatement();
			resultset = statement.executeQuery(sql);
			map = new HashMap<>();
			while(resultset.next()){
				if(filterValue(resultset)){
					OpaqInputKey key = new OpaqInputKey(resultset.getString("hphm"), resultset.getString("hpys"));
					if(null == map.get(key) && key.getPlateAndColor().length<=14){
						OpaqInputValue value = new OpaqInputValue();
						value.readFields(resultset, "");
						map.put(key, value);
					}
				}
			}
			resultset.close();
			statement.close();
			connection.close();
		} catch (SQLException e) {
			LOG.error(e.getMessage());
			return selectValues(sql, props);
		}finally{
			try {
				if(resultset != null){
					resultset.close();
				}
				if(statement != null){
					statement.close();
				}
				if(connection != null){
					connection.close();
				}
			} catch (SQLException e) {
				LOG.error(e.getMessage());
			}
		}
//		System.out.println(map);
		return map;
	}
	
	private boolean filterValue(ResultSet results){
		
		try {
			
			// Set the key field value as the output key value
			String plateNum = results.getString("hphm");
			//是否是带卡口字段的SQL查询语句,如果执行if内的操作
			plateNum = StringUtils.trimToNull(plateNum);
			if (null == plateNum) {
				return false;
			}
			
			//本地车牌过滤
			if(excludeLocalPlateEnable && null != localPlateStartWith && localPlateStartWith.length()>0){
				if (plateNum.startsWith(localPlateStartWith)) {
					return false; 
				}
			}
			
			//检查车牌是否是排除列表中的车牌
			if (null != excludeCarnumStartWith && excludeCarnumStartWith.length > 0) {
				for(String str : excludeCarnumStartWith) {
					if (plateNum.startsWith(str)) {
						return false; 
					}
				}
			}
			
			if (null != excludeCarnumEndWith && excludeCarnumEndWith.length > 0) {
				for(String str : excludeCarnumEndWith) {
					if (plateNum.endsWith(str)) {
						return false;
					}
				}
			}
			
			if (null != excludeCarnumContain && excludeCarnumContain.length > 0) {
				for(String str : excludeCarnumContain) {
					if (plateNum.contains(str)) {
						return false;
					}
				}
			}
			
			if (null != excludeCarnumEqual && excludeCarnumEqual.length > 0) {
				for(String str : excludeCarnumEqual) {
					if (plateNum.equalsIgnoreCase(str)) {
						return false;
					}
				}
			}
			
			String plateColor = StringUtils.trimToNull(results.getString("hpys"));
			if (null == plateColor) {
				return false;
			}
			
			if (null != excludePlateColorEqual && excludePlateColorEqual.length > 0) {
				for(String str : excludePlateColorEqual) {
					if (plateColor.equalsIgnoreCase(str)) {
						return false;
					}
				}
			}
			
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
		
		return true;
	}
	
	private Map<OpaqInputKey, OpaqInputValue> merge(ArrayList<Future<Map<OpaqInputKey, OpaqInputValue>>> executes){
		
		Map<OpaqInputKey, OpaqInputValue> map = new HashMap<>();
		if(executes.size()>0){
			try {
				for (Future<Map<OpaqInputKey, OpaqInputValue>> futures : executes) {
					if(map.size()==0 && null != futures.get()){
						map.putAll(futures.get());
					}else {
						for(Entry<OpaqInputKey, OpaqInputValue> entry : futures.get().entrySet()){
							if(null == map.get(entry.getKey())){
								map.put(entry.getKey(), entry.getValue());
							}else {
								if(entry.getValue().getJgsj()<map.get(entry.getKey()).getJgsj()){
									map.put(entry.getKey(), entry.getValue());
								}
							}
						}
					}
				}
			} catch (InterruptedException e) {
				LOG.error(e.getMessage());
			} catch (ExecutionException e) {
				LOG.error(e.getMessage());
			}
		}
		
		return map;
	}
	
	/**
	 * 
	 * @param props
	 * @return
	 */
	public Connection getMysqlConnection(Properties props) {
		
		Connection connection = null;
		
		while (null == connection) {
			try {
				Class.forName("com.mysql.jdbc.Driver");
				if(props.getProperty("db.mysql.user") == null) {
					connection = DriverManager.getConnection(props.getProperty("db.mysql.url"));
				} else {
					connection = DriverManager.getConnection(
							props.getProperty("db.mysql.url"), 
							props.getProperty("db.mysql.user"), 
							props.getProperty("db.mysql.password"));
				}
			} catch (Exception e) {
				if(debug) LOG.info("db.mysql.url="+props.get("db.mysql.url")
						+" , db.mysql.user="+props.get("db.mysql.user")
						+" , db.mysql.password="+props.get("db.mysql.password"));
				if(debug) LOG.info("Get MySql Connection fail , message is : " + e.getMessage());
				try {
					Thread.sleep(1000 * 60);
				} catch (InterruptedException e1) {
					LOG.error(e.getMessage());
				}
				connection = null;
			}
		}
		
		return connection;
	}
	
	/**
	 * 
	 * @param props
	 * @return
	 */
	public Connection getOpaqConnection(Properties props) {
		
		Connection connection = null;
		while (null == connection) {
			try {
				Class.forName("com.mysql.jdbc.Driver");
				String opaqDriver = props.getProperty("db.opaq.driver");
				Class.forName(props.getProperty("db.opaq.driver"));
				//LOG.info("++++++++++++" + opaqDriver + "+++++++++++++++");
				if(props.getProperty("db.opaq.user") == null) {
					connection = DriverManager.getConnection(props.getProperty("db.opaq.url"));
				} else {
					connection = DriverManager.getConnection(
							props.getProperty("db.opaq.url"), 
							props.getProperty("db.opaq.user"), 
							props.getProperty("db.opaq.password"));
				}
			} catch (Exception e) {
				if(debug) LOG.info("db.opaq.url="+props.get("db.opaq.url")
						+" , db.opaq.user="+props.get("db.opaq.user")
						+" , db.opaq.password="+props.get("db.opaq.password"));
				if(debug) LOG.info("Get OPAQ Connection fail , message is : " + e.getMessage());
				try {
					Thread.sleep(1000 * 60);
				} catch (InterruptedException e1) {
					LOG.error(e.getMessage());
				}
				connection = null;
			}
		}
		
		return connection;
	}
	
	public List<String> getSplits(String searchDate) throws IOException, InterruptedException, ParseException {
		//String selectSql = "SELECT jlbh,jgsj,0 as clsd,0 as xzqh,sbbh as kkbh,0 as cdbh,hphm,hpys,hpzl,csys,cllx,clpp,clzpp,clnk FROM " + table_name + " WHERE hphm not in ('00000000','非机动车','无车牌') ";
		List<String> selectSqls = new ArrayList<>();
		List<String> splits = AnalysisUtil.getSplits(searchDate, tasks);
		SimpleDateFormat sdf =   new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );  

		for(int i=1; i<splits.size(); i++) {
			StringBuffer sqlBuf = new StringBuffer();
			sqlBuf.append(selectSql);
			sqlBuf.append(" and jgsj >= " + sdf.parse(splits.get(i-1)).getTime());
			sqlBuf.append(" and jgsj < " + sdf.parse(splits.get(i)).getTime() + " order by jgsj");
			selectSqls.add(sqlBuf.toString());
			//LOG.info(sqlBuf.toString());
		}
		
		return selectSqls;
	}
	
}
