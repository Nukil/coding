package com.netposa.poseidon.human.bean;

public class FetcherDataBean {
    /**
     * 特征数据偏移位置
     */
    private int offset;
    /**
     * 日志编号
     */
    private String recordId;
    /**
     * 相机编号
     */
    private String cameraId;
    /**
     * 采集时间
     */
    private String gatherTime;

    public FetcherDataBean(int offset, String recordId, String cameraId, String gatherTime) {
        this.offset = offset;
        this.recordId = recordId;
        this.cameraId = cameraId;
        this.gatherTime = gatherTime;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    public String getGatherTime() {
        return gatherTime;
    }

    public void setGatherTime(String gatherTime) {
        this.gatherTime = gatherTime;
    }
}
