package com.netposa.poseidon.fakecardetect.utils;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public final class ConnectionPoolUtil {
    private static ConnectionPoolUtil instance;
    private static Object lock = new Object();
    public ComboPooledDataSource ds;
    private static String c3p0Properties = "c3p0.properties";
    public static String sql ;  // 插入Sql
    public static String tbname; //操作的表名称

    private ConnectionPoolUtil() throws Exception {
        Properties props = LoadInitUtil.initProps(c3p0Properties);

        InputStreamReader in = new InputStreamReader(ConnectionPoolUtil.class.getClassLoader().getResourceAsStream("server.properties"),"UTF-8");
        props.load(in);
        ds = new ComboPooledDataSource();
        ds.setUser(props.getProperty("vmc_user"));
        ds.setPassword(props.getProperty("vmc_password"));
        ds.setJdbcUrl(props.getProperty("vmc_url"));
        ds.setInitialPoolSize(Integer.parseInt(props.getProperty("initialPoolSize")));
        ds.setMinPoolSize(Integer.parseInt(props.getProperty("minPoolSize")));
        ds.setMaxPoolSize(Integer.parseInt(props.getProperty("maxPoolSize")));
        ds.setMaxStatements(Integer.parseInt(props.getProperty("maxStatements")));
        ds.setMaxIdleTime(Integer.parseInt(props.getProperty("maxIdleTime")));
        ds.setAcquireRetryAttempts(Integer.parseInt(props.getProperty("acquireRetryAttempts")));
        ds.setAcquireRetryDelay(Integer.parseInt(props.getProperty("acquireRetryDelay")));
        ds.setBreakAfterAcquireFailure(Boolean.getBoolean(props.getProperty("breakAfterAcquireFailure")));
        ds.setTestConnectionOnCheckin(Boolean.getBoolean(props.getProperty("testConnectionOnCheckin")));
        ds.setTestConnectionOnCheckout(Boolean.getBoolean(props.getProperty("testConnectionOnCheckout")));
        ds.setIdleConnectionTestPeriod(Integer.parseInt(props.getProperty("idleConnectionTestPeriod")));
        sql = props.getProperty("sqlString");
        tbname = props.getProperty("tbname");
    }

    public static String getTbname() {
        return tbname;
    }

    public static final String getSql() {
        return sql;
    }


    public static final ConnectionPoolUtil getInstance() {
        synchronized (lock) {
            if (instance == null) {
                try {
                    instance = new ConnectionPoolUtil();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return instance;
    }

    public  synchronized final Connection getConnection() {
        try {
            return ds.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void shutdown() {
        if (instance != null) {
            synchronized (lock) {
                try {
                    DataSources.destroy(ds);
                } catch (SQLException e) {
                    e.printStackTrace();
                }finally {
                    instance = null;
                }
            }
        }
    }
}
