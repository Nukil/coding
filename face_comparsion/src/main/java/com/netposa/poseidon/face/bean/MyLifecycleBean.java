package com.netposa.poseidon.face.bean;

import com.netposa.poseidon.face.init.LoadPropers;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lifecycle.LifecycleBean;
import org.apache.ignite.lifecycle.LifecycleEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;

public class MyLifecycleBean implements LifecycleBean {
    private static CacheConfiguration<String, CacheFaceFeature> CACHE_CFG = new CacheConfiguration<>();
    static {
        // 设置数据缓存策略
        CACHE_CFG.setCacheMode(CacheMode.PARTITIONED);
        // 设置动态缓存,新加入的数据会自动分散在各个server
        CACHE_CFG.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
    }
    @Override
    public void onLifecycleEvent(LifecycleEventType evt) throws IgniteException {
        Logger log = LoggerFactory.getLogger(MyLifecycleBean.class);
        String startDate = LoadPropers.getProperties().getProperty("start.time");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        try {
            long start = new SimpleDateFormat("yyyy-MM-dd").parse(startDate).getTime();
            int duration = Integer.parseInt(LoadPropers.getProperties().getProperty("duration.time"));
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, (-1) * duration);
            long startTime = cal.getTimeInMillis() > start ? cal.getTimeInMillis() : start;
            cal.setTimeInMillis(System.currentTimeMillis());
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.add(Calendar.DATE, 1);

            String igniteName = LoadPropers.getProperties().getProperty("ignite.name");
            if (evt == LifecycleEventType.AFTER_NODE_START) {
                Collection<String> cacheNames = Ignition.ignite(igniteName).cacheNames();
                for (String cacheName : cacheNames) {
                    if (cacheName.contains("Netposa") && cacheName.contains("FACE")) {
                        Ignition.ignite(igniteName).cache(cacheName).destroy();
                        log.info(String.format("destroy cache [%s]", cacheName));
                    }
                }
                while (cal.getTimeInMillis() > startTime) {
                    cal.add(Calendar.DATE, -1);
                    String cacheName = igniteName + "_FACE_" + sdf.format(new java.util.Date(cal.getTimeInMillis()));
                    CACHE_CFG.setName(cacheName);
                    Ignition.ignite(igniteName).createCache(CACHE_CFG);
                    log.info(String.format("create cache, cache name is : %s", cacheName));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}