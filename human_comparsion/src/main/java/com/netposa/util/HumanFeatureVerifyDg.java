package com.netposa.util;

public class HumanFeatureVerifyDg {

    private static final int FLOAT_SIZE = 4;
    private static final int FEAT_RAW_DIM = 256;

    public static float verify(byte[] feat1, int offset1, byte[] feat2, int offset2) {
        float score = 0.0f;
        if (feat1.length < FLOAT_SIZE * FEAT_RAW_DIM || feat2.length < FLOAT_SIZE * FEAT_RAW_DIM) {
            return score;
        }
        for (int i = 0; i < FEAT_RAW_DIM; ++i) {
            float first = byte2float(feat1, i * FLOAT_SIZE + offset1);
            float second = byte2float(feat2, i * FLOAT_SIZE + offset2);
            score -= first * second;
        }

        return (float)(0.5 - 0.5 * score) * 100;
    }

    /**
     * 字节转换为浮点
     * @param b 字节（至少4个字节）
     * @param index 开始位置
     * @return
     */
    public static float byte2float(byte[] b, int index) {
        int l;
        l = b[index];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= ((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long) b[index + 3] << 24);
        return Float.intBitsToFloat(l);
    }
}