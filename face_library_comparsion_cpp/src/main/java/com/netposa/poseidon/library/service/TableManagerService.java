package com.netposa.poseidon.library.service;

import com.netposa.poseidon.library.bean.ConnectionManagerKey;
import com.netposa.poseidon.library.bean.TableStatusCode;
import com.netposa.poseidon.library.util.FileMemUtil;
import com.netposa.poseidon.library.util.LoadPropers;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class TableManagerService extends Thread {
    private Properties properties = LoadPropers.getSingleInstance().getProperties("server");
    // 每次获取心跳间隔
    private int heartBeatTimeInterval = Integer.parseInt(properties.getProperty("heart.beat.time.interval"));
    private int clusterSize = properties.getProperty("cluster.list").split(",").length;
    private String mataDataFilePath = properties.getProperty("metadata.file.path", "/home/");
    private Logger logger = Logger.getLogger(TableManagerService.class);
    private Map<String, Map<String, TableStatusCode>> table = new HashMap<>();
    private boolean stopped = false;

    private int lastHashCode;

    // 单例模式, 数据只保存一份
    private TableManagerService() {}
    private static class LazyHolder {
        private static final TableManagerService instance = new TableManagerService();
    }
    public static final TableManagerService getInstance() {
        return TableManagerService.LazyHolder.instance;
    }

    @Override
    public void run() {
        while (!stopped) {
            int tmpHashCode = table.hashCode();
            if (tmpHashCode != this.lastHashCode) {
                while (!FileMemUtil.memDump2File(table, mataDataFilePath)) {
                    logger.error("fail to write local mata data file, try after 3s");
                    try {
                        TimeUnit.SECONDS.sleep(3);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                this.lastHashCode = tmpHashCode;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(heartBeatTimeInterval);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public Map<String, Map<String, TableStatusCode>> getTable() {
        return table;
    }

    public void setTable(Map<String, Map<String, TableStatusCode>> table) {
        this.table = table;
    }

    public void updateTable(String key, String ip, TableStatusCode code) {
        if (!table.containsKey(key)) {
            table.put(key, new HashMap<String, TableStatusCode>());
        }
        table.get(key).put(ip, code);
    }

    public void deleteTable(String key) {
        if (table.containsKey(key)) {
            table.remove(key);
        }
    }
}
