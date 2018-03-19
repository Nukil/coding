package com.netposa.poseidon.face.util;

/**
 * 格林深瞳人脸特征比对算法 Created by zhangxd on 2017/06/09.
 */
public final class FaceFeatureVerifyDg {

	private FaceFeatureVerifyDg() {

	}

	private static final int FLOAT_SIZE = 4; // FLOAT大小，按字节
	private static final int FEAT_RAW_DIM = 128; // 裸特征维度

	private static final double[] x = new double[] { 2.0027446832091833, 23.990992009634173, 38.904877294196034,
			26.741325612484186, 0.9302016548098435 };
	private static final double[] y = new double[] { -1.0013723416045917, -16.387118663615745, -26.947079625741388,
			-18.27199669225456, 0.3199681003954373 };
	private static final double[] k = new double[] { 0.845993, 0.885992, 0.910992, 0.945991 };

	/**
	 * 字节转换为浮点
	 *
	 * @param b
	 *            字节（至少4个字节）
	 * @param index
	 *            开始位置
	 * @return
	 */
	public static float byte2float(byte[] b, int index) {
		int intValue;
		intValue = b[index + 0];
		intValue &= 0xff;
		intValue |= ((long) b[index + 1] << 8);
		intValue &= 0xffff;
		intValue |= ((long) b[index + 2] << 16);
		intValue &= 0xffffff;
		intValue |= ((long) b[index + 3] << 24);
		return Float.intBitsToFloat(intValue);
	}

	public static float verify(byte[] pFaceFeat1, int offset1, byte[] pFaceFeat2, int offset2) {
		if (null == pFaceFeat1 || null == pFaceFeat2 || pFaceFeat1.length < FEAT_RAW_DIM * FLOAT_SIZE
				|| pFaceFeat2.length < FEAT_RAW_DIM * FLOAT_SIZE) {
			return 0;
		}
		float ftScore = .0f;
		float feat_float1 = 0;
		float feat_float2 = 0;
		for (int i = 0; i < FEAT_RAW_DIM; ++i) {
			float f1 = byte2float(pFaceFeat1, i * FLOAT_SIZE + offset1);
			float f2 = byte2float(pFaceFeat2, i * FLOAT_SIZE + offset2);
			ftScore += f1 * f2;
			feat_float1 += f1 * f1;
			feat_float2 += f2 * f2;
		}
		return (float) transform(0.5 + 0.5 * ftScore / (Math.sqrt(feat_float1) * Math.sqrt(feat_float2)))*100;
	}

	private static double transform(double originalSocre) {
		double tempScore = Math.exp(originalSocre) / (Math.exp(originalSocre) + 1);
		for (int i = 0; i < x.length; i++) {
			if (i == k.length) {
				return x[i] * tempScore + y[i];
			}
			if (originalSocre < k[i]) {
				return x[i] * tempScore + y[i];
			}
		}
		return 0.0;
	}
}