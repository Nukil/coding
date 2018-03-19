package netposa.fakecar.feature.rpc.error;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import netposa.fakecar.feature.rpc.ResultRecord;
import netposa.fakecar.feature.rpc.ValueRecord;
import netposa.fakecar.feature.rpc.search.SimilarVehicleSearchThread;
import netposa.fakecar.feature.rpc.search.SimilarVehicleTableConfLoadUtils;
import netposa.fakecar.feature.rpc.search.SimilarVehicleTableDef;
import netposa.fakecar.feature.rpc.util.FeatureTableConfLoadUtils;
import netposa.fakecar.feature.rpc.util.FeatureTableDef;
import netposa.fakecar.feature.rpc.util.ThreadExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 向数据库写入套牌分析结果数据
 * @author dl
 *
 */
public class InsertErrorSqlToDbThread extends Thread {
	private static final Logger LOG = LoggerFactory.getLogger(InsertErrorSqlToDbThread.class);
	
	private final FeatureTableDef featureTableDef ;
	private final SimilarVehicleTableDef similarTableDef ;
	
	private final int flush_interval = 1000 * 60 ;
	
	private boolean running = false;
	
	private long lastFlushTime = 0;
	
	private Connection feature_connection;
	private Statement feature_statement;
	private Connection similar_connection;
	private Statement similar_statement;
	
	private SimilarVehicleSearchThread searchThread;
	
	private int idLength = 20;
	private int featureSize = 288;
	
	private boolean debug = false;
	
	public InsertErrorSqlToDbThread(SimilarVehicleSearchThread searchThread,boolean debug) throws Exception {
		
		this.featureTableDef = FeatureTableConfLoadUtils.loadConfigByClassPath();
		this.similarTableDef = SimilarVehicleTableConfLoadUtils.loadConfigByClassPath();
		
		running = true;
		lastFlushTime = 0;
		this.searchThread = searchThread;
		this.debug = debug;
		
		getFeatureConnection();
		getSimilarConnection();
		
		this.setUncaughtExceptionHandler(new ThreadExceptionHandler());
	}
	
	public void close() {
		running = false;
		closeFeatureConnection();
		closeSimilarConnection();
	}
	
	
	@Override
	public void run() {
		while(running) {
			try {
				if ((lastFlushTime + flush_interval) < System.currentTimeMillis()) {
					execute();
					LOG.info("flush local data to database by timeout" );
				}
				
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					LOG.warn("flush sleep error:" + e.getMessage(),e);
				}
				
			}catch(Exception ex) {
				LOG.warn("THIS WHILE ERROR => " + ex.getMessage(),ex);
			}
		}
	}
	
	
	/**
	 * 执行数据库写入操作
	 * @return
	 */
	private void execute() {
		
		LOG.info("flush local feature data to database ......" );
		List<String> list = WriteErrorSqlToFile.getSqlFromFile("feature");
		if(list!=null && list.size()>0){
			for(String str : list){
				executeFeatureSql(str);
			}
		}
		
		LOG.info("flush local similar data to database ......" );
		list = WriteErrorSqlToFile.getSqlFromFile("similar");
		if(list!=null && list.size()>0){
			for(String str : list){
				executeSimilarSql(str);
			}
		}
		
		if(searchThread!=null){
			LOG.info("compare local feature ......" );
			List<ResultRecord> list_ = WriteErrorSqlToFile.readFeatureToFile(idLength, featureSize);
			if(list_!=null && list_.size()>0){
				for(ResultRecord record : list_){
					searchThread.addValue(record);
				}
			}
			
			List<ValueRecord> bdklist_ = WriteErrorSqlToFile.readFeatureToFileForBdk(idLength, featureSize);
			if(bdklist_!=null && bdklist_.size()>0){
				for(ValueRecord record : bdklist_){
					searchThread.addBdkValue(record);
				}
			}
		}
	}
	
	/**
	 * 把指定的INSERT SQL写入到数据库中
	 * @param sql
	 */
	private void executeFeatureSql(String sql) {
		boolean isCommit = false;
		int max_retry = 6;
		int sleep_time = 1000;
		if (debug) {
			LOG.info("insert into fakecar sql is => " + sql);
		}
		
		int retry = 0;
		for(; retry < max_retry; ) {
			//如果是数据库连接失败,进行重试
			try {
				if (feature_statement == null || feature_connection == null || feature_statement.isClosed() || feature_connection.isClosed()) {
					getFeatureConnection();
				}
				feature_statement.execute(sql);
				retry = max_retry;
				isCommit = true;
			}catch(SQLException e1) {
				retry ++;
				String sqlState = e1.getSQLState();
				if ("08S01".equals(sqlState) || "40001".equals(sqlState)){
					LOG.info("reconnection to db zzzzzzzzzzz");
					closeFeatureConnection();
					getFeatureConnection();
				}else {
					switch (e1.getErrorCode()) {
					case 1158:
					case 1159:
					case 1160:
					case 1161:
					case 1203:
					case 1226:
						try {
							Thread.sleep(sleep_time);
						} catch (InterruptedException e) {
							LOG.info(e.getMessage());
						}
						closeFeatureConnection();
						getFeatureConnection();
						break;
					default:
						retry = max_retry;
						break;
					}
				}
				LOG.warn(sql);
				LOG.warn(e1.getMessage(),e1);
			}catch (NullPointerException en) {
				retry ++;
				LOG.warn(sql);
				LOG.warn(en.getMessage(),en);
				try {
					Thread.sleep(sleep_time);
				} catch (InterruptedException e) {
					LOG.info(e.getMessage());
				}
				closeFeatureConnection();
				getFeatureConnection();
			}
		}
		
		if(!isCommit){
			LOG.warn("insert into feature vechicle error sql is => " + sql);
			WriteErrorSqlToFile.writeSqlToFile(sql, "feature");
		}
	}
	
	/**
	 * 把指定的INSERT SQL写入到数据库中
	 * @param sql
	 */
	private void executeSimilarSql(String sql) {
		boolean isCommit = false;
		int max_retry = 6;
		int sleep_time = 1000;
		if (debug) {
			LOG.info("insert into similar sql is => " + sql);
		}
		
		int retry = 0;
		for(; retry < max_retry; ) {
			//如果是数据库连接失败,进行重试
			try {
				if (similar_statement == null || similar_connection == null || similar_statement.isClosed() || similar_connection.isClosed()) {
					getSimilarConnection();
				}
				similar_statement.execute(sql);
				retry = max_retry;
				isCommit = true;
			}catch(SQLException e1) {
				retry ++;
				String sqlState = e1.getSQLState();
				if ("08S01".equals(sqlState) || "40001".equals(sqlState)){
					LOG.info("reconnection to db zzzzzzzzzzz");
					closeSimilarConnection();
					getSimilarConnection();
				}else {
					switch (e1.getErrorCode()) {
					case 1158:
					case 1159:
					case 1160:
					case 1161:
					case 1203:
					case 1226:
						try {
							Thread.sleep(sleep_time);
						} catch (InterruptedException e) {
							LOG.info(e.getMessage());
						}
						closeSimilarConnection();
						getSimilarConnection();
						break;
					default:
						retry = max_retry;
						break;
					}
				}
				LOG.warn(sql);
				LOG.warn(e1.getMessage(),e1);
			}catch (NullPointerException en) {
				retry ++;
				LOG.warn(sql);
				LOG.warn(en.getMessage(),en);
				try {
					Thread.sleep(sleep_time);
				} catch (InterruptedException e) {
					LOG.info(e.getMessage());
				}
				closeSimilarConnection();
				getSimilarConnection();
			}
		}
		
		if(!isCommit){
			LOG.warn("insert into similar vechicle error sql is => " + sql);
			WriteErrorSqlToFile.writeSqlToFile(sql, "similar");
		}
	}
	
	private void getFeatureConnection() {
		try {
			feature_connection = getConnection(featureTableDef.getDriverClass(), 
					featureTableDef.getUrl(), featureTableDef.getUser(),
					featureTableDef.getPassword());
			
			feature_statement = feature_connection.createStatement();
			
			LOG.info("get feature_connection is " + feature_connection.toString());
			
		} catch (Exception e) {
			LOG.warn(e.getMessage(),e);
		}
	}
	
	private void getSimilarConnection() {
		try {
			similar_connection = getConnection(similarTableDef.getDriverClass(), 
					similarTableDef.getUrl(), similarTableDef.getUser(),
					similarTableDef.getPassword());
			
			similar_statement = similar_connection.createStatement();
			
			LOG.info("get similar_connection is " + similar_connection.toString());
			
		} catch (Exception e) {
			LOG.warn(e.getMessage(),e);
		}
	}
	
	private void closeFeatureConnection() {
		try {
			if (null != feature_statement) {
				feature_statement.close();
				feature_statement = null;
			}
			if (null != feature_connection) {
				feature_connection.close();
				feature_connection = null;
			}
		}catch(SQLException e) {
			LOG.warn(e.getMessage(),e);
		}
	}
	
	private void closeSimilarConnection() {
		try {
			if (null != similar_statement) {
				similar_statement.close();
				similar_statement = null;
			}
			if (null != similar_connection) {
				similar_connection.close();
				similar_connection = null;
			}
		}catch(SQLException e) {
			LOG.warn(e.getMessage(),e);
		}
	}
	
	/**
	 * 通过指定的配置信息得到一个SQL连接实例
	 * @param driverClass
	 * @param url
	 * @param user
	 * @param password
	 * @return
	 * @throws Exception
	 */
	public static Connection getConnection(String driverClass,
			String url,String user,String password) throws Exception {
		try {
			Class.forName(driverClass);
		} catch (ClassNotFoundException e) {
			throw new Exception(e.getMessage(),e);
		}
		
		if (null == user || "NULL".equalsIgnoreCase(user)) {
			Connection connection = DriverManager.getConnection(url);
	        return connection;
	    }
		
		if (null == password || "NULL".equalsIgnoreCase(password)) {
			Connection connection = DriverManager.getConnection(url, user, null);
		    return connection;
		}
		//DriverManager.setLoginTimeout(5);
		Connection connection = DriverManager.getConnection(url, user, password);
		return connection;
	}

}
