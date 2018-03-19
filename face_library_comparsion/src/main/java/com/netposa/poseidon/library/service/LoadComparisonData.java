package com.netposa.poseidon.library.service;

import com.netposa.poseidon.library.bean.CacheBean;
import com.netposa.poseidon.library.util.HbaseUtil;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HRegionLocation;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.ignite.*;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobAdapter;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskAdapter;
import org.apache.ignite.configuration.CacheConfiguration;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dell on 2017/7/4.
 */
public class LoadComparisonData {
    private static final Logger log = LoggerFactory.getLogger(LoadComparisonData.class);
    static String igniteName = "Netposa";
    static IgniteCache<String, String> igniteCache = Ignition.ignite(igniteName).cache(igniteName + "_LIBRARY_GLOBAL");

  
    private static final String META_TABLE = igniteCache.get("TABLENAME");
    private static final String IGNITE_NAME = igniteCache.get("IGNITENAME");
    public static List<RegionInfo> getRegionInfo(String tableName) {
        List<RegionInfo> splitBeans = new ArrayList<>();
        try {
            RegionLocator regionLocator = HbaseUtil.getConn().getRegionLocator(TableName.valueOf(tableName.getBytes()));
            if (!regionLocator.getAllRegionLocations().isEmpty()) {
                for (HRegionLocation hRegionLocation : regionLocator.getAllRegionLocations()) {
                    RegionInfo bean = new RegionInfo(tableName, hRegionLocation.getHostname(), hRegionLocation.getRegionInfo().getStartKey(),hRegionLocation.getRegionInfo().getEndKey());
                    splitBeans.add(bean);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return splitBeans;
    }

    public static boolean loadData() {
    	log.info("free ignite old data start...... ");
        new FreeIgniteMemoey(IGNITE_NAME).freeCache();
        log.info("free ignite old data finish......");
        if (!HbaseUtil.tableIsExists(META_TABLE)) {
            return true;
        }
        Table table = null;
        try {
            Ignite ignite = Ignition.ignite(IGNITE_NAME);
            IgniteCompute compute = ignite.compute();
            Connection conn = HbaseUtil.getConn();
            table = conn.getTable(TableName.valueOf(META_TABLE));
            Scan scan = new Scan();
            ResultScanner scanner = table.getScanner(scan);
            for (Result rs : scanner) {
                String libraryId = new String(rs.getRow());
                long start = System.currentTimeMillis();
                try{
                    log.info("load bigLibrary ["+ libraryId + "] data start......");
                    int count = compute.execute(new BigLibraryDataTask(IGNITE_NAME), getRegionInfo(libraryId));
                    log.info("load bigLibrary [" + libraryId + "] data " + count + " finish,used time: " + (System.currentTimeMillis() - start) + "ms!================");
                }catch (IgniteClientDisconnectedException e) {
                    log.error(e.getMessage(),e);
                    //断线重连
                    e.reconnectFuture().get();
                    //激活持久化功能
                    ignite.active(true);
                }
            }
            return true;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                if (table != null) {
                    table.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return false;
    }
}

class BigLibraryDataTask extends ComputeTaskAdapter<List<RegionInfo>, Integer> implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(BigLibraryDataTask.class);
    private static IgniteCache<String, String> igniteCache = Ignition.ignite("Netposa").cache("Netposa_LIBRARY_GLOBAL");
    private static CacheConfiguration<String,CacheBean> cacheCfg = new CacheConfiguration<>();
    private String IGNITE_NAME;
    static {
        cacheCfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        cacheCfg.setMemoryPolicyName("MeM_RANDOM_2_LRU");
        cacheCfg.setOnheapCacheEnabled(false);
        cacheCfg.setCacheMode(CacheMode.PARTITIONED);
        cacheCfg.setRebalanceBatchSize(Integer.parseInt(igniteCache.get("rebalance.batch.size.bytes")));
    }
    BigLibraryDataTask(String igniteName) {
        this.IGNITE_NAME = igniteName;
    }

    @Nullable
    @Override
    public Map<? extends ComputeJob, ClusterNode> map(List<ClusterNode> list, @Nullable List<RegionInfo> regionInfos) throws IgniteException {
        Map<LoadLibraryDataJob, ClusterNode> map = new HashMap<>();
        try {
            for (ClusterNode node : list) {
                for (RegionInfo regionInfo : regionInfos) {
                    if (node.hostNames().contains(regionInfo.getHostName())) {
                        map.put(new LoadLibraryDataJob(regionInfo, cacheCfg, IGNITE_NAME), node);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return map;
    }

    @Nullable
    @Override
    public Integer reduce(List<ComputeJobResult> list) throws IgniteException {
        int sum = 0;
        for (ComputeJobResult res : list) {
            sum += res.<Integer>getData();
        }
        return sum;
    }
}

//class SmallLibraryDataTask extends ComputeTaskAdapter<List<RegionInfo>, Integer> implements Serializable {
//    private static CacheConfiguration<String, Map<String,CacheBean>> cacheCfg = new CacheConfiguration<>();
//    private String IGNITE_NAME;
//    static {
//        cacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
//        cacheCfg.setCacheMode(CacheMode.REPLICATED);
//    }
//
//    SmallLibraryDataTask(String igniteName) {
//        this.IGNITE_NAME = igniteName;
//    }
//
//    @Nullable
//    @Override
//    public Map<? extends ComputeJob, ClusterNode> map(List<ClusterNode> list, @Nullable List<RegionInfo> regionInfos) throws IgniteException {
//        Map<LoadLibraryDataJob, ClusterNode> map = new HashMap<>();
//        for (ClusterNode node : list) {
//            for (RegionInfo regionInfo : regionInfos) {
//                if (node.hostNames().contains(regionInfo.getHostName())) {
//                    map.put(new LoadLibraryDataJob(regionInfo, cacheCfg, IGNITE_NAME), node);
//                }
//            }
//        }
//        return map;
//    }
//
//    @Nullable
//    @Override
//    public Integer reduce(List<ComputeJobResult> list) throws IgniteException {
//        int sum = 0;
//        for (ComputeJobResult res : list) {
//            sum += res.<Integer>getData();
//        }
//        return sum;
//    }
//}

class LoadLibraryDataJob extends ComputeJobAdapter implements Serializable {
    private static Logger log = LoggerFactory.getLogger(LoadLibraryDataJob.class);
    private static IgniteCache<String, String> igniteCache = Ignition.ignite("Netposa").cache("Netposa_LIBRARY_GLOBAL");
    private static final String ZK_QUORUM = igniteCache.get("ZK_QUORUM");
    private static final String CLIENT_PORT = igniteCache.get("CLIENT_PORT");
    private CacheConfiguration<String,CacheBean> cacheCfg;
    private RegionInfo regionInfo;
    private String IGNITE_NAME;
    public LoadLibraryDataJob(RegionInfo regionInfo, CacheConfiguration<String,CacheBean> cacheCfg, String igniteName) {
        this.regionInfo = regionInfo;
        this.cacheCfg = cacheCfg;
        this.IGNITE_NAME = igniteName;
    }
    static{
        HbaseUtil.hConfiguration.set("hbase.zookeeper.quorum",ZK_QUORUM);
        HbaseUtil.hConfiguration.set("hbase.zookeeper.property.clientPort",CLIENT_PORT);
    }
    @Override
    public Integer execute() throws IgniteException {
        int sum = 0;
        String str = new String(regionInfo.getStartKey());
        if(str == null || str.equals("")){
            str = "00";
        }
        int preLength = str.length();
        cacheCfg.setName(regionInfo.getTableName() + "_" + str);
        IgniteCache<String,CacheBean> cache = Ignition.ignite(IGNITE_NAME).getOrCreateCache(cacheCfg);
        log.info("load library data start,region host is {},start key is {}",regionInfo.getHostName(),str);
        Table table = null;
        ResultScanner scanner = null;
        try {
            table = HbaseUtil.getConn().getTable(TableName.valueOf(regionInfo.getTableName()));
            Scan scan = new Scan();
            scan.setStartRow(regionInfo.getStartKey());
            scan.setStopRow(regionInfo.getEndKey());
            scanner = table.getScanner(scan);
            for (Result res : scanner) {
                sum += 1;
                CacheBean cacheBean = new CacheBean();
                Map<String, byte[]> map = new HashMap<>();
                for (Cell cell : res.listCells()) {
                    if ("ext".equals(Bytes.toString(CellUtil.cloneQualifier(cell)))) {
                        cacheBean.setExt(Bytes.toString(CellUtil.cloneValue(cell)));
                    } else {
                        map.put(Bytes.toString(CellUtil.cloneQualifier(cell)), CellUtil.cloneValue(cell));
                    }
                }
                cacheBean.setFeatures(map);
                cache.put(Bytes.toString(res.getRow()).substring(preLength), cacheBean);
            }

            log.info("load library data finish,region host is {},start key is {},load data count {}.",regionInfo.getHostName(),str,sum);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                if(scanner != null){
                    scanner.close();
                }
                if (table != null) {
                    table.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return sum;
    }
}

class RegionInfo {
    private String tableName;
    private String hostName;
    private byte[] startKey;
    private byte[] endKey;

    public RegionInfo() {
    }

    public RegionInfo(String tableName, String hostName, byte[] startKey, byte[] endKey) {
        this.tableName = tableName;
        this.hostName = hostName;
        this.startKey = startKey;
        this.endKey = endKey;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getHostName() {
        return hostName;
    }
    public byte[] getEndKey() {
        return endKey;
    }

    public void setEndKey(byte[] endKey) {
        this.endKey = endKey;
    }
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public byte[] getStartKey() {
        return startKey;
    }

    public void setStartKey(byte[] startKey) {
        this.startKey = startKey;
    }
}