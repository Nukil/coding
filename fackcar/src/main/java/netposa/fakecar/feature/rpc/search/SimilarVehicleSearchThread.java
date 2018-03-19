package netposa.fakecar.feature.rpc.search;

import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;

import netposa.fakecar.feature.rpc.ResultRecord;
import netposa.fakecar.feature.rpc.ValueRecord;
import netposa.fakecar.feature.rpc.error.WriteErrorSqlToFile;
import netposa.fakecar.feature.rpc.search.SimilarVehicleSearchRpcService.Client;
import netposa.fakecar.feature.rpc.util.ThreadExceptionHandler;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimilarVehicleSearchThread extends Thread {
	
	private static final Logger LOG = LoggerFactory.getLogger(SimilarVehicleSearchThread.class);
	
	private static final Charset charset = Charset.forName("UTF-8");
	
	private DateFormat df = new SimpleDateFormat("yyyyMMdd");
	
	private static final int timeout = 20000;
	private static final int flush_interval = 1000*10;
	private static final int flush_number = 10;
	
	private int result_count = 50;
	private int result_distence = 55;
	private int searchDate = 30;
	private boolean result_id_only = true;
	
	private String host;
	private int port;
	private boolean debug = false;
	
	//相似车辆表
	private final SimilarVehicleTableDef tableDef;
	//pcc-vim 车辆颜色字典
	protected Map<String,String> vehicleColor;
	//pcc-vim 车辆品牌字典
	protected Map<String,String> vehicleBrand;
	
	private String insertSql;
	private String insertBaseValue;
	
	private boolean running = false;
	
	private Connection connection;
	private Statement statement;
	
	private long last_time = 0;
	
	private int idLength = 20;
	private int featureSize = 288;
	
	private LinkedList<ResultRecord> buffers = null;
	private LinkedList<ValueRecord> bdkBuffers = null;
	private int buffer_size = 0;
	private int bdk_buffer_size = 0;
	
	/**
	 * 
	 * @param result_count 返回结果数量
	 * @param result_distence 相似度阈值
	 * @param searchDate 查询回溯天数
	 * @param host 特征比对服务IP
	 * @param port 特征比对服务端口
	 * @param debug 调试开关
	 * @param result_id_only 是否只返回ID，否则返回JSON串
	 */
	public SimilarVehicleSearchThread(int result_count, int result_distence, int searchDate, String host, int port, boolean debug, boolean result_id_only, int idLength, int featureSize){
		
		this.tableDef = SimilarVehicleTableConfLoadUtils.loadConfigByClassPath();
		this.vehicleColor = VehicleColorConfLoadUtils.loadConfigByClassPath();
		this.vehicleBrand = VehicleBrandConfLoadUtils.loadConfigByClassPath();
		
		this.host = host;
		this.port = port;
		this.debug = debug;
		this.result_count = result_count;
		this.result_distence = result_distence;
		this.searchDate = searchDate;
		
		this.result_id_only = result_id_only;
		this.idLength = idLength;
		this.featureSize = featureSize;
		
		running = true;
		buffers = new LinkedList<ResultRecord>();
		bdkBuffers = new LinkedList<ValueRecord>();
		
		buildSqlKey();
		
		getConnection();
		this.setUncaughtExceptionHandler(new ThreadExceptionHandler());
	}
	
	//添加套牌信息
	public void addValue(ResultRecord value) {
		synchronized (buffers) {
			buffers.add(value);
			buffer_size ++;
		}
	}
	
	public void addBdkValue(ValueRecord value) {
		synchronized (bdkBuffers) {
			bdkBuffers.add(value);
			bdk_buffer_size ++;
		}
	}
	
	public void close() {
		running = false;
		closeConnection();
	}
	
	@Override
	public void run() {
		while(running) {
			try {
				
				if ((buffer_size > 0 || bdk_buffer_size > 0) && (last_time + flush_interval) < System.currentTimeMillis()) {
					int size = execute();
					LOG.info("flush similar vechicle to database by timeout, size is " + size);
				}else if (buffer_size >= flush_number || bdk_buffer_size >= flush_number) {
					int size = execute();
					LOG.info("flush similar vechicle to database by number too large, size is " + size);
				}
				
				sleep(1000);
				
			}catch(Exception ex) {
				LOG.warn("THIS WHILE ERROR => " + ex.getMessage(),ex);
			}
		}
		
	}
	
	/**
	 * 执行数据库写入操作
	 * @return
	 */
	private int execute() {
		
		LinkedList<ResultRecord> flushList = null;
		synchronized (buffers) {
			flushList = buffers;
			buffers = new LinkedList<ResultRecord>();
			buffer_size = 0;
		}
		
		LinkedList<ValueRecord> bdkFlushList = null;
		synchronized (bdkBuffers) {
			bdkFlushList = bdkBuffers;
			bdkBuffers = new LinkedList<ValueRecord>();
			bdk_buffer_size = 0;
		}
		
		StringBuffer sqlbBuffer = null;
		int size = 0;
		if(flushList != null) size = flushList.size();
		
		int bdk_size = 0;
		if(bdkFlushList != null) bdk_size = bdkFlushList.size();
		
		TTransport transport = new TSocket(host, port, timeout);
		transport = new TFramedTransport(transport);
		
		int max_retry = 6;
		int sleep_time = 1000;
		boolean open_success = false;
		
		int retry = 0;
		for(; retry < max_retry; ) {
			try {
				transport.open();
				retry = max_retry;
				open_success = true;
			} catch (TTransportException e) {
				LOG.warn("特征比对服务异常...... " + (retry + 1));
				retry ++;
				LOG.warn(e.getMessage(),e);
				try {
					Thread.sleep(sleep_time);
				} catch (InterruptedException e1) {
					LOG.info(e1.getMessage(),e1);
				}
			}
		}
		
		TProtocol protocol = new TCompactProtocol(transport);
		Client client = new Client(protocol);
		
		if(!open_success){
			LOG.warn("RPC transport open error !");
			for(int i=0; i<size; i++) {
				try {
					WriteErrorSqlToFile.writeFeatureToFile(flushList.get(i), idLength, featureSize);
					LOG.warn("write Feature date to error file success !");
				} catch (Exception e) {
					LOG.warn("write Feature date to error file fail !");
					LOG.info(e.getMessage(),e);
					e.printStackTrace();
				}
			}
			
			for(int i=0; i<bdk_size; i++) {
				try {
					WriteErrorSqlToFile.writeFeatureToFile(bdkFlushList.get(i), idLength, featureSize);
					LOG.warn("write Feature date to error file success !");
				} catch (Exception e) {
					LOG.warn("write Feature date to error file fail !");
					LOG.info(e.getMessage(),e);
					e.printStackTrace();
				}
			}
			LOG.warn("write Feature date to error file end !");
			last_time = System.currentTimeMillis();
			return 0;
		}
		
		for(int i=1; i<=size; i++) {
			ResultRecord record = flushList.get(i-1);
			sqlbBuffer = buildSql(record, sqlbBuffer, client);
			if(sqlbBuffer != null && (i%10==0 || i==size)){
				String sql = sqlbBuffer.toString();
				if(sql.endsWith(",")){
					sql = sql.substring(0,sql.length()-1);
				}
				executeSql(sql);
				sqlbBuffer = null;
			}
		}
		
		for(int i=1; i<=bdk_size; i++) {
			ValueRecord record = bdkFlushList.get(i-1);
			sqlbBuffer = buildBdkSql(record, sqlbBuffer, client);
			if(sqlbBuffer != null && (i%10==0 || i==size)){
				String sql = sqlbBuffer.toString();
				if(sql.endsWith(",")){
					sql = sql.substring(0,sql.length()-1);
				}
				executeSql(sql);
				sqlbBuffer = null;
			}
		}
		
		transport.close();
		
		last_time = System.currentTimeMillis();
		return size;
	}

	/**
	 * 特征搜索
	 * @param value 套牌数据
	 * @param client RPC客户端
	 * @return
	 * @throws TException 
	 */
	private String executeSearch(ValueRecord value,Client client) throws TException{
		
		InputRecord record = new InputRecord();
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.add(Calendar.DAY_OF_MONTH, -searchDate);
		
		record.setStartTime(df.format(calendar.getTime()));
		record.setEndTime(df.format(new Date()));
		
		record.setCount(result_count);
		record.setDistence(result_distence);
		record.setFeature(value.getVehicle_feature_buffer());
		
		String clpp = "-1";
		if(value.getVehicle_logo() !=null && value.getVehicle_logo().length>0 && 
				vehicleBrand.get(new String(value.getVehicle_logo(),charset))!=null){
			clpp = vehicleBrand.get(new String(value.getVehicle_logo(),charset));
		}
		
		String csys = "-1";
		if(value.getVehicle_color() !=null && value.getVehicle_color().length>0 && 
				vehicleColor.get(new String(value.getVehicle_color(),charset))!=null){
			csys = vehicleColor.get(new String(value.getVehicle_color(),charset));
		}
		
		record.setClpp(clpp);
		record.setCsys(Integer.valueOf(csys));
		
		return client.getSearchData(record);
	}
	
	/**
	 * 把指定的INSERT SQL写入到数据库中
	 * @param sql
	 */
	private void executeSql(String sql) {
		
		boolean isCommit = false;
		int max_retry = 6;
		int sleep_time = 1000;
		
		if (debug) {
			LOG.info("insert into similar vechicle sql is => " + sql);
		}
		
		int retry = 0;
		for(; retry < max_retry; ) {
			
			try {
				//如果是数据库连接失败,进行重试
				if (statement == null || connection == null || statement.isClosed() || connection.isClosed()) {
					getConnection();
				}
				
				statement.execute(sql);
				retry = max_retry;
				isCommit = true;
			}catch(SQLException e1) {
				retry ++;
				String sqlState = e1.getSQLState();
				if ("08S01".equals(sqlState) || "40001".equals(sqlState)){
					LOG.info("reconnection to db zzzzzzzzzzz");
					closeConnection();
					getConnection();
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
						closeConnection();
						getConnection();
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
				closeConnection();
				getConnection();
			}
		}
		
		if(!isCommit){
			LOG.warn("insert into similar vechicle error sql is => " + sql);
			WriteErrorSqlToFile.writeSqlToFile(sql, "similar");
		}
	}
	
	/**
	 * 解析jlbh
	 * @param recordids
	 * @param result
	 * @return
	 */
	private StringBuffer getJlbh(StringBuffer recordids, String result) {
		
		if(recordids == null){
			recordids = new StringBuffer();
		}
		
		if(result != null && result.trim().length()>10){
			
			if(debug) {
				LOG.info("result : " + result);
			}
			
			result = result.substring(3, result.length()-3);
			String[] results = result.split("\\},\\{");
			for (String res : results) {
				String[] res_ = res.split("\",\"");
				for(String re : res_){
					String[] re_ = re.split("\":\"");
					if(re_.length==2){
						String key = re_[0].replaceAll("\"", "");
						String value = re_[1].replaceAll("\"", "");
						if(key.equals("jlbh") && value != null && !value.equals("null")){
							if(recordids.length()!=0){
								recordids.append(",");
							}
							recordids.append(value);
							break;
						}
					}
				}
				
			}
		}
		
		return recordids;
	}
	
	/**
	 * 把要写入的数据生成成sql的value值
	 * @param flushList
	 * @return
	 */
	private StringBuffer buildSql(ResultRecord record,StringBuffer sql ,Client client) {
		
		String result1 = null;
		String result2 = null;
		try {
			result1 = executeSearch(record.getFirstValue(), client);
			result2 = executeSearch(record.getSecondValue(), client);
		} catch (TException e) {
			LOG.warn("特征比对服务异常......");
			LOG.warn(e.getMessage(),e);
		}
		
		if(result1==null || result2==null){
			try {
				WriteErrorSqlToFile.writeFeatureToFile(record, idLength, featureSize);
				LOG.warn("write Feature date to error file success !");
			} catch (Exception e) {
				LOG.warn("write Feature date to error file fail !");
				LOG.info(e.getMessage(),e);
			}
			return sql;
		}
		
		StringBuffer recordids1 = new StringBuffer();
		StringBuffer recordids2 = new StringBuffer();
		
		if(result_id_only){
			recordids1 = getJlbh(recordids1, result1);
			recordids2 = getJlbh(recordids2, result1);
		}else {
			if(result1 != null && result1.trim().length()>10){
				recordids1.append(result1);
			}
			if(result2 != null && result2.trim().length()>10){
				recordids2.append(result2);
			}
		}
		
		if(debug) {
			LOG.info("recordids1 : " + recordids1.toString());
			LOG.info("recordids2 : " + recordids2.toString());
		}
		
		if((recordids1 !=null && recordids1.length()>0 && !recordids1.equals("null")) || (recordids2 !=null && recordids2.length()>0 && !recordids2.equals("null"))){
			if(sql == null){
				sql = new StringBuffer();
				sql.append(insertSql);
			}
			String valueSql = insertBaseValue.replace("@FirstTrafficId@", caseValue(record.getFirstValue().getRecord_id()))
					.replace("@FirstSimilarTrafficId@", "'" + recordids1.toString() + "'")
					.replace("@SecondTrafficId@", caseValue(record.getSecondValue().getRecord_id()))
					.replace("@SecondSimilarTrafficId@", "'" + recordids2.toString() + "'");
			if(debug) {
				LOG.info(valueSql);
			}
			sql.append(valueSql+",");
		}
		
		return sql;
	}
	
	/**
	 * 把要写入的数据生成成sql的value值
	 * @param flushList
	 * @return
	 */
	private StringBuffer buildBdkSql(ValueRecord record,StringBuffer sql ,Client client) {
		
		String result1 = null;
		try {
			result1 = executeSearch(record, client);
		} catch (TException e) {
			LOG.warn("特征比对服务异常......");
			LOG.warn(e.getMessage(),e);
		}
		
		if(result1==null){
			try {
				WriteErrorSqlToFile.writeFeatureToFile(record, idLength, featureSize);
				LOG.warn("write Feature date to error file success !");
			} catch (Exception e) {
				LOG.warn("write Feature date to error file fail !");
				LOG.info(e.getMessage(),e);
			}
			return sql;
		}
		
		StringBuffer recordids1 = new StringBuffer();
		
		if(result_id_only){
			recordids1 = getJlbh(recordids1, result1);
		}else {
			if(result1 != null && result1.trim().length()>10){
				recordids1.append(result1);
			}
		}
		
		if(debug) {
			LOG.info("recordids1 : " + recordids1.toString());
		}
		
		if((recordids1 !=null && recordids1.length()>0 && !recordids1.equals("null"))){
			if(sql == null){
				sql = new StringBuffer();
				sql.append(insertSql);
			}
			String valueSql = insertBaseValue.replace("@FirstTrafficId@", caseValue(record.getRecord_id()))
					.replace("@FirstSimilarTrafficId@", "'" + recordids1.toString() + "'")
					.replace("@SecondTrafficId@", "''")
					.replace("@SecondSimilarTrafficId@", "''");
			if(debug) {
				LOG.info(valueSql);
			}
			sql.append(valueSql+",");
		}
		
		return sql;
	}
	
	/**
	 * 生成插入语句
	 */
	private void buildSqlKey() {
		insertBaseValue = "(@FirstTrafficId@,@FirstSimilarTrafficId@,@SecondTrafficId@,@SecondSimilarTrafficId@)";
		
		StringBuffer sb = new StringBuffer();
		sb.append("INSERT INTO ").append(tableDef.getTableName())
			.append(" (")
			.append(tableDef.getFirstTrafficId() + ",")
			.append(tableDef.getFirstSimilarTrafficId() + ",")
			.append(tableDef.getSecondTrafficId() + ",")
			.append(tableDef.getSecondSimilarTrafficId())
			.append(") VALUES ");
		
		this.insertSql = sb.toString();
	}
	
	private String caseValue(byte[] value) {
		if (value == null || value.length == 0) {
			return "null";
		}else {
			StringBuffer str = new StringBuffer();
			str.append("'").append(new String(value,charset)).append("'");
			return str.toString();
		}
	}

	private void getConnection() {
		try {
			connection = getConnection(tableDef.getDriverClass(), 
					tableDef.getUrl(), tableDef.getUser(),
					tableDef.getPassword());
			
			statement = connection.createStatement();
			
			LOG.info("get connection is " + connection.toString());
		} catch (Exception e) {
			LOG.warn(e.getMessage(),e);
		}
	}
	
	private void closeConnection() {
		try {
			if (null != statement) {
				statement.close();
				statement = null;
			}
			if (null != connection) {
				connection.close();
				connection = null;
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