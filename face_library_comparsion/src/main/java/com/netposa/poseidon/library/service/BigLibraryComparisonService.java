package com.netposa.poseidon.library.service;

import com.netposa.poseidon.library.bean.CacheBean;
import com.netposa.poseidon.library.init.LoadPropers;
import com.netposa.poseidon.library.rpc.ImgScore;
import com.netposa.poseidon.library.rpc.QueryRequest;
import com.netposa.poseidon.library.rpc.RecordInfo;
import com.netposa.poseidon.library.util.FaceFeatureVerify;
import com.netposa.poseidon.library.util.FaceFeatureVerifyDg;
import com.netposa.poseidon.library.util.HbaseUtil;
import com.netposa.poseidon.library.util.SearchImgResultFromHbase;
import org.apache.ignite.*;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.*;
import org.apache.ignite.configuration.CacheConfiguration;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by dell on 2017/6/28.
 */
public class BigLibraryComparisonService {
    private static final Logger LOG = LoggerFactory.getLogger(BigLibraryComparisonService.class);
    private static String igniteName = "Netposa";
    private static Ignite ignite = Ignition.ignite(igniteName);

    public static List<RecordInfo> executeQuery(QueryRequest request) {
        List<RecordInfo> recordInfos = new ArrayList<>();
        LOG.info("bigLibrary executeQuery start...");
        long time1 = System.currentTimeMillis();
        IgniteCompute compute = ignite.compute(ignite.cluster().forServers());
        long time2 = System.currentTimeMillis();
        LOG.info("bigLibrary compute start...used time is {} ms.", time2 - time1);
        try {
            PriorityQueue<RecordInfo> resultQueue = compute.execute(new BigLibraryComparisonTask(request.getRCount()), request);
            long time3 = System.currentTimeMillis();
            LOG.info("bigLibrary compute finish...compute result size is = {},used time is {} ms.", resultQueue.size(), time3 - time2);
            while (resultQueue.size() > request.getRCount()) {
                resultQueue.poll();
            }
            resultQueue = SearchImgResultFromHbase.execute(request, resultQueue);
            long time4 = System.currentTimeMillis();
            LOG.info("bigLibrary hbase query finish...hbase query result size is {},used time is {} ms.", resultQueue.size(), time4 - time3);
            int x = resultQueue.size();
            for (int i = 0; i < x; i++) {
                recordInfos.add(resultQueue.poll());
            }
            Collections.reverse(recordInfos);
            LOG.info("bigLibrary executeQuery finish...used time is {} ms.", System.currentTimeMillis() - time4);
        }catch (IgniteClientDisconnectedException e) {
            LOG.error(e.getMessage(),e);
            //断线重连
            e.reconnectFuture().get();
            //激活持久化功能
            ignite.active(true);
        }
        return recordInfos;
    }
}

class BigLibraryComparisonTask extends ComputeTaskAdapter<QueryRequest, PriorityQueue<RecordInfo>> implements Serializable {
    private static Logger LOG = LoggerFactory.getLogger(BigLibraryComparisonTask.class);
    private int rCount;

    public BigLibraryComparisonTask(int rCount) {
        this.rCount = rCount;
    }

    @Nullable
    @Override
    public Map<? extends ComputeJob, ClusterNode> map(List<ClusterNode> list, @Nullable QueryRequest request) throws IgniteException {
        Map<ComputeJob, ClusterNode> result = new java.util.HashMap<>();
        for (ClusterNode node : list) {
            for (String libId : request.getLibraryIds()) {
                List<RegionInfo> regionInfos = LoadComparisonData.getRegionInfo(libId);
                for (RegionInfo info : regionInfos) {
                    String str = new String(info.getStartKey());
                    String tableName = info.getTableName() + "_" + ((str == null || str.equals("")) ? "00" : str);
                    result.put(new BigLibraryComparisonJob(request, tableName), node);
                }
            }
        }
        return result;
    }

    @Nullable
    @Override
    public PriorityQueue<RecordInfo> reduce(List<ComputeJobResult> list) throws IgniteException {
        long startTime = System.currentTimeMillis();
        PriorityQueue<RecordInfo> result = new PriorityQueue(rCount + 1);
        for (ComputeJobResult com : list) {
            for (RecordInfo info : com.<PriorityQueue<RecordInfo>>getData()) {
                result.offer(info);
                if (result.size() > rCount) {
                    result.poll();
                }
            }
        }
        LOG.info("client reduce action used time is {}", System.currentTimeMillis() - startTime);
        return result;
    }
}

class BigLibraryComparisonJob extends ComputeJobAdapter implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(BigLibraryComparisonJob.class);
    private static String igniteName = "Netposa";
    private static IgniteCache<String, String> igniteCache = Ignition.ignite(igniteName).cache(igniteName + "_LIBRARY_GLOBAL");
    private static final String algorithm = igniteCache.get("ALGORITHM");
    private static final int versionSize = Integer.parseInt(igniteCache.get("VERSIONSIZE"));
    private QueryRequest request;
    private String tableName;

    public BigLibraryComparisonJob(QueryRequest request, String tableName) {
        this.request = request;
        this.tableName = tableName;
    }

    @Override
    public PriorityQueue<RecordInfo> execute() throws IgniteException {
        long time = System.currentTimeMillis();
        final byte[] feature = request.getFeature();
        final int rCount = request.getRCount();
        final int similarity = request.getSimilarity();
        PriorityQueue<RecordInfo> queue = new PriorityQueue<>(rCount + 1);
        IgniteCache<String, CacheBean> cache = Ignition.ignite(igniteName).cache(tableName);
        if (cache == null) {
            LOG.error(String.format("cache [%s] is not found!", tableName));
            return queue;
        }
        Iterable<Cache.Entry<String, CacheBean>> iter = cache.localEntries(CachePeekMode.PRIMARY);
        long time1 = System.currentTimeMillis();
        LOG.info("comparison query task start! prepare used time {}ms.", time1 - time);
        int count = 0;
        float highestScore = -1;
        if ("netposa".equalsIgnoreCase(algorithm)) {
            for (final Cache.Entry<String, CacheBean> entry : iter) {
                count++;
                highestScore = -1;
                for (Map.Entry<String, byte[]> e : entry.getValue().getFeatures().entrySet()) {
                    float score = FaceFeatureVerify.verify(feature, versionSize, e.getValue(), versionSize);
                    if (score > similarity && score > highestScore) {
                        highestScore = score;
                    }
                }
                if (highestScore >= similarity) {
                    RecordInfo recordinfo = new RecordInfo();
                    recordinfo.setId(entry.getKey());
                    recordinfo.setHighestScore(highestScore);
                    queue.offer(recordinfo);
                    if (queue.size() > rCount) {
                        queue.poll();
                    }
                }
            }
        } else {
            for (final Cache.Entry<String, CacheBean> entry : iter) {
                count++;
                highestScore = -1;
                for (Map.Entry<String, byte[]> e : entry.getValue().getFeatures().entrySet()) {
                    float score = FaceFeatureVerifyDg.verify(feature, versionSize, e.getValue(), versionSize);
                    if (score > similarity && score > highestScore) {
                        highestScore = score;
                    }
                }
                if (highestScore >= similarity) {
                    RecordInfo recordinfo = new RecordInfo();
                    recordinfo.setId(entry.getKey());
                    recordinfo.setHighestScore(highestScore);
                    queue.offer(recordinfo);
                    if (queue.size() > rCount) {
                        queue.poll();
                    }
                }
            }
        }
        LOG.info("cache is {},query task finish! data size is {},used time {}ms.", tableName, count, System.currentTimeMillis() - time1);
        return queue;
    }
}
