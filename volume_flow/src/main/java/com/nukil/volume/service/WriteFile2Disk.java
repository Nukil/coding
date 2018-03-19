package com.nukil.volume.service;

import com.nukil.volume.bean.HumanBean;
import com.nukil.volume.util.LoadPropers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class WriteFile2Disk extends Thread {
    private static boolean DEBUG = Boolean.parseBoolean(LoadPropers.getProperties().getProperty("service.process.debug.enable", "false").trim());
    private static String FILE_PATH = LoadPropers.getProperties().getProperty("save.data.file.path", "./data/");
    private static SaveData2Mysql saveData2Mysql = SaveData2Mysql.getInstance();
    private static Logger LOG = LoggerFactory.getLogger(WriteFile2Disk.class);
    private static boolean STOPPED = false;
    private static BlockingQueue<HumanBean> queue = new LinkedBlockingDeque<>();
    private static WriteFile2Disk instance = null;
    private WriteFile2Disk() {}
    public static WriteFile2Disk getInstance() {
        if (null == instance) {
            synchronized (WriteFile2Disk.class) {
                if (null == instance) {
                    instance = new WriteFile2Disk();
                }
            }
        }
        return instance;
    }

    @Override
    public void run() {
        LOG.info("write file to disk thread start.......");
        while (!STOPPED) {
            if (queue.size() > 0) {
                try {
                    File file = new File(FILE_PATH);
                    if (!file.exists()) {
                        if (DEBUG) {
                            LOG.info("create dir to save data files!");
                        }
                        while (!file.mkdirs()) {
                            LOG.error("create data file failed, after 3s to retry");
                            Thread.sleep(3000);
                        }
                    }

                    for (int i = 0; i < queue.size(); ++i) {
                        HumanBean hb = queue.take();
                        File newFile = new File(new StringBuilder(FILE_PATH).append(hb.getDate()).append(",")
                                .append(hb.getHour()).append(",")
                                .append(hb.getAreaID()).append(",")
                                .append(hb.getCameraID()).append(",")
                                .append(hb.getCount()).append(",")
                                .append(hb.getTableName()).append(",")
                                .append(hb.getUuid()).toString());
                        if (!newFile.exists()) {
                            while (!newFile.createNewFile()) {
                                LOG.error("create data file failed, after 3s to retry");
                                Thread.sleep(3000);
                            }
                        }
                        LOG.info(String.format("send task to save data to mysql thread, file name is %s", hb.toString()));
                        saveData2Mysql.addTask(hb);
                    }
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
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
