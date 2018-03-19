package netposa.righttravel.analysis.lib.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

public class OpaqRecordReader extends RecordReader<OpaqInputKey, OpaqInputValue> {
	private static final Log LOG = LogFactory.getLog(OpaqRecordReader.class);
	private OpaqInputKey key;
	private OpaqInputValue value;

	private OpaqInputSplit split;

	private ResultSet results;
	private PreparedStatement statement;
	private Connection connection;
	private Configuration conf;
	
	private String[] excludeCarnumStartWith;
	private String[] excludeCarnumEndWith;
	private String[] excludeCarnumContain;
	private String[] excludeCarnumEqual;
	
	//private String import_time;
	//private long importTimeLong;
	private String search_date;
	
	private long begin = 0;
	private long end = 0;
	private long execCount = 0;
	
	private long length = 100;
	private long pos = 0;
	
	private boolean flag = false;//用来检查result值设置是否成功

	public OpaqRecordReader(OpaqInputSplit split, Connection connection, Configuration conf) {
		this.split = split;
		this.connection = connection;
		this.conf = conf;
		String startwith = this.conf.get("exclude.carnum.startwith");
		if (null != startwith) {
			excludeCarnumStartWith = startwith.split(",");
		}
		String endwith = this.conf.get("exclude.carnum.endwith");
		if (null != endwith) {
			excludeCarnumEndWith = endwith.split(",");
		}
		String contain = this.conf.get("exclude.carnum.contain");
		if (null != contain) {
			excludeCarnumContain = contain.split(",");
		}
		String equals = this.conf.get("exclude.carnum.equals");
		if (null != equals) {
			excludeCarnumEqual = equals.split(",");
		}
		/*
		import_time = this.conf.get("analysis.jdbc.input.query.maximporttime");
		try {
			this.importTimeLong = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(import_time).getTime();
		} catch (ParseException e) {
			LOG.warn(e.getMessage(),e);
		}
		*/
		search_date = this.conf.get("analysis.jdbc.input.query.searchdate");
		
		//LOG.info(Thread.currentThread().getName() + "-> init opaq record reader successed!");
		
		begin = System.currentTimeMillis();
	}
	

	@Override
	public void close() throws IOException {
		end = System.currentTimeMillis();
		try {
			if (null != results) {
				results.close();
			}
			if (null != statement) {
				statement.close();
			}
			if (null != connection) {
				connection.close();
			}
		} catch (SQLException e) {
			LOG.error(e.getMessage(), e);
			throw new IOException(e.getMessage(), e);
		} finally {
			LOG.info(Thread.currentThread().getName() + "-> record-reader execute time is ["
					+ (end - begin)
					+ " ms],select count is " 
					+ execCount + ", execute sql is " + getSelectQuery());
		}
	}

	public String getSelectQuery() {
		return split.getSql();
	}

	public ResultSet executeQuery(String query) throws SQLException {
		statement = connection.prepareStatement(query);
		statement.setFetchSize(1000);
		return statement.executeQuery();
	}

	@Override
	public OpaqInputKey getCurrentKey() throws IOException, InterruptedException {
		return key;
	}

	@Override
	public OpaqInputValue getCurrentValue() throws IOException,
			InterruptedException {
		return value;
	}

	@Override
	public float getProgress() throws IOException, InterruptedException {
		return pos / (float)length;
	}

	@Override
	public void initialize(InputSplit arg0, TaskAttemptContext arg1)
			throws IOException, InterruptedException {
	}

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		try {
			flag = false;
			/*if (execCount == 0) {
				LOG.info(Thread.currentThread().getName() + "-> first find by opaq data set............................");
			}*/
			if (key == null) {
				key = new OpaqInputKey();
			}
			if (value == null) {
				value = new OpaqInputValue();
			}
			if (null == this.results) {
				// First time into this method, run the query.
				this.results = executeQuery(getSelectQuery());
				LOG.info(Thread.currentThread().getName() 
						+ "-> execute sql query sucessed, and sql is "
						+ getSelectQuery());
			}
			
			OUT : while(!flag) {
				if (!results.next()) {
					length = pos;
					return false;
				}

				// Set the key field value as the output key value
				String plateNum = results.getString(1);
				//是否是带卡口字段的SQL查询语句,如果执行if内的操作
				plateNum = StringUtils.trimToNull(plateNum);
				if (null == plateNum) {
					flag = false;
					continue;
				}
				
				//检查车牌是否是排除列表中的车牌
				if (null != excludeCarnumStartWith && excludeCarnumStartWith.length > 0) {
					for(String str : excludeCarnumStartWith) {
						if (plateNum.startsWith(str)) {
							flag = false; 
							continue OUT;
						}
					}
				}
				
				if (null != excludeCarnumEndWith && excludeCarnumEndWith.length > 0) {
					for(String str : excludeCarnumEndWith) {
						if (plateNum.endsWith(str)) {
							flag = false;
							continue OUT;
						}
					}
				}
				
				if (null != excludeCarnumContain && excludeCarnumContain.length > 0) {
					for(String str : excludeCarnumContain) {
						if (plateNum.contains(str)) {
							flag = false;
							continue OUT;
						}
					}
				}
				
				if (null != excludeCarnumEqual && excludeCarnumEqual.length > 0) {
					for(String str : excludeCarnumEqual) {
						if (plateNum.equalsIgnoreCase(str)) {
							flag = false;
							continue OUT;
						}
					}
				}
				
				flag = key.readFields(results, plateNum);
				if (!flag) continue;
				flag = value.readFields(results,search_date);
			}
			execCount++;
			pos ++;
			length = pos + 100;
		} catch (SQLException e) {
			LOG.error(e.getMessage(),e);
			throw new IOException("SQLException in nextKeyValue", e);
		}
		return true;
	}
	
	

}
