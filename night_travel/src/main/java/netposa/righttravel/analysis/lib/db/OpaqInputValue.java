package netposa.righttravel.analysis.lib.db;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

import scala.Serializable;

public class OpaqInputValue implements WritableComparable<OpaqInputValue>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2902625683721697980L;
	
	private long trafficTime;

	private String searchDate;

	
	public OpaqInputValue(){}
	

	
	public boolean readFields(ResultSet rs, String searchDate) throws SQLException {
		trafficTime = rs.getTimestamp(3).getTime();
		this.searchDate = searchDate;
		//importTime = rs.getTimestamp(4).getTime();
		return true;
	}



	@Override
	public void readFields(DataInput in) throws IOException {
		trafficTime = in.readLong();
		searchDate = Text.readString(in);
	}



	@Override
	public void write(DataOutput out) throws IOException {
		out.writeLong(trafficTime);
		Text.writeString(out, searchDate);
	}



	@Override
	public int compareTo(OpaqInputValue o) {
		long comp = trafficTime - o.trafficTime;
		if (comp != 0) {
			return comp > 0 ? 1 : -1;
		}
		return 0;
	}



	@Override
	public String toString() {
		return "OpaqInputValue [trafficTime=" + trafficTime +
				", searchDate=" + searchDate + "]";
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		//result = prime * result + (int) (importTime ^ (importTime >>> 32));
		result = prime * result
				+ ((searchDate == null) ? 0 : searchDate.hashCode());
		result = prime * result + (int) (trafficTime ^ (trafficTime >>> 32));
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OpaqInputValue other = (OpaqInputValue) obj;
		/*
		if (importTime != other.importTime)
			return false;
		*/
		if (searchDate == null) {
			if (other.searchDate != null)
				return false;
		} else if (!searchDate.equals(other.searchDate))
			return false;
		if (trafficTime != other.trafficTime)
			return false;
		return true;
	}



	public long getTrafficTime() {
		return trafficTime;
	}

	public void setTrafficTime(long trafficTime) {
		this.trafficTime = trafficTime;
	}
/*
	public long getImportTime() {
		return importTime;
	}

	public void setImportTime(long importTime) {
		this.importTime = importTime;
	}
*/
	public String getSearchDate() {
		return searchDate;
	}

	public void setSearchDate(String searchDate) {
		this.searchDate = searchDate;
	}
	
	

}
