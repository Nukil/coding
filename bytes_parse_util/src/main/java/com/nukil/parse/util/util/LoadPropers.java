package com.nukil.parse.util.util;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by Nukil on 2017/4/
 */
public class LoadPropers {
    private Properties properties = null;
    private Logger logger = Logger.getLogger(LoadPropers.class);

    private LoadPropers() {}

    private static LoadPropers instance = null;

    public static LoadPropers getSingleInstance() {
        if (null == instance) {
            synchronized (LoadPropers.class) {
                if (null == instance) {
                    instance = new LoadPropers();
                }
            }
        }
        return instance;
    }

    private Properties loadProperties(String filename) {
        ClassLoader classLoader = this.getClass().getClassLoader();
        Properties tmpProperties = new Properties();
        InputStream configStream = classLoader.getResourceAsStream(filename);
        try {
            tmpProperties.load(configStream);
        } catch (IOException e) {
            logger.error(e.getMessage());
        } finally {
            try {
                configStream.close();
            } catch (IOException x) {
                logger.error(x.getMessage());
            }
        }
        return tmpProperties;
    }

    public Properties getProperties(String filename) {
        if (null == properties) {
            synchronized (LoadPropers.class) {
                if (null == properties) {
                    properties = loadProperties(filename + ".properties");
                }
            }
        }
        return properties;
    }
}
