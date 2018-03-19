package netposa.righttravel.analysis.lib.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.util.StringUtils;

public class OpaqRecordWriter extends RecordWriter<OpaqInputKey, RightTravelOutputValue> {
	
	private static final Log LOG = LogFactory.getLog(OpaqRecordWriter.class);
	
	private Connection connection;
	private PreparedStatement statement;
	private Configuration conf;
	private String sql;
	private int batchSize = 0;
	
	public OpaqRecordWriter(){}
	
	public OpaqRecordWriter(Connection connection,PreparedStatement statement,
			Configuration conf,String sql) throws SQLException {
		this.connection = connection;
		this.statement = statement;
		this.conf = conf;
		this.sql = sql;
		
		//this.connection.setAutoCommit(false);
	}

	@Override
	public void close(TaskAttemptContext context) throws IOException,
			InterruptedException {
		
		try {
			statement.close();
			connection.close();
		}catch(SQLException e) {
			LOG.warn(StringUtils.stringifyException(e));
		}
	}

	@Override
	public void write(OpaqInputKey key, RightTravelOutputValue value)
			throws IOException, InterruptedException {
		try {
			statement.setString(1, value.getKey());
			statement.setString(2, key.getPlateNum());
			statement.setString(3, key.getPlateNumColor());
			statement.setString(4, value.getSearchDate());
			statement.setString(5, value.getResultType());
			statement.setString(6, value.getImportTime());
			statement.setInt(7, value.getCount());
			
			statement.execute();
			
			batchSize += 1;
			if (batchSize >= 200) {
				//LOG.info("execute output batch begin ................");
				statement.executeBatch();
				batchSize = 0;
			}
		} catch (SQLException e) {
			String sqlState = e.getSQLState();
			if ("08S01".equals(sqlState) || "40001".equals(sqlState)){
				try {
					statement.close();
					connection.close();
				}catch(SQLException e1) {
					LOG.warn(e1.getMessage());
				}
				connection = OpaqOutputFormat.getConnection(conf);
				try {
					statement = connection.prepareStatement(sql);
				} catch (SQLException e1) {
					LOG.warn(e1.getMessage());
				}
			}
			LOG.warn(StringUtils.stringifyException(e));
		}
	}

	public Connection getConnection() {
		return connection;
	}

	public PreparedStatement getStatement() {
		return statement;
	}

}
