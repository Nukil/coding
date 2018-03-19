package com.nukil.parse.util.impl;

import com.nukil.parse.util.bean.HumanBean;
import com.nukil.parse.util.bean.HumanParams;
import com.nukil.parse.util.inter.HumanDataETLInterface;
import com.nukil.parse.util.util.ParseBytesUtils;

import java.util.ArrayList;
import java.util.List;

public class HumanDataETL implements HumanDataETLInterface {
    @Override
    public HumanBean dataAnalyze(byte[] message, int size, List<HumanParams> params) {
        ParseBytesUtils parseBytesUtils = new ParseBytesUtils();
        return parseBytesUtils.humanDataForETL2PB(message, size, params);
    }

    @Override
    public List<HumanBean> dataBatchAnalyze(List<byte[]> messages, int size, List<HumanParams> params) {
        List<HumanBean> faceBeans = new ArrayList<>();
        for (byte[] message : messages) {
            faceBeans.add(dataAnalyze(message, size, params));
        }
        return faceBeans;
    }
}