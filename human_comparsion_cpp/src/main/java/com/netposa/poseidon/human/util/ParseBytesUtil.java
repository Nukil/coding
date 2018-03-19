package com.netposa.poseidon.human.util;

import com.netposa.poseidon.human.bean.FetcherDataBean;
import com.netposa.poseidon.human.bean.HumanFeature;
import org.apache.log4j.Logger;

import java.nio.charset.Charset;
import java.util.ArrayList;

public class ParseBytesUtil {
    private Logger logger = Logger.getLogger(ParseBytesUtil.class);
    //===============需要解析的所有字段 声明==============================
    private byte[] JSON_START = "{".getBytes();
    private byte[] JSON_END = "}".getBytes();
    private byte[] JSON_STOP = ",".getBytes();
    private byte[] EMPTY_CHAR = " ".getBytes();
    private byte[] END_ARR = "]".getBytes();
    private byte[] JSON_CHAR = ":".getBytes();
    private byte[] JSON_MARK = "\"".getBytes();
    private Charset charset = Charset.forName("UTF-8");

    //pcc
    byte[] _1_1 = "recordId".getBytes(charset);
    byte[] _1_2 = "sourceId".getBytes(charset);
    byte[] _1_3 = "absTime".getBytes(charset);

    /**
     * 在sour中查找target 的位置
     * 返回位置下标
     */
    int indexOf(byte[] sour, byte[] target) {
        if (target.length == 0) {
            return 0;
        }
        if (sour.length < target.length) {
            return -1;
        }
        int i = 0;
        int j = 0;
        boolean continued = false;
        while (i < sour.length - target.length + 1 && !continued) {
            j = 0;
            continued = true;
            while (j < target.length && continued) {
                if (sour[i + j] != target[j]) {
                    continued = false;
                }
                j = j + 1;
            }
            i = i + 1;
        }
        if (continued) {
            return i - 1;
        } else {
            return -1;
        }
    }

    /**
     * 从sour中解析出 target的值
     * 返回类型Array[Byte]
     * index 在数据源中的起始位置
     */
    private byte[] getValueToBytes(byte[] sour, byte[] target) {
        int index = indexOf(sour, target);
        if (index < 0) {
            return ("".getBytes());
        }
        index += target.length;
        //查找开始的位置
        int i = index;
        boolean Next = true;
        while (i < sour.length && Next) {
            if (sour[i] == JSON_CHAR[0] || sour[i] == EMPTY_CHAR[0] || sour[i] == JSON_MARK[0]) {
                i = i + 1;
            } else {
                Next = false;
            }
        }
        index = i;
        //查找结束位置
        Next = true;
        int tmpTuples = 0;
        byte startChar = sour[i];
        while (i < sour.length && Next) {
            if (startChar == JSON_START[0]) {
                //返回结构体
                if (sour[i] == JSON_START[0]) {
                    tmpTuples += 1;
                }
                if (sour[i] == JSON_END[0]) {
                    tmpTuples -= 1;
                }
                if (tmpTuples == 0) {
                    Next = false;
                }
                i += 1;
            } else {
                if (sour[i] == JSON_END[0] || sour[i] == JSON_STOP[0] || sour[i] == JSON_MARK[0]) {
                    Next = false;
                } else {
                    i += 1;
                }
            }
        }
        byte[] re = new byte[i - index];
        System.arraycopy(sour, index, re, 0, i - index);
        return re;
    }

    /**
     * 数据组装
     */
    HumanFeature dataAssembleToPB(byte[] msg, int featureSize) {
        HumanFeature remsg = new HumanFeature();
        try {
            //(offset,recordId, cameraId, absTime)
            FetcherDataBean data = fetcherData(msg);
            if (null != data) {
                if (data.getOffset() >= 0 && msg.length >= data.getOffset() + featureSize + 4) {
                    byte[] feature = new byte[msg.length - data.getOffset() - 4];
                    System.arraycopy(msg, data.getOffset() + 4, feature, 0, msg.length - data.getOffset() - 4 );
                    remsg.setFeature(feature);
                    remsg.setJlbh(data.getRecordId());
                    remsg.setCameraId(data.getCameraId());
                    remsg.setGatherTime(data.getGatherTime());
                } else {
                    logger.error("recordId=" + data.getRecordId() + ",cameraId=" + data.getCameraId()+ ",absTime=" + data.getGatherTime() + ",offset=" + data.getOffset() + ",dataSize=" + featureSize + ", msg.length=" + msg.length + " this record has no feature ...");
                    logger.error(new String(msg));
                }
            }
        } catch(Exception e) {
            logger.error("fetcher data exception", e);
        }
        return remsg;
    }

    public HumanFeature dataForETL2PB(byte[] rowmessage, int featureSize) {
        if (rowmessage != null) {
            //数据组装
            return dataAssembleToPB(rowmessage, featureSize);
        } else {
            logger.warn("dataForETL2PB info is null");
            return null;
        }
    }

    private FetcherDataBean fetcherData(byte[] src) {
        int offset = HashAlgorithm.bytesToInt2(src,0);
        String recordId = "";
        String cameraId = "";
        String absTime = "";

        ByteArrayJsonParseUtils jsonUtils = new ByteArrayJsonParseUtils();

        // 解析第一级 recordId
        ArrayList<int[]> recordId_list = jsonUtils.parseJsonBytesByKey(src, _1_1, 0, src.length);
        if (recordId_list != null && recordId_list.size() > 0) {
            int[] recordId_offsets = recordId_list.get(0);
            recordId = new String(src, recordId_offsets[0] + 1, recordId_offsets[1] - recordId_offsets[0] - 3);
        }

        // 解析第一级 cameraId
        ArrayList<int[]> cameraId_list = jsonUtils.parseJsonBytesByKey(src, _1_2, 0, src.length);
        if (cameraId_list != null && cameraId_list.size() > 0) {
            int[] cameraId_offsets = cameraId_list.get(0);
            //      cameraId = new String(src, cameraId_offsets(0), cameraId_offsets(1) - cameraId_offsets(0) - 1)
            cameraId = new String(src, cameraId_offsets[0] + 1, cameraId_offsets[1] - cameraId_offsets[0] - 3);
        }

        // 解析第一级 absTime
        ArrayList<int[]> absTime_list = jsonUtils.parseJsonBytesByKey(src, _1_3, 0, src.length);
        if (absTime_list != null && absTime_list.size() > 0) {
            int[] absTime_offsets = absTime_list.get(0);
            absTime = new String(src, absTime_offsets[0], absTime_offsets[1] - absTime_offsets[0]-1);
            absTime = absTime.replaceAll("-", "");
        }

        return new FetcherDataBean(offset, recordId, cameraId, absTime);
    }
}

