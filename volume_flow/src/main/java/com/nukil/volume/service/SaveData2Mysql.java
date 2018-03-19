package com.nukil.volume.service;
import com.nukil.volume.bean.HumanBean;
import com.nukil.volume.util.DBUtil;
import com.nukil.volume.util.LoadPropers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class SaveData2Mysql extends Thread {
    private static boolean DEBUG = Boolean.parseBoolean(LoadPropers.getProperties().getProperty("service.process.debug.enable", "false").trim());
    private static String DATA_FILE_PATH = LoadPropers.getProperties().getProperty("save.data.file.path", "./data/");
    private static String HISTORY_DATA_FILE_PATH = LoadPropers.getProperties().getProperty("save.history.data.file.path", "./history_data/");
    private static Logger LOG = LoggerFactory.getLogger(SaveData2Mysql.class);
    private boolean STOPPED = false;
    private BlockingQueue<HumanBean> queue = new LinkedBlockingDeque<>();
    private static SaveData2Mysql instance = null;
    Connection conn = DBUtil.getConnection();
    private SaveData2Mysql() {
    }

    public static SaveData2Mysql getInstance() {
        if (null == instance) {
            synchronized (SaveData2Mysql.class) {
                if (null == instance) {
                    instance = new SaveData2Mysql();
                }
            }
        }
        return instance;
    }

    @Override
    public void run() {
        LOG.info("save data to Mysql thread start........");
        while (!STOPPED) {
            while (queue.size() > 0) {
                PreparedStatement ps = null;
                ResultSet res = null;
                try {
                    if (null == conn || conn.isClosed()) {
                        conn = DBUtil.getConnection();
                        while (null == conn || conn.isClosed()) {
                            LOG.error("mysql connection is closed, sleep 5s then reget");
                            Thread.sleep(5000);
                            conn = DBUtil.getConnection();
                        }
                    }
                    for (int i = 0; i < queue.size(); ++i) {
                        HumanBean hb = queue.take();
                        ps = DBUtil.getPreparedStatement(conn, hb.getSelectSql());
                        LOG.info(String.format("====execute sql is %s====", hb.getSelectSql()));
                        res = ps.executeQuery();
                        if (res.next()) {
                            int dayCount = Integer.parseInt(res.getString("day_count"));
                            int hourCount = Integer.parseInt(res.getString(String.format("hour%s", hb.getHour())));
                            LOG.info(String.format("====execute sql is %s====", hb.getUpdateSql(dayCount + hb.getCount(), hourCount + hb.getCount())));
                            ps = DBUtil.getPreparedStatement(conn, hb.getUpdateSql(dayCount + hb.getCount(), hourCount + hb.getCount()));
                            int lines = ps.executeUpdate();
                            if (DEBUG) {
                                LOG.info(String.format("update affect lines is %d", lines));
                            }
                        } else {
                            ps = DBUtil.getPreparedStatement(conn, hb.getInsertSql());
                            LOG.info(String.format("====execute sql is %s====", hb.getInsertSql()));
                            int lines = ps.executeUpdate();
                            if (DEBUG) {
                                LOG.info(String.format("Insert affect lines is %d", lines));
                            }
                        }
                        File file = new File(new StringBuilder(DATA_FILE_PATH).append(hb.getDate()).append(",")
                                .append(hb.getHour()).append(",")
                                .append(hb.getAreaID()).append(",")
                                .append(hb.getCameraID()).append(",")
                                .append(hb.getCount()).append(",")
                                .append(hb.getTableName()).append(",")
                                .append(hb.getUuid()).toString());
                        if (file.exists()) {
                            while (!file.delete()) {
                                LOG.error(String.format("failed to delete file : %s", file.getName()));
                                Thread.sleep(3000);
                            }
                            if (DEBUG) {
                                LOG.info(String.format("delete file : %s", file.getName()));
                            }
                        } else {
                            File fileHistory = new File(new StringBuilder(HISTORY_DATA_FILE_PATH).append(hb.getDate()).append(",")
                                    .append(hb.getHour()).append(",")
                                    .append(hb.getAreaID()).append(",")
                                    .append(hb.getCameraID()).append(",")
                                    .append(hb.getCount()).append(",")
                                    .append(hb.getTableName()).append(",")
                                    .append(hb.getUuid()).toString());
                            if (fileHistory.exists()) {
                                while (!fileHistory.delete()) {
                                    LOG.error(String.format("failed to delete file : %s", fileHistory.getName()));
                                    Thread.sleep(3000);
                                }
                                if (DEBUG) {
                                    LOG.info(String.format("delete file : %s", fileHistory.getName()));
                                }
                            }
                        }
                    }
                    //数据库插入正常，进行数据补录
                    File historyDataDir = new File(HISTORY_DATA_FILE_PATH);
                    File historyFiles[] = historyDataDir.listFiles();
                    if (null != historyFiles && historyFiles.length != 0) {
                        for (File historyFile : historyFiles) {
                            String filename = historyFile.getName();
                            String split[] = filename.split(",");
                            HumanBean hb = new HumanBean(split[0], split[1], split[2], split[3], Integer.parseInt(split[4]), split[5], split[6]);
                            queue.add(hb);
                            LOG.info(String.format("send history data to queue, file is %s", hb.toString()));
                        }
                    }
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                } finally {
                    if (null != res) {
                        DBUtil.closeResultSet(res);
                    }
                    if (null != ps) {
                        DBUtil.closeStatement(ps);
                    }
                }
            }
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public void addTask(HumanBean hb) {
        queue.add(hb);
    }

    public void shutDown() {
        STOPPED = true;
    }
}
