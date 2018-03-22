package com.netposa.poseidon.fakecardetect.utils;

import com.netposa.poseidon.fakecardetect.bean.PropConfig;

public class FeatureVertifyUtil {
	private static  PropConfig propConfig = new PropConfig();
	private static final int VERSION_SIZE = propConfig.feature_version_size();
	private static final int BYTES_SIZE = propConfig.feature_bytes_size();
	private static final int FEATURE_SIZE = propConfig.feature_size();
	private static final int BUFF_BEGIN_OFFSET = VERSION_SIZE + BYTES_SIZE;
	private static final int FEATURE_BIT_SIZE = FEATURE_SIZE * 8;
	private static final int[] BIT_FLAGS = {0x01,0x02,0x04,0x08,0x10,0x20,0x40,0x80};
	private static final int HAMMING_DIST_TH = 612;
	private static int FLOAT_SIZE = 4;
	private static final int FLOAT_LENGTH = VERSION_SIZE + BYTES_SIZE + FEATURE_SIZE / FLOAT_SIZE;

	/**
	 * 格灵深瞳特征相似度计算
	 * @param featureBuff1
	 * @param featureBuff2
	 * @return
	 */
	public static float Compute_reid_score(byte[] featureBuff1, byte[] featureBuff2){
		float score = 0.0f;
		float feat_float1;
		float feat_float2;

		for (int i = 0; i < FLOAT_LENGTH; i++) {
			feat_float1 = byte2float(featureBuff1, i * FLOAT_SIZE);
			feat_float2 = byte2float(featureBuff2, i * FLOAT_SIZE);
			score -= feat_float1 * feat_float2;
		}
		score = (float)(0.5 - 0.5 * score) * 100;
		return score;
	}
	/**
	 * 海明距离比对
	 *
	 * @param featureBuff1
	 * @param featureBuff2
	 * @return
	 *
	 */
	public static float CalcHammingDist(byte[] featureBuff1, byte[] featureBuff2) {
	    boolean headerFlag = true;
	    if(featureBuff1.length<BUFF_BEGIN_OFFSET || featureBuff2.length<BUFF_BEGIN_OFFSET){
	    	return 0;
	    }
	    for(int i=VERSION_SIZE; i<BUFF_BEGIN_OFFSET; i++) {
	        if (featureBuff1[i] != featureBuff2[i]) {
	            headerFlag = false;
	            break;
	        }
	    }
	    if (!headerFlag) {
	        return 0;
	    }
	    int iByte,iBit,sumXor = 0;
	    for(int i=0; i<FEATURE_BIT_SIZE; i++) {
	    	iByte = i / 8 + BUFF_BEGIN_OFFSET;
	        iBit = i % 8;
	        if (((featureBuff1[iByte] & BIT_FLAGS[iBit]) ^ (featureBuff2[iByte] & BIT_FLAGS[iBit])) != 0x00) {
	            sumXor ++;
	        }
	    }
	    if (sumXor < 0) {
	    	return -1;
	    }
	    return (float)(100*Math.exp(-1.0*sumXor/HAMMING_DIST_TH));
	}
	
	public static float CalcHammingDist(byte[] featureBuff1, byte[] featureBuff2, int dissimilarity_count) {
		
		if (FEATURE_SIZE > featureBuff1.length || FEATURE_SIZE > featureBuff2.length) {
			return 0;
		}
	    int iByte,iBit,sumXor = 0;
	    for(int i=0; i<FEATURE_BIT_SIZE; i++) {
	    	iByte = i / 8 + BUFF_BEGIN_OFFSET;
	        iBit = i % 8;
	        if (((featureBuff1[iByte] & BIT_FLAGS[iBit]) ^ (featureBuff2[iByte] & BIT_FLAGS[iBit])) != 0x00) {
	            sumXor ++;
	        }
	        if(sumXor>dissimilarity_count){
	        	sumXor = -1;
	        	break;
	        }
	    }
	    if (sumXor < 0) {
	    	return -1;
	    }
	    return (float)(100*Math.exp(-1.0*sumXor/HAMMING_DIST_TH));
	}
	
    public static int getDistanceDic(float distance) {
    	int i = 0;
    	double dis = 100;
    	while (dis>=distance && i<=256*8) {
    		i++;
    		dis = 100*Math.exp(-1.0*i/HAMMING_DIST_TH);
    	}
    	return i-1;
    }

	/**
	 * 字节转换为浮点
	 * @param b 字节（至少4个字节）
	 * @param index 开始位置
	 * @return
	 */
	private static float byte2float(byte[] b, int index) {
		int l;
		l = b[index + 0];
		l &= 0xff;
		l |= ((long) b[index + 1] << 8);
		l &= 0xffff;
		l |= ((long) b[index + 2] << 16);
		l &= 0xffffff;
		l |= ((long) b[index + 3] << 24);
		return Float.intBitsToFloat(l);
	}
}

