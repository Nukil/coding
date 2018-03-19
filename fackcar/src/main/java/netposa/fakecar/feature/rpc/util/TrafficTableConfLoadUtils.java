package netposa.fakecar.feature.rpc.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 加载classpath中的traffic_table_conf.xml配置文件,并生成FeatureTableDef实例
 * @author hongs.yang
 *
 */
public class TrafficTableConfLoadUtils {
	private static final Logger LOG = LoggerFactory.getLogger(TrafficTableConfLoadUtils.class);
	
	private static FeatureTableDef featureTableDef = null;
	
	public static TrafficTableDef loadConfigByClassPath(FeatureTableDef ftd) {
		featureTableDef = ftd;
		TrafficTableDef table = new TrafficTableDef();
		
		loadResultConfTableAttr(table);
		loadResultConfFieldsAttr(table);
		loadResultConfConnectAttr(table);
		
		return table;
	}
	
	
	/**
	 * 加载配置文件中fields配置中的信息
	 * @param fields
	 * @param areaConf
	 */
	private static void loadResultConfFieldsAttr(TrafficTableDef obj) {
		obj.setJlbh("jlbh");
		obj.setXzqh("xzqh");
		obj.setKkbh("kkbh");
	}
	
	/**
	 * 加载配置文件中table属性的内容
	 * @param table
	 * @param areaConf
	 */
	private static void loadResultConfTableAttr(TrafficTableDef obj) {
		obj.setTableName("sjkk_gcjl");
	}
	
	/**
	 * 加载配置文件中connection部分的配置信息
	 * @param connection
	 * @param areaConf
	 */
	private static void loadResultConfConnectAttr(TrafficTableDef obj) {
		
		String opaqIP = null;
		String opaqPORT = null;
		String opaqUserName = null;
		String opaqPassWord = null;
		
		Connection conn = null;
		Statement stat = null;
		try {
			conn = getConnection(featureTableDef.getDriverClass(), featureTableDef.getUrl(), featureTableDef.getUser(), featureTableDef.getPassword());
			stat = conn.createStatement();
			
			ResultSet rs = stat.executeQuery("select * from system_config where TYPE_='OPAQ_CONFIG'");
			ResultSetMetaData md = rs.getMetaData(); //得到结果集(rs)的结构信息，比如字段数、字段名等   
	        int columnCount = md.getColumnCount();
	        while(rs.next()) {
	        	String kV = null;
	        	String vV = null;
	        	for (int i = 1; i <= columnCount; i++) {
		        	String tmpStr = rs.getString(i);
		        	if(StringUtils.isNotBlank(tmpStr)) {
		        		if(md.getColumnName(i).equalsIgnoreCase("KEY_")) {
		        			kV = tmpStr;
			        	} else if(md.getColumnName(i).equalsIgnoreCase("VALUE_")) {
			        		vV = tmpStr;
			        	}
		        	}
		        }
	        	
	        	if("OPAQ_IP".equals(kV)) {
	        		opaqIP = vV;
	        	} else if("OPAQ_PORT".equals(kV)) {
	        		opaqPORT = vV;
	        	} else if("OPAQ_USERNAME".equals(kV)) {
	        		opaqUserName = vV;
	        	} else if("OPAQ_PASSWORD".equals(kV)) {
	        		opaqPassWord = vV;
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
		
		obj.setDriverClass("com.mysql.jdbc.Driver");
		obj.setUser(opaqUserName);
		obj.setUrl("jdbc:mysql://"+opaqIP+":"+opaqPORT+"/netposa01?useUnicode=true&characterEncoding=utf8");
		obj.setPassword(opaqPassWord);
		
	}

	
	private static Connection getConnection(String driverClass,
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
