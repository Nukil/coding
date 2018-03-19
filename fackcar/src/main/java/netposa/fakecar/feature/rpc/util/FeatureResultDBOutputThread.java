package netposa.fakecar.feature.rpc.util;

import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import netposa.fakecar.feature.rpc.FeatureCalculatorServiceImpl;
import netposa.fakecar.feature.rpc.ResultRecord;
import netposa.fakecar.feature.rpc.error.WriteErrorSqlToFile;

/**
 * 向数据库写入套牌分析结果数据
 * @author hongs.yang
 *
 */
public class FeatureResultDBOutputThread extends Thread {
	private static final Logger LOG = LoggerFactory.getLogger(FeatureResultDBOutputThread.class);
	
	private static final Charset charset = Charset.forName("UTF-8");
	
	private final FeatureTableDef tableDef;
	private final TrafficTableDef trafficTableDef;
	private final int flush_interval;
	private final int flush_number;
	private final int cleanDay;
	
	private boolean running = false;
	
	private LinkedList<ResultRecord> buffers = null;
	private long last_time = 0;
	private int buffer_size = 0;
	private String insertSql;
	private String insertBaseValue;
	private Connection connection;
	private Statement statement;
	private boolean debug = false;
	
	private long lastCleantime = 0;
	
	private FeatureCalculatorServiceImpl service;
	
	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public FeatureResultDBOutputThread(FeatureCalculatorServiceImpl service,
			FeatureTableDef tableDef, TrafficTableDef trafficTableDef,
			int flush_interval,int flush_number, int cleanDay, boolean debug) {
		setName("FeatureResultDBOutputThread:" + getName());
		this.service = service;
		this.tableDef = tableDef;
		this.trafficTableDef = trafficTableDef;
		this.flush_interval = flush_interval;
		this.flush_number = flush_number;
		this.cleanDay = cleanDay;
		
		buffers = new LinkedList<ResultRecord>();
		running = true;
		buffer_size = 0;
		last_time = 0;
		this.debug = debug;
		
		buildSqlKey();
		
		getConnection();
		this.setUncaughtExceptionHandler(new ThreadExceptionHandler());
	}
	
	
	public void addValue(ResultRecord value) {
		synchronized (buffers) {
			buffers.add(value);
			buffer_size += 1;
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
				if (buffer_size > 0 &&
						(last_time + flush_interval) < System.currentTimeMillis()) {
					int size = execute();
					
					LOG.info("flush fakecar to database by timeout, size is " + size);
				}else if (buffer_size >= flush_number) {
					int size = execute();
					
					LOG.info("flush fakecar to database by number too large, size is " + size);
				}
				
				try {
					sleep(500);
				} catch (InterruptedException e) {
					LOG.warn("flush error:" + e.getMessage(),e);
				}
				
				if (cleanDay > 0) {
					if (lastCleantime == 0) {
						//初始进入,设置一个执行时间为下一天的晚上２点
						Calendar calendar = Calendar.getInstance();
					    calendar.setTimeInMillis(System.currentTimeMillis());
					    calendar.set(Calendar.HOUR_OF_DAY, 2);
					    calendar.set(Calendar.MINUTE, 1);
					    calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
					    lastCleantime = calendar.getTimeInMillis();
					}else if (System.currentTimeMillis() > lastCleantime) {
						//先设置下一个执行时间,下一天的晚上２点
						Calendar calendar = Calendar.getInstance();
					    calendar.setTimeInMillis(lastCleantime);
					    calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
					    lastCleantime = calendar.getTimeInMillis();
					    //执行清理操作
					    Thread thread = new CleanOldKeyThread(service,cleanDay);
					    thread.setDaemon(true);
					    thread.start();
					}
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
	private int execute() {
		LinkedList<ResultRecord> flushList = null;
		synchronized (buffers) {
			flushList = buffers;
			buffers = new LinkedList<ResultRecord>();
			buffer_size = 0;
		}
		String sql = buildSql(flushList);
		
		executeSql(sql);
		last_time = System.currentTimeMillis();
		return flushList.size();
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
			LOG.info("insert into fakecar sql is => " + sql);
		}
		
		int retry = 0;
		for(; retry < max_retry; ) {
			//如果是数据库连接失败,进行重试
			try {
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
			LOG.warn("insert into fakecar vechicle error sql is => " + sql);
			WriteErrorSqlToFile.writeSqlToFile(sql, "feature");
		}
	}
	
	/**
	 * 把要写入的数据生成成sql的value值
	 * @param flushList
	 * @return
	 */
	private String buildSql(LinkedList<ResultRecord> flushList) {
		StringBuffer sql = new StringBuffer();
		sql.append(insertSql);
		int index = 0;
		int size = flushList.size();
		String now_time = df.format(new Date(System.currentTimeMillis()));
		for(ResultRecord record : flushList) {
			byte[] fkkbh = record.getFirstValue().getKkbh();
			byte[] fxzqh = record.getFirstValue().getXzqh();
			byte[] skkbh = record.getSecondValue().getKkbh();
			byte[] sxzqh = record.getSecondValue().getXzqh();
			
			try {
				byte[][] tmpResult = null;
				if(fkkbh == null || fxzqh == null) {
					tmpResult = executeTrafficSQL(record.getFirstValue().getRecord_id());
					if(tmpResult != null) {
						fxzqh = tmpResult[0];
						fkkbh = tmpResult[1];
					}
				}
				if(skkbh == null || sxzqh == null) {
					tmpResult = executeTrafficSQL(record.getSecondValue().getRecord_id());
					if(tmpResult != null) {
						sxzqh = tmpResult[0];
						skkbh = tmpResult[1];
					}
				}
			} catch(Exception e) {
				LOG.error("connect OPAQ error!!", e);
			}
			
			String valueSql = insertBaseValue.replace("@LicencePlate@",
					"'"+new String(record.getKey().getLicence_plate(),charset)+"'")
				.replace("@FirstLicencePlateColor@",caseValue(record.getFirstValue().getLicence_plate_color()))
				.replace("@SecondLicencePlateColor@",caseValue(record.getSecondValue().getLicence_plate_color()))
				.replace("@FirstTrafficId@", caseValue(record.getFirstValue().getRecord_id()))
				.replace("@SecondTrafficId@", caseValue(record.getSecondValue().getRecord_id()))
				.replace("@FirstVehicleLogo@", caseValue(record.getFirstValue().getVehicle_logo()))
				.replace("@SecondVehicleLogo@", caseValue(record.getSecondValue().getVehicle_logo()))
				.replace("@FirstVehicleChildLogo@", caseValue(record.getFirstValue().getVehicle_child_logo()))
				.replace("@SecondVehicleChildLogo@", caseValue(record.getSecondValue().getVehicle_child_logo()))
				.replace("@FirstVehicleStyle@", caseValue(record.getFirstValue().getVehicle_style()))
				.replace("@SecondVehicleStyle@", caseValue(record.getSecondValue().getVehicle_style()))
				.replace("@FirstVehicleType@", caseValue(record.getFirstValue().getVehicle_type()))
				.replace("@SecondVehicleType@", caseValue(record.getSecondValue().getVehicle_type()))
				.replace("@FirstVehicleColor@", caseValue(record.getFirstValue().getVehicle_color()))
				.replace("@SecondVehicleColor@", caseValue(record.getSecondValue().getVehicle_color()))
				.replace("@StoreTime@", now_time)
				.replace("@FirstMonitorId@", caseValue(fkkbh))
				.replace("@FirstOrgId@", caseValue(fxzqh))
				.replace("@SecondMonitorId@", caseValue(skkbh))
				.replace("@SecondOrgId@", caseValue(sxzqh));
			
			sql.append(valueSql);
			if (index != size-1) {
				sql.append(",");
			}
			index ++;
		}
		return sql.toString();
	}
	
	/**
	 * 从opaq获取通行记录的组织机构id和卡口id
	 * 
	 * index=0  组织机构id
	 * index=1 卡口id
	 * @param trafficId
	 * @return
	 */
	private byte[][] executeTrafficSQL(byte[] trafficId) {
		byte[][] result = new byte[2][];
		Connection conn = null;
		Statement stat = null;
		try {
			conn = getConnection(trafficTableDef.getDriverClass(), 
					trafficTableDef.getUrl(), trafficTableDef.getUser(),
					trafficTableDef.getPassword());
			
			stat = conn.createStatement();
			
			ResultSet rs = stat.executeQuery("select "+trafficTableDef.getJlbh()+","+trafficTableDef.getXzqh()+","+trafficTableDef.getKkbh()+" from " + trafficTableDef.getTableName() + " where jlbh='" + new String(trafficId) + "'");
			ResultSetMetaData md = rs.getMetaData(); //得到结果集(rs)的结构信息，比如字段数、字段名等   
	        int columnCount = md.getColumnCount();
	        if(rs.next()) {
	        	for (int i = 1; i <= columnCount; i++) {
		        	String tmpStr = rs.getString(i);
		        	if(StringUtils.isNotBlank(tmpStr)) {
		        		if(md.getColumnName(i).equalsIgnoreCase(trafficTableDef.getXzqh())) {
			        		result[0] = tmpStr.getBytes();
			        	} else if(md.getColumnName(i).equalsIgnoreCase(trafficTableDef.getKkbh())) {
			        		result[1] = tmpStr.getBytes();
			        	}
		        	}
		        }
	        }
		} catch (Exception e) {
			LOG.warn(e.getMessage(),e);
		} finally {
			try {
				if (null != stat) {
					stat.close();
					stat = null;
				}
				if (null != conn) {
					conn.close();
					conn = null;
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		return result;
	}
	
	/**
	 * 生成插入语句
	 */
	private void buildSqlKey() {
		insertBaseValue = "(@LicencePlate@,@FirstLicencePlateColor@,@SecondLicencePlateColor@,"
				+ "@FirstTrafficId@,@SecondTrafficId@,"
				+ "@FirstVehicleLogo@,@SecondVehicleLogo@,"
				+ "@FirstVehicleChildLogo@,@SecondVehicleChildLogo@,"
				+ "@FirstVehicleStyle@,@SecondVehicleStyle@,"
				+ "@FirstVehicleType@,@SecondVehicleType@,"
				+ "@FirstVehicleColor@,@SecondVehicleColor@,'0','@StoreTime@', @FirstMonitorId@,"
				+ "@FirstOrgId@, @SecondMonitorId@, @SecondOrgId@)";
		
		StringBuffer sb = new StringBuffer();
		sb.append("INSERT INTO ").append(tableDef.getTableName())
			.append(" (")
			.append(tableDef.getLicencePlate() + ",")
			.append(tableDef.getFirstLicencePlateColor() + ",")
			.append(tableDef.getSecondLicencePlateColor() + ",")
			.append(tableDef.getFirstTrafficId() + ",")
			.append(tableDef.getSecondTrafficId() + ",")
			.append(tableDef.getFirstVehicleLogo() + ",")
			.append(tableDef.getSecondVehicleLogo() + ",")
			.append(tableDef.getFirstVehicleChildLogo() + ",")
			.append(tableDef.getSecondVehicleChildLogo() + ",")
			.append(tableDef.getFirstVehicleStyle() + ",")
			.append(tableDef.getSecondVehicleStyle() + ",")
			.append(tableDef.getFirstVehicleType() + ",")
			.append(tableDef.getSecondVehicleType() + ",")
			.append(tableDef.getFirstVehicleColor() + ",")
			.append(tableDef.getSecondVehicleColor() + ",")
			.append(tableDef.getType() + ",")
			.append(tableDef.getStoreTime() + ",")
			.append(tableDef.getFirstMonitorId() + ",")
			.append(tableDef.getFirstOrgId() + ",")
			.append(tableDef.getSecondMonitorId() + ",")
			.append(tableDef.getSecondOrgId())
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
