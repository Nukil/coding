package com.netposa.poseidon.face.util;

public class Byte2FloatUtil {
    /**
     * 字节转换为浮点
     * @param b 字节（至少4个字节）
     * @param index 开始位置
     * @return float
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
