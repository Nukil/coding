package netposa.righttravel.analysis.lib.db;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

import scala.Serializable;

public class RightTravelOutputValue implements
		WritableComparable<RightTravelOutputValue>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 893326176876660025L;
	
	private String searchDate;
	private String resultType;
	private int count;
	private String importTime;
	private String key;
	
	public RightTravelOutputValue(){}
	
	public RightTravelOutputValue(String searchDate, String resultType,
			int count, String importTime, String key) {
		this.searchDate = searchDate;
		this.resultType = resultType;
		this.count = count;
		this.importTime = importTime;
		this.key = key;
	}



	@Override
	public void readFields(DataInput in) throws IOException {
		searchDate = Text.readString(in);
		resultType = Text.readString(in);
		count = in.readInt();
		importTime = Text.readString(in);
		key = Text.readString(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		Text.writeString(out, searchDate);
		Text.writeString(out, resultType);
		out.writeInt(count);
		Text.writeString(out, importTime);
		Text.writeString(out, key);
	}

	@Override
	public int compareTo(RightTravelOutputValue o) {
		return count - o.count;
	}
	
	
	

	public String getSearchDate() {
		return searchDate;
	}

	public void setSearchDate(String searchDate) {
		this.searchDate = searchDate;
	}

	public String getResultType() {
		return resultType;
	}

	public void setResultType(String resultType) {
		this.resultType = resultType;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getImportTime() {
		return importTime;
	}

	public void setImportTime(String importTime) {
		this.importTime = importTime;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	
}
