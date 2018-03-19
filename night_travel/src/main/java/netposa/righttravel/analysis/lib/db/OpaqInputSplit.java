package netposa.righttravel.analysis.lib.db;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputSplit;

public class OpaqInputSplit extends InputSplit implements Writable {
	private long start = 0;
	private long end = 0;
	private String sql;
	
	public OpaqInputSplit(){}
	
	public OpaqInputSplit(String sql){
		this.sql = sql;
	}
	
	
	public long getStart() {
		return start;
	}
	
	public long getEnd() {
		return end;
	}
	
	public String getSql() {
		return sql;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.sql = Text.readString(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		Text.writeString(out, sql);
	}

	@Override
	public long getLength() throws IOException, InterruptedException {
		return end - start;
	}

	@Override
	public String[] getLocations() throws IOException, InterruptedException {
		return new String[]{};
	}

}
