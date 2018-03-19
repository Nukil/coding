package com.nukil.parse.util.service;

import com.nukil.parse.util.bean.FaceBean;
import com.nukil.parse.util.bean.FaceParams;
import com.nukil.parse.util.bean.HumanBean;
import com.nukil.parse.util.bean.HumanParams;
import com.nukil.parse.util.impl.FaceDataETL;
import com.nukil.parse.util.impl.HumanDataETL;

import java.util.List;

public class DataAnalyzeUtil {
    public static FaceBean faceDataAnalyze(byte[] message, int featureSize, List<FaceParams> params) {
        return new FaceDataETL().dataAnalyze(message, featureSize, params);
    }

    public static List<FaceBean> faceDataBtachAnalyze(List<byte[]> messages, int featureSize, List<FaceParams> params) {
        return new FaceDataETL().dataBatchAnalyze(messages, featureSize, params);
    }

    public static HumanBean humanDataAnalyze(byte[] message, int featureSize, List<HumanParams> params) {
        return new HumanDataETL().dataAnalyze(message, featureSize, params);
    }

    public static List<HumanBean> humanDataBtachAnalyze(List<byte[]> messages, int featureSize, List<HumanParams> params) {
        return new HumanDataETL().dataBatchAnalyze(messages, featureSize, params);
    }
}
