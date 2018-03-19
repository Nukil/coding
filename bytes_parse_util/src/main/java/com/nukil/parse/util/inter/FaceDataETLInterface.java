package com.nukil.parse.util.inter;

import com.nukil.parse.util.bean.FaceBean;
import com.nukil.parse.util.bean.FaceParams;

import java.util.List;

public interface FaceDataETLInterface {
    /**
     * 解析单条数据
     * @param message 待解析数据
     * @param size 特征长度
     * @param params 需要解析的字段
     * @return FaceBean
     */
    FaceBean dataAnalyze(byte[] message, int size, List<FaceParams> params);

    /**
     * 批量数据解析
     * @param messages 数据集
     * @param size 特征长度
     * @param params 待解析字段
     * @return list<FaceBean>
     */
    List<FaceBean> dataBatchAnalyze(List<byte[]> messages, int size, List<FaceParams> params);
}