package com.netposa.bean;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by dell on 2017/6/29.
 */
public class CacheBean implements Serializable{
    private Map<String,byte[]> features; //特征
    private String ext; // 其它结构化信息

    public CacheBean() {
    }

    public CacheBean(Map<String, byte[]> features, String ext) {
        this.features = features;
        this.ext = ext;
    }
    public Map<String, byte[]> getFeatures() {
        return features;
    }

    public void setFeatures(Map<String, byte[]> features) {
        this.features = features;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }
}
