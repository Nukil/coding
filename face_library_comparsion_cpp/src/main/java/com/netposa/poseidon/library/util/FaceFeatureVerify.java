package com.netposa.poseidon.library.util;

/**
 * 特征比对算法
 *
 * @author Y.yong 2016年11月1日
 */
public class FaceFeatureVerify {
	private static final String VERSION = LoadPropers.getSingleInstance().getProperties("server").getProperty("face.arithmetic.version");
	private static final int FLOAT_SIZE = 4; // FLOAT大小，按字节
	private static int FEAT_RAW_DIM; // 裸特征维度
	/**
	 * Description: 映射曲线拐点
	 */
	private static float[] x;
	private static float[] y;
	/**
	 * Description: 比对分数映射折线
	 */
	private static float[] k; // 斜率
	private static float[] b;

	static {
        if (VERSION.equals("1.5")) {
			FEAT_RAW_DIM = 256;
			x = new float[] {-1f, 0.54f, 0.59f, 0.63f, 0.66f, 0.709f, 1f};
			k = new float[] {38.96f, 200.0f, 374.99f, 166.67f, 102.04f, 17.18f};
			b = new float[] {38.96f, -48.0f, 151.24f, -20.0f, 22.66f, 82.818f};
        }else if (VERSION.equals("5.6.2")) {
			FEAT_RAW_DIM = 128;
			x = new float[] { -1f, 0.133351f, 0.237756f, 0.317707f, 0.38652f, 0.451677f, 0.535134f, 1f };
			y = new float[] { 0f, 0.40000000596f, 0.5f, 0.600000023842f, 0.699999988079f, 0.800000011921f, 0.899999976158f, 1f };

			k = new float[x.length - 1]; // 斜率
			b = new float[x.length - 1]; // 截距

			/**
			 * Description: 求折线的斜率和截距 斜率：k = (y1-y2) / (x1-x2) 截距：b = y1 - kx1
			 */
			for (int i = 0; i < k.length; i++) {
				k[i] = (y[i] - y[i + 1]) / (x[i] - x[i + 1]);
				b[i] = y[i] - k[i] * x[i];
			}
		} else if (VERSION.equals("1.3")) {
            FEAT_RAW_DIM = 256;
            x = new float[] {-1f, 0.26f, 0.3f, 0.4f, 0.48f, 0.55f, 0.61f, 0.69f, 1f};
            k = new float[] {47.619f, 125.0f, 100.0f, 62.5f, 71.428f, 83.333f, 62.5f, 16.129f, 1f};
            b = new float[] {47.619f, 27.5f, 35.0f, 50.0f, 45.714f, 39.16667f, 51.875f, 83.871f, 1f};
        }
	}

	/**
	 * 特征比对算法
	 *
	 * @param pFaceFeat1
	 *            人脸特征1
	 * @param offset1
	 *            人脸特征1比对开始index
	 * @param pFaceFeat2
	 *            人脸特征2
	 * @param offset2
	 *            人脸特征2比对开始index
	 * @return 相似度
	 */
	public static float verify(byte[] pFaceFeat1, int offset1,
							   byte[] pFaceFeat2, int offset2) {
		if (null == pFaceFeat1 || null == pFaceFeat2
				|| pFaceFeat1.length < FEAT_RAW_DIM * FLOAT_SIZE
				|| pFaceFeat2.length < FEAT_RAW_DIM * FLOAT_SIZE) {
            return 0.0f;
		}
		// Description: 特征比对分数
		float ftScore = .0f;
		float feat_float1 = 0;
		float feat_float2 = 0;

		for (int i = 0; i < FEAT_RAW_DIM; ++i) {
			feat_float1 = byte2float(pFaceFeat1, i * FLOAT_SIZE + offset1);
			feat_float2 = byte2float(pFaceFeat2, i * FLOAT_SIZE + offset2);
			ftScore += feat_float1 * feat_float2;
		}

		for (int i = 1; i < x.length; i++) {
			if (ftScore <= x[i]) {
				ftScore = ftScore * k[i - 1] + b[i - 1];
				break;
			}
		}

		if (VERSION.equals("1.5") || VERSION.equals("1.3")) {
			return ftScore;
		} else {
			ftScore = ftScore <= 1.001 ? ftScore : 0;
			if (ftScore > 1) {
				ftScore = 1;
			}
			return ftScore * 100;
		}
	}

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
