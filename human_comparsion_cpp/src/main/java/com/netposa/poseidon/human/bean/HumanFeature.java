package com.netposa.poseidon.human.bean;

/**
 * 人脸特征数据封装Bean(对应特征数据存储格式)
 * @author Y.yong
 * 2016年10月27日
 */
public class HumanFeature implements Comparable<HumanFeature>{
	/**
	 * 记录编号
	 */
	private String jlbh;
	/**
	 * 采集时间
	 */
	private String gatherTime;
	/**
	 * 相机编号
	 */
	private String cameraId;
	/**
	 * 特征数据
	 */
	private byte[] feature;
	
	public HumanFeature() {
	}
	
	public HumanFeature(String jlbh, String gatherTime, String cameraId, byte[] feature) {
		super();
		this.jlbh = jlbh;
		this.gatherTime = gatherTime;
		this.cameraId = cameraId;
		this.feature = feature;
	}

	public String getJlbh() {
		return jlbh;
	}

	public void setJlbh(String jlbh) {
		this.jlbh = jlbh;
	}

	public String getGatherTime() {
		return gatherTime;
	}

	public void setGatherTime(String gatherTime) {
		this.gatherTime = gatherTime;
	}

	public String getCameraId() {
		return cameraId;
	}

	public void setCameraId(String cameraId) {
		this.cameraId = cameraId;
	}

	public byte[] getFeature() {
		return feature;
	}

	public void setFeature(byte[] feature) {
		this.feature = feature;
	}

	@Override
	public int compareTo(HumanFeature o) {
		return this.getGatherTime().compareTo(o.getGatherTime());
	}

}
