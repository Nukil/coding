package netposa.fakecar.feature.rpc;

import java.io.Serializable;
import java.util.Arrays;

import netposa.fakecar.feature.rpc.util.ByteUtils;

public class KeyRecord implements Serializable, Comparable<KeyRecord> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4611051601255636616L;
	
	private byte[] licence_plate;
	private byte[] licence_plate_color;
	private int head_rear = Byte.MIN_VALUE;
	
	
	public KeyRecord() {
	}
	
	public KeyRecord(byte[] licence_plate) {
		this.licence_plate = licence_plate;
	}
	
	public KeyRecord(byte[] licence_plate, byte[] licence_plate_color) {
		this.licence_plate = licence_plate;
		this.licence_plate_color = licence_plate_color;
	}
	public KeyRecord(byte[] licence_plate, int head_rear) {
		this.licence_plate = licence_plate;
		this.head_rear = head_rear;
	}
	
	public KeyRecord(byte[] licence_plate, byte[] licence_plate_color, int head_rear) {
		this.licence_plate = licence_plate;
		this.licence_plate_color = licence_plate_color;
		this.head_rear = head_rear;
	}
	

	public byte[] getLicence_plate() {
		return licence_plate;
	}
	public void setLicence_plate(byte[] licence_plate) {
		this.licence_plate = licence_plate;
	}
	public byte[] getLicence_plate_color() {
		return licence_plate_color;
	}
	public void setLicence_plate_color(byte[] licence_plate_color) {
		this.licence_plate_color = licence_plate_color;
	}

	public int getHead_rear() {
		return head_rear;
	}

	public void setHead_rear(int head_rear) {
		this.head_rear = head_rear;
	}

	@Override
	public int compareTo(KeyRecord o) {
		int compare = ByteUtils.compareTo(licence_plate, 0, licence_plate.length,
				o.licence_plate, 0, o.licence_plate.length);
		
		if (compare != 0) {
			return compare;
		}
		
		if (null != licence_plate_color && null != o.licence_plate_color) {
			compare = ByteUtils.compareTo(licence_plate_color, 0, licence_plate_color.length,
					o.licence_plate_color, 0, o.licence_plate_color.length);
			
			if (compare != 0) {
				return compare;
			}
		}
		
		return head_rear - o.getHead_rear();
	}

	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(licence_plate);
		result = prime * result + Arrays.hashCode(licence_plate_color);
		if(head_rear > Byte.MIN_VALUE)
			result = prime * result + prime * head_rear;
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
		KeyRecord other = (KeyRecord) obj;
		if (!Arrays.equals(licence_plate, other.licence_plate))
			return false;
		if (!Arrays.equals(licence_plate_color, other.licence_plate_color))
			return false;
		if(head_rear != other.getHead_rear()) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "KeyRecord [licence_plate=" + new String(licence_plate)
				+ ", licence_plate_color="
				+ (licence_plate_color == null 
				? "null" : new String(licence_plate_color)) + "head_rear=" + head_rear + "]";
	}
	
}
