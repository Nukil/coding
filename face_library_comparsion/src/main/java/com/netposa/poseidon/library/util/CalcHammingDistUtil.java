package com.netposa.poseidon.library.util;

public class CalcHammingDistUtil {
	static final int FEATURE_SIZE = 256;
	static final int HAMMING_DIST_TH = 840;
	/**
	 * 海明距离比对算法
	 * @param featureBuff1 	待比对特征1
	 * @param offset1  	特征1起始点下标(默认为零)
	 * @param featureBuff2	待比对特征2
	 * @param offset2	特征2起始点下标(默认为零)
	 * @return	两个特征的相似度
	 */
	public static float calcHammingDist( byte[] featureBuff1,
			int offset1, byte[] featureBuff2, int offset2) {
		if (offset1 + FEATURE_SIZE > featureBuff1.length
				|| offset2 + FEATURE_SIZE > featureBuff2.length) {
			return 0;
		}
		int x, tmpXor;
		float sumXor = 0;
		for (int i = 0; i < FEATURE_SIZE; i++) {
			x = (featureBuff1[offset1 + i] ^ featureBuff2[offset2 + i]) & 0XFF;
			for (tmpXor = 0; x != 0; tmpXor++) {
				x &= (x - 1);
			}
			sumXor += tmpXor;
		}
		if (sumXor < 0) {
			return -1;
		}
		return (float) (100.0 * Math.exp(-1.0 * sumXor/HAMMING_DIST_TH));
	}
}

