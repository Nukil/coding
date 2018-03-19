package com.nukil.parse.util.bean;

public class FaceBean {

    private StatusCode rCode;

    private String rMessage;

    private byte[] feature;

    /**
     * 记录唯一编号,可以对应原始数据
     */
    private String recordId;

    /**
     * 抓拍时间戳
     */
    private Long absTime;

    /**
     * 推送时间戳(统计延迟使用)
     */
    private Long pushTime;

    /**
     * 0,卡口,1-实时,2-历史
     */
    private Integer sourceType;

    /**
     * 设备id/文件id
     */
    private String sourceId;

    /**
     * 卡口/结构化原始数据
     */
    private StructSourceInfo sourceInfo;

    /**
     * 网力特征信息
     */
    private NpFeaturesInfo npFeaturesInfo;

    /**
     * 原始人脸属性信息（暂无，保留）
     */
    public String featuresInfo;

    public static final class StructSourceInfo {
        /**
         * 特征图,用于特征提取或二次识别只用
         */
        private String traitImgUrl;

        /**
         * 场景图保存在pfs上的位置
         */
        private String sceneImgUrl;
        /**
         * 人脸位置
         */
        private String location;
        /**
         * 目标出现时间戳
         */
        private Long startTime;

        /**
         * 目标消失时间戳
         */
        private Long endTime;
        /**
         * 置信度(0,1)
         */
        private Double confidence;

        /**
         * 存储时间
         */
        private Long saveTime;

        private String traitLocation;

    }

    public static final class NpFeaturesInfo {

        /**
         * 记录唯一编号,可以对应原始数据
         */
        private String recordId;

        /**
         * 推送时间戳
         */
        private Long absTime;

        /**
         * 推送时间戳(统计延迟使用)
         */
        private Long pushTime;

        /**
         * 年龄
         */
        private Integer age = -1;

        /**
         * 魅力指数
         */
        private Integer attractive = -1;

        /**
         * 表情
         */
        private Integer expression = -1;

        /**
         * 戴眼镜(0,1)
         */
        private Integer eyeGlass = -1;

        /**
         * 性别(0,1)
         */
        private Integer gender = -1;

        /**
         * 微笑(0,1)
         */
        private Integer smile = -1;

        /**
         * 太阳镜(0,1)
         */
        private Integer sunGlass = -1;

        /**
         * 口罩
         */
        private Integer mask = -1;

    }

    public StatusCode getrCode() {
        return rCode;
    }

    public void setrCode(StatusCode rCode) {
        this.rCode = rCode;
    }

    public String getrMessage() {
        return rMessage;
    }

    public void setrMessage(String rMessage) {
        this.rMessage = rMessage;
    }

    public byte[] getFeature() {
        return feature;
    }

    public void setFeature(byte[] feature) {
        this.feature = feature;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public Long getAbsTime() {
        return absTime;
    }

    public void setAbsTime(Long absTime) {
        this.absTime = absTime;
    }

    public Long getPushTime() {
        return pushTime;
    }

    public void setPushTime(Long pushTime) {
        this.pushTime = pushTime;
    }

    public Integer getSourceType() {
        return sourceType;
    }

    public void setSourceType(Integer sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getFeaturesInfo() {
        return featuresInfo;
    }

    public void setFeaturesInfo(String featuresInfo) {
        this.featuresInfo = featuresInfo;
    }
}
