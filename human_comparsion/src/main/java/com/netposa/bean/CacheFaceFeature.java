package com.netposa.bean;

import java.io.Serializable;

/**
 * 人脸特征数据封装Bean(对应特征数据存储格式)
 * @author Y.yong
 * 2016年10月27日
 */
public class CacheFaceFeature implements Serializable{
	/**
	 * 采集时间
	 */
	private byte[] gatherTime;
	/**
	 * 相机编号
	 */
	private byte[] cameraId;
	/**
	 * 特征数据
	 */
	private byte[] feature;
	
	public CacheFaceFeature() {
	}


	public CacheFaceFeature(byte[] gatherTime, byte[] cameraId,
			byte[] feature) {
		super();
		this.gatherTime = gatherTime;
		this.cameraId = cameraId;
		this.feature = feature;
	}

	public byte[] getGatherTime() {
		return gatherTime;
	}

	public byte[] getCameraId() {
		return cameraId;
	}

	public void setCameraId(byte[] cameraId) {
		this.cameraId = cameraId;
	}


	public void setGatherTime(byte[] gatherTime) {
		this.gatherTime = gatherTime;
	}



	public byte[] getFeature() {
		return feature;
	}



	public void setFeature(byte[] feature) {
		this.feature = feature;
	}


}
