package netposa.firstincity.rpc;

import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirstIntoSearchRpcServiceHandler implements
		FisrtIntoSearchRpcService.Iface {
	private static final Logger LOG = LoggerFactory.getLogger(FirstIntoSearchRpcServiceHandler.class);
	
	private Charset charset = Charset.forName("utf-8");
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	// 配置信息
	private Properties props;
	private Integer[] ruleIds;

	public FirstIntoSearchRpcServiceHandler(Properties props) {
		this.props = props;
		String[] tmps = StringUtils.trimToEmpty(
				props.getProperty("filter.rule.ids", "1,3,6")).split(",");
		ruleIds = new Integer[tmps.length];
		int index = 0;
		for (String str : tmps) {
			ruleIds[index] = Integer.valueOf(str);
			index++;
		}
	}

	@Override
	public Map<Integer, Boolean> judgeFirstInto(InputRecord record)
			throws TException{
		String hphm = new String(record.getLicence_plate(), charset);
		String hpys = new String(record.getLicence_plate_color(), charset);
		String passTime = new String(record.getPass_time(), charset);
		long psTime = 0;
		try {
			psTime = sdf.parse(passTime).getTime();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		LOG.info(String.format("firstinto rpc receive data, hphm=%s,hpys=%s,jgsj=%s", hphm, hpys, passTime));
		
		return hasValue(props, hphm, hpys,psTime);
	}
	
	private Map<Integer, Boolean> hasValue(Properties props, String hphm, String hpys, Long passTime){
		Map<Integer, Boolean> resultMap = new HashMap<Integer, Boolean>();
		Map<Integer, String> sqlMap = new HashMap<Integer, String>();
		Date currentDate = new Date();
		for(Integer ruleId : ruleIds) {
			resultMap.put(ruleId, false);
			Calendar cal = Calendar.getInstance();
			cal.setTime(currentDate);
			cal.add(Calendar.MONTH, 0-ruleId);
			String table_name = props.getProperty("db.opaq.tablename");
			String selectSql = "SELECT jgsj,hphm,hpys FROM"+table_name+" WHERE ";
			StringBuffer sqlBuf = new StringBuffer();
			sqlBuf.append(selectSql);
			sqlBuf.append(" hphm = '" + hphm + "'");
			sqlBuf.append(" and hpys = '" + hpys + "'");
			sqlBuf.append(" and jgsj >= '" + sdf.format(cal.getTime()) + "'");
			sqlBuf.append(" and jgsj < '" + passTime + "' limit 1");
//			System.out.println(sqlBuf.toString());
			sqlMap.put(ruleId, sqlBuf.toString());
		}
		Connection connection = null;
		Statement statement = null;
		ResultSet resultset = null;
		
		try {
			Class.forName("com.mysql.jdbc.Driver");
			if(props.getProperty("db.opaq.user") == null) {
				connection = DriverManager.getConnection(props.getProperty("db.opaq.url"));
			} else {
				connection = DriverManager.getConnection(
						props.getProperty("db.opaq.url"), 
						props.getProperty("db.opaq.user"), 
						props.getProperty("db.opaq.password"));
			}
			statement = connection.createStatement();
			
			boolean notNeedSql = false;
			
			for(int i=ruleIds.length;i>0;i--) {
				Integer ruleId = ruleIds[i-1];
				if(notNeedSql) {
					resultMap.put(ruleId, true);
				} else {
					resultset = statement.executeQuery(sqlMap.get(ruleId));
					
					if(resultset.next()) {
						resultMap.put(ruleId, false);
					} else {
						resultMap.put(ruleId, true);
						notNeedSql = true;
					}
					resultset.close();
				}
			}
			statement.close();
			connection.close();
		} catch (Exception e) {
			LOG.error(e.getMessage());
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
		LOG.info(String.format("firstinto rpc hphm=[%s], result={[%s],[%s],[%s]}", hphm, resultMap.get(1), resultMap.get(3), resultMap.get(6)));
		return resultMap;
	}
}
