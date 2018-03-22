package netposa.firstincity.analysis.lib.db;

import scala.Serializable;

public class FirstincityOutputValue implements Serializable {

	private static final long serialVersionUID = 893326176876660025L;
	
	private String trafficid;
	private long passtime;
	private int speed;
	private String orgid;
	private String roadmonitorid;
	private String channelid;
	private String platenumber;
	private String platecolor;
	private String platetype;
	private String vehiclecolor;
	private String vehicletype;
	private String vehiclebrand;
	private String vehiclesubbrand;
	private String vehiclemodelyear;
	private String backtype;
	
	public FirstincityOutputValue(){}
	
	public FirstincityOutputValue(OpaqInputValue value, String backtype) {
		this.trafficid = value.getJlbh();
		this.passtime = value.getJgsj();
		this.speed = value.getClsd();
		this.orgid = value.getXzqh();
		this.roadmonitorid = value.getKkbh();
		this.channelid = value.getCdbh();
		this.platenumber = value.getHphm();
		this.platecolor = value.getHpys();
		this.platetype = value.getHpzl();
		this.vehiclecolor = value.getCsys();
		this.vehicletype = value.getCllx();
		this.vehiclebrand = value.getClpp();
		this.vehiclesubbrand = value.getClzpp();
		this.vehiclemodelyear = value.getClnk();
		this.backtype = backtype;
	}

	public String getTrafficid() {
		return trafficid;
	}

	public void setTrafficid(String trafficid) {
		this.trafficid = trafficid;
	}

	public long getPasstime() {
		return passtime;
	}

	public void setPasstime(long passtime) {
		this.passtime = passtime;
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public String getOrgid() {
		return orgid;
	}

	public void setOrgid(String orgid) {
		this.orgid = orgid;
	}

	public String getRoadmonitorid() {
		return roadmonitorid;
	}

	public void setRoadmonitorid(String roadmonitorid) {
		this.roadmonitorid = roadmonitorid;
	}

	public String getChannelid() {
		return channelid;
	}

	public void setChannelid(String channelid) {
		this.channelid = channelid;
	}

	public String getPlatenumber() {
		return platenumber;
	}

	public void setPlatenumber(String platenumber) {
		this.platenumber = platenumber;
	}

	public String getPlatecolor() {
		return platecolor;
	}

	public void setPlatecolor(String platecolor) {
		this.platecolor = platecolor;
	}

	public String getPlatetype() {
		return platetype;
	}

	public void setPlatetype(String platetype) {
		this.platetype = platetype;
	}

	public String getVehiclecolor() {
		return vehiclecolor;
	}

	public void setVehiclecolor(String vehiclecolor) {
		this.vehiclecolor = vehiclecolor;
	}

	public String getVehicletype() {
		return vehicletype;
	}

	public void setVehicletype(String vehicletype) {
		this.vehicletype = vehicletype;
	}

	public String getVehiclebrand() {
		return vehiclebrand;
	}

	public void setVehiclebrand(String vehiclebrand) {
		this.vehiclebrand = vehiclebrand;
	}

	public String getVehiclesubbrand() {
		return vehiclesubbrand;
	}

	public void setVehiclesubbrand(String vehiclesubbrand) {
		this.vehiclesubbrand = vehiclesubbrand;
	}

	public String getVehiclemodelyear() {
		return vehiclemodelyear;
	}

	public void setVehiclemodelyear(String vehiclemodelyear) {
		this.vehiclemodelyear = vehiclemodelyear;
	}

	public String getBacktype() {
		return backtype;
	}

	public void setBacktype(String backtype) {
		this.backtype = backtype;
	}

}
