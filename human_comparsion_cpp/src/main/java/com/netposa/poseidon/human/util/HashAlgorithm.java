package com.netposa.poseidon.human.util;

import org.apache.hadoop.hbase.util.Bytes;

public class HashAlgorithm {
    private static LoadPropers instance = LoadPropers.getSingleInstance();
    private static int regionNum = Integer.parseInt(instance.getProperties("server").getProperty("hbase.split.region.num", "50").trim());
    private static final String formatStr = "%0" + String.valueOf(regionNum - 1).length() + "d" ;
    public static final int BKDRHashValue = 1024;
    public static String hash(String key) {
        return String.format(formatStr, Math.abs((key.hashCode() % regionNum))) + key;
    }
    public static byte[][] getSplitKeys() {
        if (regionNum < 1) {
            regionNum = 1;
        }
        byte[][] splitKeys = new byte[regionNum - 1][];
        int span = 1;
        int splitKey = 0;
        for (int i = 1; i < regionNum; i++) {
            splitKey = splitKey + span;
            splitKeys[i - 1] = Bytes.toBytes(String.format(formatStr, splitKey));
        }
        return splitKeys;
    }
    /**
     * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和和intToBytes（）配套使用
     *
     * @param src
     *            byte数组
     * @param offset
     *            从数组的第offset位开始
     * @return int数值
     */
    public static int bytesToInt(byte[] src, int offset) {
        int value;
        value = (int) ((src[offset] & 0xFF)
                | ((src[offset+1] & 0xFF)<<8)
                | ((src[offset+2] & 0xFF)<<16)
                | ((src[offset+3] & 0xFF)<<24));
        return value;
    }

    /**
     * byte数组中取int数值，本方法适用于(低位在后，高位在前)的顺序。和intToBytes2（）配套使用
     */
    public static int bytesToInt2(byte[] src, int offset) {
        int value;
        value = (int) ( ((src[offset] & 0xFF)<<24)
                |((src[offset+1] & 0xFF)<<16)
                |((src[offset+2] & 0xFF)<<8)
                |(src[offset+3] & 0xFF));
        return value;
    }

    /**
     * BKDR Hash
     */
    public static int BKDRHash(String str)
    {
        int seed = 131; // 31 131 1313 13131 131313 etc..
        int hash = 0;
        for(int i = 0; i < str.length(); i++) {
            hash = (hash * seed) + str.charAt(i);
        }
        return (hash & 0x7FFFFFFF);
    }
}
