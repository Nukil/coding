package com.nukil.parse.util.inter;

import com.nukil.parse.util.bean.HumanBean;
import com.nukil.parse.util.bean.HumanParams;

import java.util.List;

public interface HumanDataETLInterface {
    /**
     * 解析单条数据
     * @param message 待解析数据
     * @param size 特征长度
     * @param params 需要解析的字段
     * @return HumanBean
     */
    HumanBean dataAnalyze(byte[] message, int size, List<HumanParams> params);

    /**
     * 批量数据解析
     * @param messages 数据集
     * @param size 特征长度
     * @param params 待解析字段
     * @return list<HumanBean>
     */
    List<HumanBean> dataBatchAnalyze(List<byte[]> messages, int size, List<HumanParams> params);
}
