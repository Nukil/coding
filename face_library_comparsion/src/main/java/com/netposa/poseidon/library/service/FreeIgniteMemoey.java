package com.netposa.poseidon.library.service;

import com.netposa.poseidon.library.util.HbaseUtil;
import org.apache.hadoop.hbase.HRegionLocation;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class FreeIgniteMemoey {
    private Logger log = LoggerFactory.getLogger(FreeIgniteMemoey.class);
    private String igniteName;
    public FreeIgniteMemoey(String igniteName) {
        this.igniteName = igniteName;
    }
    public void freeCache() {
        Collection<String> cacheNames = Ignition.ignite(igniteName).cacheNames();
        IgniteCache<String, String> cache = Ignition.ignite(igniteName).cache(igniteName + "_LIBRARY_GLOBAL");
        String metaTable = cache.get("TABLENAME");
        Connection conn = HbaseUtil.getConn();
        Scan scan = new Scan();
        try {
            Table table = conn.getTable(TableName.valueOf(metaTable));
            ResultScanner scanner = table.getScanner(scan);
            for (Result rs : scanner) {
                List<RegionInfo> regionList = LoadComparisonData.getRegionInfo(new String(rs.getRow()));
                for (RegionInfo region : regionList) {
                    String str = new String(region.getStartKey());
                    if(str == null || str.equals("")){
                        str = "00";
                    }
                    String cacheName = region.getTableName() + "_" + str;
                    if (cacheNames.contains(cacheName)) {
                        Ignition.ignite(igniteName).cache(cacheName).destroy();
                        log.info(String.format("destroy cache : %s", cacheName));
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
