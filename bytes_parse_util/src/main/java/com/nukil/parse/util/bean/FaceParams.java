package com.nukil.parse.util.bean;

public enum FaceParams {
    /**
     * 记录编号
     */
    RECORD_ID("recordId"),

    /**
     * 抓拍时间戳
     */
    ABS_TIME("absTime"),


    /**
     * 推送时间戳(统计延迟使用)
     */
    PUSH_TIME("pushTime"),

    /**
     * 0,卡口,1-实时,2-历史
     */
    SOURCE_TYPE("sourceType"),

    /**
     * 设备id/文件id
     */
    SOURCE_ID("sourceId"),

    /**
     * 原始人脸属性信息（暂无，保留）
     */
    FEATURES_INFO("featuresInfo");

    private final String value;

    FaceParams(String value) {
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}
