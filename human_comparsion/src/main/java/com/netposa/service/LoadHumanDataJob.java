package com.netposa.service;

import com.netposa.bean.CacheFaceFeature;
import com.netposa.init.LoadPropers;
import com.netposa.util.HbaseUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.compute.ComputeJobAdapter;
import org.apache.ignite.configuration.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class LoadHumanDataJob extends ComputeJobAdapter implements Serializable {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = LoggerFactory.getLogger(LoadHumanDataJob.class);
    private static CacheConfiguration<String, CacheFaceFeature> cacheCfg = new CacheConfiguration<>();

    private static byte[] FAMILY = "cf".getBytes();
    private static byte[] CAMERAID = "cameraId".getBytes();
    private static byte[] JLBH = "logNum".getBytes();
    private static byte[] FEATURE = "feature".getBytes();
    private static byte[] GATHERTIME = "gatherTime".getBytes();
    private String hostName;
    private String startKey;
    private String IGNITE_NAME;

    static {
        // 设置数据缓存策略
        cacheCfg.setCacheMode(CacheMode.PARTITIONED);
        // 设置动态缓存,新加入的数据会自动分散在各个server
        cacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
    }

    public LoadHumanDataJob(String hostName, String startKey, String igniteName) {
        this.hostName = hostName;
        this.startKey = startKey;
        this.IGNITE_NAME = igniteName;
    }

    @Override
    public Integer execute() throws IgniteException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        int size_sum = 0;
        try {
            String localhost = InetAddress.getLocalHost().getCanonicalHostName();
            if (hostName.equals(localhost)) {
                LOG.info("load human data start,startKey =>[" + startKey + "]! localhost is :" + localhost);
                long time1 = System.currentTimeMillis();
                Connection conn = HbaseUtil.getConn();
                IgniteCache<String, String> cache = Ignition.ignite(IGNITE_NAME).cache(IGNITE_NAME + "_HUMAN_GLOBAL");
                Table table = conn.getTable(TableName.valueOf(cache.get("TABLENAME")));
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, (-1) * Integer.parseInt(LoadPropers.getProperties().getProperty("duration.time", "30").trim()));
                long startTime = cal.getTimeInMillis();
                cal.setTimeInMillis(System.currentTimeMillis() / 86400000 * 86400000 + 86400000);
                while (cal.getTimeInMillis() > startTime) {
                    int size = 0;
                    long time2 = System.currentTimeMillis();
                    String endRow = startKey + cal.getTimeInMillis();
                    cal.add(Calendar.DATE, -1);
                    String current_date = sdf.format(new java.util.Date(cal.getTimeInMillis()));
                    cacheCfg.setName(IGNITE_NAME + "_HUMAN_" + current_date);
                    IgniteCache<String, CacheFaceFeature> igniteCache = Ignition.ignite(IGNITE_NAME).getOrCreateCache(cacheCfg);
                    String startRow = startKey + cal.getTimeInMillis();
                    Scan scan = new Scan();
                    scan.setStartRow(startRow.getBytes());
                    scan.setStopRow(endRow.getBytes());
                    scan.setCaching(10000);
                    ResultScanner scanner = table.getScanner(scan);
                    for (Result result : scanner) {
                        if (result != null
                                && result.getValue(FAMILY, JLBH) != null
                                && result.getValue(FAMILY, GATHERTIME) != null
                                && result.getValue(FAMILY, CAMERAID) != null
                                && result.getValue(FAMILY, FEATURE) != null) {
                            size++;
                            igniteCache.put(new String(result.getValue(FAMILY, JLBH)),
                                    new CacheFaceFeature(result.getValue(FAMILY, GATHERTIME),
                                            result.getValue(FAMILY, CAMERAID),
                                            result.getValue(FAMILY, FEATURE)));
                        }
                    }
                    size_sum += size;
                    long time3 = System.currentTimeMillis();
                    LOG.info("current_date,startRow,endRow =>> [" + cacheCfg.getName() + "," + startRow + "," + endRow + "] ,=>load " + size
                            + " human data and put cache used time  ====>>> " + (time3 - time2) + "ms!");
                    if (scanner != null) {
                        scanner.close();
                    }
                }
                LOG.info("===LOCALHOST=[" + localhost + "],load " + size_sum + " human data and put cache used time  ====>>> " + (System.currentTimeMillis() - time1) + "ms!");
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return size_sum;
    }
}
