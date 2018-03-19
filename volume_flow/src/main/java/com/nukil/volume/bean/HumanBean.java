package com.nukil.volume.bean;

public class HumanBean {
    private String date;
    private String hour;
    private String areaID;
    private String cameraID;
    private int count;
    private String tableName;
    private String uuid;

    public HumanBean(String date, String hour, String areaID, String cameraID, int count, String tableName, String uuid) {
        this.date = date;
        this.hour = hour;
        this.areaID = areaID;
        this.cameraID = cameraID;
        this.count = count;
        this.tableName = tableName;
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "HumanBean{" +
                "date='" + date + '\'' +
                ", hour='" + hour + '\'' +
                ", areaID='" + areaID + '\'' +
                ", cameraID='" + cameraID + '\'' +
                ", count=" + count +
                ", tableName='" + tableName + '\'' +
                ", uuid='" + uuid + '\'' +
                '}';
    }

    public String getSelectSql() {
        return String.format("select date_, camera_id, day_count, hour%s from %s where(date_='%s' and area_id='%s' and camera_id='%s')", hour, tableName, date, areaID, cameraID);
    }
    public String getInsertSql() {
        return String.format("insert into %s (date_, hour%s, area_id, camera_id, day_count) VALUES (%s, %s, %s, %s, %d)", tableName, hour, date, count, areaID, cameraID, count);
    }
    public String getUpdateSql(int dayCount, int hourCount) {
        return String.format("update %s set hour%s = %d, day_count = %d where(date_ = '%s' and area_id='%s' and camera_id='%s')", tableName, hour, hourCount, dayCount, date, areaID, cameraID);
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public String getAreaID() {
        return areaID;
    }

    public void setAreaID(String areaID) {
        this.areaID = areaID;
    }

    public String getCameraID() {
        return cameraID;
    }

    public void setCameraID(String cameraID) {
        this.cameraID = cameraID;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
