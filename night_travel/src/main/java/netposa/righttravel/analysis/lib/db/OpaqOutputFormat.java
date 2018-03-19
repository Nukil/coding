package netposa.righttravel.analysis.lib.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.OutputCommitter;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputCommitter;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class OpaqOutputFormat extends OutputFormat<OpaqInputKey, RightTravelOutputValue> {
	
	private static final Log LOG = LogFactory.getLog(OpaqOutputFormat.class);

	@Override
	public void checkOutputSpecs(JobContext context) throws IOException,
			InterruptedException {
	}

	@Override
	public OutputCommitter getOutputCommitter(TaskAttemptContext context)
			throws IOException, InterruptedException {
		return new FileOutputCommitter(FileOutputFormat.getOutputPath(context),
				context);
	}

	@Override
	public RecordWriter<OpaqInputKey, RightTravelOutputValue> getRecordWriter(
			TaskAttemptContext context) throws IOException, InterruptedException {
		String tableName = context.getConfiguration().get("analysis.opaq.output.table.name");
		String[] fieldNames = context.getConfiguration().getStrings("analysis.opaq.output.field.names");
		if (null == fieldNames) {
			fieldNames = new String[context.getConfiguration().getInt("analysis.opaq.output.field.count", 0)];
		}
		
		try {
			
			String sql = buildInsertSql(tableName, fieldNames);
			
			Connection connection = getConnection(context.getConfiguration());
			while (null == connection) {
				try {
					Thread.sleep(1000 * 60);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				connection = getConnection(context.getConfiguration());
			}
			
			PreparedStatement statement = connection.prepareStatement(sql);
			return new OpaqRecordWriter(connection, statement,
					context.getConfiguration(),sql);
		}catch(Exception e) {
			throw new IOException(e.getMessage(), e);
		}
		
	}
	
	/**
	 * 通过指定的table与字段名称生成写入sql
	 * @param table
	 * @param fieldNames
	 * @return
	 */
	public String buildInsertSql(String table,String[] fieldNames) {
		if (null == table) {
			throw new IllegalArgumentException("table may not by null");
		}
		if (null == fieldNames || fieldNames.length < 1) {
			throw new IllegalArgumentException("Field names may not by null");
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT ignore INTO ").append(table);
		if (fieldNames[0] != null) {
			sql.append(" (");
			for(int i=0; i<fieldNames.length; i++) {
				sql.append(fieldNames[i]);
				if (i != fieldNames.length-1) {
					sql.append(",");
				}
			}
			sql.append(")");
		}
		
		sql.append(" VALUES (");
		for(int i=0; i<fieldNames.length; i++) {
			sql.append("?");
			if (i != fieldNames.length-1) {
				sql.append(",");
			}
		}
		sql.append(")");
		
		return sql.toString();
	}
	
	@SuppressWarnings("static-access")
	public static Connection getConnection(Configuration conf) {
		Connection connection = null;
		try {
		  Class.forName(conf.get("analysis.jdbc.driver.class"));
	      if (null == connection) {
	        // The connection was closed; reinstantiate it.
	    	if(conf.get("analysis.jdbc.username") == null) {
	    		connection = DriverManager.getConnection(
   	               conf.get("analysis.jdbc.url"));
	   	    } else {
	   	        connection = DriverManager.getConnection(
	   	           conf.get("analysis.jdbc.url"), 
	   	           conf.get("analysis.jdbc.username"), 
	   	           conf.get("analysis.jdbc.password"));
	   	    }
	      }
	    } catch (Exception e) {
	    	connection = null;
	    	LOG.error("analysis.jdbc.url=%s , analysis.jdbc.username=%s , analysis.jdbc.password=%s".format(conf.get("analysis.jdbc.url"), conf.get("analysis.jdbc.username"),conf.get("analysis.jdbc.password")));
	    	LOG.error("Get Output Connection fail , message is : " + e.getMessage(), e);
	    }
	    return connection;
	}
	
	public static void setOutput(Job job, String tableName, String[] fieldNames) throws IOException{
		if (fieldNames.length > 0 && fieldNames[0] != null) {
			job.setOutputFormatClass(OpaqOutputFormat.class);
			job.setReduceSpeculativeExecution(false);
			job.getConfiguration().set("analysis.opaq.output.table.name", tableName);
			job.getConfiguration().setStrings("analysis.opaq.output.field.names", fieldNames);
		}else {
			if (fieldNames.length > 0) {
				job.setOutputFormatClass(OpaqOutputFormat.class);
				job.setReduceSpeculativeExecution(false);
				job.getConfiguration().set("analysis.opaq.output.table.name", tableName);
				job.getConfiguration().setInt("analysis.opaq.output.field.count", fieldNames.length);
			}else {
				throw new IllegalArgumentException("Field names must be greater than 0");
			}
		}
	}

}
