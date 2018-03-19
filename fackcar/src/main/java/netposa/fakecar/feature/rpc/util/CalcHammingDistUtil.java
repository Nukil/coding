package netposa.fakecar.feature.rpc.util;

public class CalcHammingDistUtil {
	
	static final int VERSION_SIZE = 16;
	static final int BYTES_SIZE = 16;
	static final int BUFF_BEGIN_OFFSET = VERSION_SIZE + BYTES_SIZE;
	static final int FEATURE_SIZE = 256;
	static final int FEATURE_BIT_SIZE = FEATURE_SIZE * 8;
	static final int[] BIT_FLAGS = {0x01,0x02,0x04,0x08,0x10,0x20,0x40,0x80};
	static final int HAMMING_DIST_TH = 612;
	
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

	        if ((
	                (featureBuff1[iByte] & BIT_FLAGS[iBit])
	                 ^ (featureBuff2[iByte] & BIT_FLAGS[iBit])
	             ) != 0x00)
	        {
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

	        if ((
	                (featureBuff1[iByte] & BIT_FLAGS[iBit])
	                 ^ (featureBuff2[iByte] & BIT_FLAGS[iBit])
	             ) != 0x00)
	        {
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
	
    public static int getDistenceDic(float distence) {
    	int i = 0;
    	double dis = 100;
    	while (dis>=distence && i<=256*8) {
    		i++;
    		dis = 100*Math.exp(-1.0*i/HAMMING_DIST_TH);
    	}
    	return i-1;
    }
}

