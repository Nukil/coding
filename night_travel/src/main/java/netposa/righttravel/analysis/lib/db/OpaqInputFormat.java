package netposa.righttravel.analysis.lib.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

public class OpaqInputFormat extends InputFormat<OpaqInputKey, OpaqInputValue> implements Configurable{
	private static final Log LOG = LogFactory.getLog(OpaqInputFormat.class);
	
	private static long HOUR = 60 * 60 * 1000;
    private String splitField;
    private Configuration conf;
    private Connection connection;
    
    private static long[] split(long numSplits, long minVal, long maxVal) {
		List<Long> list = new ArrayList<Long>();
		long splitSize = (maxVal - minVal) / numSplits;
		if (splitSize < 1) splitSize = 1;
		long curVal = minVal;
		while(curVal <= maxVal) {
			list.add(curVal);
			curVal += splitSize;
		}
		int size = list.size();
		if (list.get(size-1) != maxVal || size == 1) {
			list.add(maxVal);
		}
		long[] splits = new long[list.size()];
		int i=0;
		for(Long cur : list) {
			splits[i] = cur.longValue();
			i ++;
		}
		if (splits.length > 1 && (splits[splits.length-1] - splits[splits.length-2]) < HOUR) {
			splits[splits.length-2] = splits[splits.length-1];
			long[] newArr = new long[splits.length-1];
			System.arraycopy(splits, 0, newArr, 0, newArr.length);
			return newArr;
		}
		return splits;
	}
    
    @SuppressWarnings("static-access")
	public Connection getConnection() {
		 try {
	      if (null == this.connection) {
	        // The connection was closed; reinstantiate it.
	    	Class.forName(conf.get("analysis.jdbc.driver.class"));
	    	if(conf.get("analysis.jdbc.username") == null) {
    	      this.connection = DriverManager.getConnection(conf.get("analysis.jdbc.url"));
    	    } else {
    	      this.connection = DriverManager.getConnection(
    	          conf.get("analysis.jdbc.url"), 
    	          conf.get("analysis.jdbc.username"), 
    	          conf.get("analysis.jdbc.password"));
    	    }
	      }
	    } catch (Exception e) {
	    	this.connection = null;
	    	LOG.error("analysis.jdbc.url=%s , analysis.jdbc.username=%s , analysis.jdbc.password=%s".format(conf.get("analysis.jdbc.url"), conf.get("analysis.jdbc.username"),conf.get("analysis.jdbc.password")));
	    	LOG.error("Get Input Connection fail , message is : " + e.getMessage(), e);
	    }
	    return this.connection;
	}

	@Override
	public RecordReader<OpaqInputKey, OpaqInputValue> createRecordReader(
			InputSplit split, TaskAttemptContext context) throws IOException,
			InterruptedException {
		
		while (null == this.connection) {
			this.connection = getConnection();
			if(null == this.connection){
				try {
					Thread.sleep(1000 * 60);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		return new OpaqRecordReader((OpaqInputSplit)split, this.connection, conf);
	}

	@Override
	public List<InputSplit> getSplits(JobContext job) throws IOException,
			InterruptedException {	
		Configuration conf = job.getConfiguration();
		splitField = conf.get("analysis.jdbc.split.field.name");
		long tasks = conf.getLong("analysis.jdbc.task.num", 12);
		String sql = conf.get("analysis.jdbc.input.query.sql");
		
		List<InputSplit> splits = new ArrayList<InputSplit>();
		Date begin = null;
		Date end = null;
		try {
			begin = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(conf.get("analysis.jdbc.input.query.begintime"));
			end = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(conf.get("analysis.jdbc.input.query.endtime"));
		} catch (ParseException e) {
			throw new IOException(e.getMessage(),e);
		}

		long[] splitPoints = split(tasks, begin.getTime(), end.getTime());
		long start = splitPoints[0];
		Date startDate = new Date(start);
		
		for(int i=1; i<splitPoints.length; i++) {
			StringBuffer sqlBuf = new StringBuffer();
			long stop = splitPoints[i];
			Date stopDate = new Date(stop);
			sqlBuf.append(sql);
			sqlBuf.append(" and ("+splitField+" >= '"+(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startDate))+"')");
			sqlBuf.append(" and ("+splitField+" < '"+(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(stopDate))+"')");
			splits.add(new OpaqInputSplit(sqlBuf.toString()));
			LOG.info(sqlBuf.toString());
			start = stop;
			startDate = stopDate;
		}
		
		return splits;
	}

	@Override
	public Configuration getConf() {
		return conf;
	}

	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;
		
		while (null == this.connection) {
			this.connection = getConnection();
			if(null == this.connection){
				try {
					Thread.sleep(1000 * 60);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
