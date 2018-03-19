package com.netposa.poseidon.library.util;

import com.google.common.collect.Lists;
import com.netposa.poseidon.library.init.LoadPropers;
import com.netposa.poseidon.library.rpc.ImgScore;
import com.netposa.poseidon.library.rpc.QueryRequest;
import com.netposa.poseidon.library.rpc.RecordInfo;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Y.yong on 2017/11/9.
 */
public class SearchImgResultFromHbase {
    private static final Logger LOG = LoggerFactory.getLogger(SearchImgResultFromHbase.class);
    private static int processSize = Runtime.getRuntime().availableProcessors();
    private static ExecutorService executorPool = Executors.newFixedThreadPool(processSize);
    private final static int regionNum = Integer.parseInt(LoadPropers.getProperties().getProperty("hbase.split.region.num", "50").trim());
    private final static int version = Integer.parseInt(LoadPropers.getProperties().getProperty("face.version.size", "16").trim());
    public static PriorityQueue<RecordInfo> execute(QueryRequest request, PriorityQueue<RecordInfo> recordInfos) {
        PriorityQueue<RecordInfo>  responseInfo = new PriorityQueue<>(request.rCount + 1);
        if (recordInfos == null || recordInfos.size() == 0) {
            return responseInfo;
        }
        final byte[] feature = request.getFeature();
        final short similar = request.getSimilarity();
        final Connection conn = HbaseUtil.getConn();
        try {
            for (String tableName : request.getLibraryIds()) {
                final Table table = conn.getTable(TableName.valueOf(tableName));
                List<Future<RecordInfo>> futureList = new ArrayList<>();
                for (RecordInfo info1 : recordInfos) {
                    final RecordInfo info = info1;
                    Future<RecordInfo> future = executorPool.submit(new Callable<RecordInfo>() {
                        @Override
                        public RecordInfo call() {
                            Get get = new Get(HashAlgorithm.hash(info.getId(), regionNum).getBytes());
                            Result rsResult = null;
                            try {
                                rsResult = table.get(get);
                            } catch (IOException e) {
                                LOG.error(e.getMessage(), e);
                            }
                            RecordInfo recordInfo = new RecordInfo();
                            if (rsResult != null && !rsResult.isEmpty()) {
                                List<ImgScore> imgScores = Lists.newArrayList();
                                float highScore = 0;
                                String highId = "";
                                for (Cell cell : rsResult.listCells()) {
                                    if ("ext".equals(Bytes.toString(CellUtil.cloneQualifier(cell)))) {
                                        recordInfo.setExt(Bytes.toString(CellUtil.cloneValue(cell)));
                                    } else {
                                        String imgId = Bytes.toString(CellUtil.cloneQualifier(cell));
                                        float score = FaceFeatureVerify.verify(feature, version, CellUtil.cloneValue(cell), version);
                                        if (score > highScore) {
                                            highScore = score;
                                            highId = imgId;
                                        }
                                        ImgScore imgScore = new ImgScore(imgId, score);
                                        imgScores.add(imgScore);
                                    }
                                }
                                if (highScore >= similar) {
                                    recordInfo.setId(Bytes.toString(rsResult.getRow()).substring(2));
                                    recordInfo.setHighestScore(highScore);
                                    recordInfo.setHighestScoreImgId(highId);
                                    recordInfo.setImgScore(imgScores);
                                    return recordInfo;
                                }
                            }
                            return recordInfo;
                        }
                    });
                    futureList.add(future);
                }
                for (Future<RecordInfo> info : futureList) {
                    RecordInfo recordTmp = info.get();
                    if (recordTmp.getHighestScore() >= similar) {
                        responseInfo.offer(recordTmp);
                        if(responseInfo.size() > request.getRCount()){
                            responseInfo.poll();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return responseInfo;
    }
}
