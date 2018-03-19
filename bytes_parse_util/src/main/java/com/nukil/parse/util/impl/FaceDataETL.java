package com.nukil.parse.util.impl;

import com.nukil.parse.util.bean.FaceBean;
import com.nukil.parse.util.bean.FaceParams;
import com.nukil.parse.util.inter.FaceDataETLInterface;
import com.nukil.parse.util.util.ParseBytesUtils;

import java.util.ArrayList;
import java.util.List;

public class FaceDataETL implements FaceDataETLInterface {
    @Override
    public FaceBean dataAnalyze(byte[] message, int featureSize, List<FaceParams> params) {
        ParseBytesUtils parseBytesUtils = new ParseBytesUtils();
        return parseBytesUtils.faceDataForETL2PB(message, featureSize, params);
    }

    @Override
    public List<FaceBean> dataBatchAnalyze(List<byte[]> messages, int featureSize, List<FaceParams> params) {
        List<FaceBean> faceBeans = new ArrayList<>();
        for (byte[] message : messages) {
            faceBeans.add(dataAnalyze(message, featureSize, params));
        }
        return faceBeans;
    }
}
