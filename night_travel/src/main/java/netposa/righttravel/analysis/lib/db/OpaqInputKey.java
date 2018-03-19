package netposa.righttravel.analysis.lib.db;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

import scala.Serializable;

public class OpaqInputKey implements WritableComparable<OpaqInputKey>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2902625683721697980L;
	
	private String plateNum;
	private String plateNumColor;
	
	public OpaqInputKey(){}
	
	public OpaqInputKey(String plateNum, String plateNumColor) {
		this.plateNum = plateNum;
		this.plateNumColor = plateNumColor;
	}
	
	public boolean readFields(ResultSet rs, String plateNum) throws SQLException {
		this.plateNum = plateNum;
		
		plateNumColor = rs.getString(2);
		plateNumColor = StringUtils.trimToEmpty(plateNumColor);
		
		return true;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.plateNum = Text.readString(in);
		this.plateNumColor = Text.readString(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		Text.writeString(out, plateNum);
		Text.writeString(out, plateNumColor);
	}

	@Override
	public int compareTo(OpaqInputKey o) {
		int comp = this.plateNum.compareTo(o.plateNum);
		if (comp != 0) return comp;
		comp = this.plateNumColor.compareTo(o.plateNumColor);
		if (comp != 0) return comp;
		return 0;
	}
	
	@Override
	public String toString() {
		return "OpaqInputKey [plateNum=" + plateNum + ", plateNumColor="
				+ plateNumColor + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((plateNum == null) ? 0 : plateNum.hashCode());
		result = prime * result
				+ ((plateNumColor == null) ? 0 : plateNumColor.hashCode());
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
		OpaqInputKey other = (OpaqInputKey) obj;
		if (plateNum == null) {
			if (other.plateNum != null)
				return false;
		} else if (!plateNum.equals(other.plateNum))
			return false;
		if (plateNumColor == null) {
			if (other.plateNumColor != null)
				return false;
		} else if (!plateNumColor.equals(other.plateNumColor))
			return false;
		return true;
	}
	
	

	public String getPlateNum() {
		return plateNum;
	}

	public void setPlateNum(String plateNum) {
		this.plateNum = plateNum;
	}

	public String getPlateNumColor() {
		return plateNumColor;
	}

	public void setPlateNumColor(String plateNumColor) {
		this.plateNumColor = plateNumColor;
	}

}
