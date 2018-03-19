package com.netposa.poseidon.library.util;

import com.netposa.poseidon.library.init.LoadPropers;
import org.apache.hadoop.hbase.util.Bytes;
public class HashAlgorithm {
    private static int regionNum = Integer.parseInt(LoadPropers.getProperties().getProperty("hbase.split.region.num", "50").trim());
    
    public static String keyHash(String key) {
        String formatStr = "%0" + String.valueOf(regionNum - 1).length() + "d" ;
        return String.format(formatStr, Math.abs((key.hashCode() % regionNum))) + key;
    }
    
    public static String hash(String key,Integer region) {
        if(region == null){
            return keyHash(key);
        }
        String formatStr = "%0" + String.valueOf(region - 1).length() + "d" ;
        return String.format(formatStr, Math.abs((key.hashCode() % region))) + key;
    }
    public static byte[][] getSplit() {
        if (regionNum < 1) {
            regionNum = 1;
        }
        String formatStr = "%0" + String.valueOf(regionNum - 1).length() + "d" ;
        byte[][] splitKeys = new byte[regionNum - 1][];
        int span = 1;
        int splitKey = 0;
        for (int i = 1; i < regionNum; i++) {
            splitKey = splitKey + span;
            splitKeys[i - 1] = Bytes.toBytes(String.format(formatStr, splitKey));
        }
        return splitKeys;
    }
    public static byte[][] getSplitKeys(Integer region) {
        if(region == null){
            return getSplit();
        }
        if (region < 1) {
            region = 1;
        }
        String formatStr = "%0" + String.valueOf(region - 1).length() + "d" ;
        byte[][] splitKeys = new byte[region - 1][];
        int span = 1;
        int splitKey = 0;
        for (int i = 1; i < region; i++) {
            splitKey = splitKey + span;
            splitKeys[i - 1] = Bytes.toBytes(String.format(formatStr, splitKey));
        }
        return splitKeys;
    }
    
    public static String getProfix(String key) {
        String formatStr = "%0" + String.valueOf(regionNum - 1).length() + "d" ;
        return String.format(formatStr, Math.abs((key.hashCode() % regionNum)));
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
}
