package com.netposa.poseidon.human.util;

import java.util.Properties;

public class HumanFeatureVerifyDg {
    private static final Properties properties = LoadPropers.getSingleInstance().getProperties("server");
    private static final int FLOAT_SIZE = 4;
    private static final int FEAT_RAW_DIM = Integer.parseInt(properties.getProperty("human.feature.size")) / FLOAT_SIZE;

    /*
    512特征比对
     */
    public static float verify(byte[] feat1, int offset1, byte[] feat2, int offset2) {
        float score = 0.0f;
        float feat_float1 = 0;
        float feat_float2 = 0;
        if (feat1.length < FLOAT_SIZE * FEAT_RAW_DIM || feat2.length < FLOAT_SIZE * FEAT_RAW_DIM) {
            return score;
        }
        for (int i = 0; i < FEAT_RAW_DIM; ++i) {
            float first = Byte2FloatUtil.byte2float(feat1, i * FLOAT_SIZE + offset1);
            float second = Byte2FloatUtil.byte2float(feat2, i * FLOAT_SIZE + offset2);
            feat_float1 += first * first;
            feat_float2 += second * second;
            score -= first * second;
        }
        return (float)(0.5 - 0.5 * score / (Math.sqrt(feat_float1) * Math.sqrt(feat_float2))) * 100;
    }
}