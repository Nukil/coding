package com.netposa.poseidon.library.util;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class HbaseUtil {
    public static Configuration hConfiguration = null;
    private static Logger log = LoggerFactory.getLogger(HbaseUtil.class);
    private static Connection hConnection = null;

    static {
        init();
    }

    /**
     * 初始化Hbase配置
     */
    public synchronized static void init() {
        hConfiguration = HBaseConfiguration.create();
    }

    /**
     * 获取连接
     *
     * @return
     */
    public synchronized static Connection getConn() {
        try {
            if (hConnection == null || hConnection.isClosed()) {
                hConnection = ConnectionFactory.createConnection(hConfiguration);
            }
        } catch (IOException e) {
            log.error("get connection failed！please check args!" + e.getMessage(), e);
            try {
                Thread.sleep(3000l);
            } catch (InterruptedException e1) {
                log.error(e.getMessage(), e);
            }
            hConnection = getConn();
        }
        return hConnection;
    }

    /**
     * 创建一个表
     *
     * @param tableName
     * @param columnFamilys
     */
    public static boolean createTable(String tableName, String[] columnFamilys, byte[][] splitkeys) {
        Admin admin = null;
        try {
            Connection conn = getConn();
            admin = conn.getAdmin();
            if (admin.tableExists(TableName.valueOf(tableName))) {
                log.warn(String.format("此%s表已存在！", tableName));
                return true;
            } else {
                HTableDescriptor tableDesc = new HTableDescriptor(TableName.valueOf(tableName));
                for (String columnFamily : columnFamilys) {
                    tableDesc.addFamily(new HColumnDescriptor(columnFamily));
                }
                if (splitkeys != null) {
                    admin.createTableAsync(tableDesc, splitkeys);
                } else {
                    admin.createTable(tableDesc);
                }
                log.info(String.format("%s表创建成功！", tableName));
                return true;
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                if (admin != null) {
                    admin.close();// 关闭释放资源
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return false;
    }

    /**
     * 创建一个表
     *
     * @param tableName
     */
    public static boolean tableIsExists(String tableName) {
        Admin admin = null;
        boolean flag = false;
        try {
            Connection conn = getConn();
            admin = conn.getAdmin();
            flag = admin.tableExists(TableName.valueOf(tableName));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                if (admin != null) {
                    admin.close();// 关闭释放资源
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return flag;
    }


    /**
     * 创建一个表，如果该表已存在，则将其覆盖（删除重建）
     *
     * @param tableName
     * @param columnFamilys
     */
    public static void createOrCoverTable(String tableName, String[] columnFamilys) {
        Admin admin = null;
        try {
            Connection conn = getConn();
            admin = conn.getAdmin();
            if (admin.tableExists(TableName.valueOf(tableName))) {
                log.info(String.format("此s%表已存在！", tableName));
                deleteTable(tableName);
            }
            HTableDescriptor tableDesc = new HTableDescriptor(TableName.valueOf(tableName));
            for (String columnFamily : columnFamilys) {
                tableDesc.addFamily(new HColumnDescriptor(columnFamily));
            }
            admin.createTable(tableDesc);
            log.info(String.format("创建表%s成功！", tableName));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                if (admin != null) {
                    admin.close();// 关闭释放资源
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 删除一个表
     *
     * @param tableName
     */
    public static boolean deleteTable(String tableName) {
        try {
            Connection conn = getConn();
            final Admin admin = conn.getAdmin();
            final TableName name = TableName.valueOf(tableName);
            if (admin.tableExists(name)) {
                admin.disableTableAsync(name);// 禁用表
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            admin.deleteTable(name);// 删除表
                        } catch (IOException e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                }).start();
                log.info(String.format("删除表%s成功！", tableName));
            } else {
                log.warn(String.format("表%s不存在！", tableName));
            }
            admin.close();// 关闭释放资源
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * 添加一条数据到Hbase
     *
     * @param tableName
     * @param rowKey
     * @param family
     * @param map
     * @return
     */
    public static boolean save(String tableName, String rowKey, String family, Map<String, byte[]> map) {
        Table table = null;
        try {
            Connection conn = getConn();
            table = conn.getTable(TableName.valueOf(tableName));
            Put put = new Put(rowKey.getBytes());// 设置rowkey
            for (Entry<String, byte[]> e : map.entrySet()) {
                put.addColumn(family.getBytes(), e.getKey().getBytes(), e.getValue());
            }
            table.put(put);
            return true;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            if (table != null) {
                try {
                    table.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return false;
    }


    /**
     * 批量添加数据到Hbase
     *
     * @param put
     * @param tableName
     * @return
     */
    public static boolean save(Put put, String tableName) {
        Table table = null;
        try {
            Connection conn = getConn();
            table = conn.getTable(TableName.valueOf(tableName));
            table.put(put);
            return true;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            if (table != null) {
                try {
                    table.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return false;
    }

    /**
     * 批量添加数据到Hbase
     *
     * @param puts
     * @param tableName
     * @return
     */
    public static boolean save(List<Put> puts, String tableName) {
        Table table = null;
        try {
            Connection conn = getConn();
            TableName name = TableName.valueOf(tableName);
            table = conn.getTable(name);
            table.put(puts);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (table != null) {
                try {
                    table.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return false;
    }

    /**
     * 根据rowKey查询一条数据
     *
     * @param tableName
     * @param rowKey
     * @return
     */
    public static Result getOneRow(String tableName, String rowKey) {
        Table table = null;
        Result rsResult = null;
        try {
            Connection conn = getConn();
            table = conn.getTable(TableName.valueOf(tableName));
            Get get = new Get(rowKey.getBytes());
            rsResult = table.get(get);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return rsResult;
    }

    /**
     * 查询指定rowKey的某几个列
     *
     * @param tableName
     * @param rowKey
     * @param cols      键为列族名称,值为列族下多个单元格的名称
     * @return
     */
    public static Result getOneRowAndMultiColumn(String tableName, String rowKey, Map<String, String[]> cols) {
        Table table = null;
        Result rsResult = null;
        try {
            Connection conn = getConn();
            table = conn.getTable(TableName.valueOf(tableName));
            Get get = new Get(rowKey.getBytes());
            for (Entry<String, String[]> entry : cols.entrySet()) {
                for (int i = 0; i < entry.getValue().length; i++) {
                    get.addColumn(entry.getKey().getBytes(), entry.getValue()[i].getBytes());
                }
            }
            get.setCheckExistenceOnly(true);
            rsResult = table.get(get);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return rsResult;
    }

    /**
     * 根据rowKey前缀查询数据
     *
     * @param tableName
     * @param rowKeyLike rowKey前缀字符串
     * @return
     */
    public static List<Result> getRows(String tableName, String rowKeyLike) {
        Table table = null;
        List<Result> list = null;
        try {
            Connection conn = getConn();
            table = conn.getTable(TableName.valueOf(tableName));
            PrefixFilter filter = new PrefixFilter(rowKeyLike.getBytes());
            Scan scan = new Scan();
            scan.setFilter(filter);
            scan.setCaching(10000);
            ResultScanner scanner = table.getScanner(scan);
            list = new ArrayList<Result>();
            for (Result rs : scanner) {
                list.add(rs);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return list;
    }

    /**
     * 根据rowKey前缀查询指定的列
     *
     * @param tableName
     * @param rowKeyLike rowKey前缀
     * @param cols       键为列族名称,值为列族下多个单元格的名称
     * @return
     */
    public static List<Result> getRows(String tableName, String rowKeyLike, Map<String, String[]> cols) {
        Table table = null;
        List<Result> list = null;
        try {
            Connection conn = getConn();
            table = conn.getTable(TableName.valueOf(tableName));
            PrefixFilter filter = new PrefixFilter(rowKeyLike.getBytes());
            Scan scan = new Scan();
            for (Entry<String, String[]> entry : cols.entrySet()) {
                for (int i = 0; i < entry.getValue().length; i++) {
                    scan.addColumn(entry.getKey().getBytes(), entry.getValue()[i].getBytes());
                }
            }
            scan.setFilter(filter);
            scan.setCaching(10000);
            ResultScanner scanner = table.getScanner(scan);
            list = new ArrayList<Result>();
            for (Result rs : scanner) {
                list.add(rs);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return list;
    }

    /**
     * 范围查询
     *
     * @param tableName
     * @param startRow  开始row
     * @param stopRow   结束row
     * @return
     */
    public static List<Result> getRows(String tableName, String startRow, String stopRow) {
        Table table = null;
        List<Result> list = null;
        ResultScanner scanner = null;
        try {
            Connection conn = getConn();
            table = conn.getTable(TableName.valueOf(tableName));
            Scan scan = new Scan();
            scan.setStartRow(startRow.getBytes());
            scan.setStopRow(stopRow.getBytes());
            scan.setCaching(500000);
            scanner = table.getScanner(scan);
            log.info("scan data send to list!!!!");
            list = new ArrayList<Result>();
            for (Result rsResult : scanner) {
                list.add(rsResult);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        return list;
    }

    /**
     * 范围查询
     *
     * @param tableName
     * @param startRow  开始row
     * @param stopRow   结束row
     * @return
     */
    public static List<Result> getRows(String tableName, String startRow, String stopRow, Map<String, String[]> cols, Filter filter) {
        Table table = null;
        List<Result> list = null;
        try {
            Connection conn = getConn();
            table = conn.getTable(TableName.valueOf(tableName));
            Scan scan = new Scan();
            scan.setStartRow(startRow.getBytes());
            scan.setStopRow(stopRow.getBytes());
            for (Entry<String, String[]> entry : cols.entrySet()) {
                for (int i = 0; i < entry.getValue().length; i++) {
                    scan.addColumn(entry.getKey().getBytes(), entry.getValue()[i].getBytes());
                }
            }
            if (filter != null && filter.hasFilterRow()) {
                scan.setFilter(filter);
            }
            scan.setCaching(500000);
            ResultScanner scanner = table.getScanner(scan);
            list = new ArrayList<>();
            for (Result rsResult : scanner) {
                list.add(rsResult);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return list;
    }

    public static List<Result> scanForQualifier(String tableName, String family, String qualifier) {
        Table table = null;
        List<Result> list = new ArrayList<>();
        try {
            Connection conn = getConn();
            table = conn.getTable(TableName.valueOf(tableName));
            Scan scan = new Scan();
            scan.addColumn(family.getBytes(), qualifier.getBytes());
            ResultScanner scanner = table.getScanner(scan);
            for (Result rsResult : scanner) {
                list.add(rsResult);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 根据前缀删除记录
     *
     * @param tableName
     * @param rowKeyLike rowKey前缀
     */
    public static boolean deleteRecords(String tableName, String rowKeyLike) {
        Table table = null;
        try {
            Connection conn = getConn();
            table = conn.getTable(TableName.valueOf(tableName));
            PrefixFilter filter = new PrefixFilter(rowKeyLike.getBytes());
            Scan scan = new Scan();
            scan.setFilter(filter);
            ResultScanner scanner = table.getScanner(scan);
            List<Delete> list = new ArrayList<Delete>();
            for (Result rs : scanner) {
                Delete del = new Delete(rs.getRow());
                list.add(del);
            }
            table.delete(list);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (table != null) {
                try {
                    table.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return false;
    }

    /**
     * 删除指定名称的列族
     *
     * @param tableName
     * @param family
     * @return
     */
    public static boolean deleteFamily(String tableName, String family) {
        Admin admin = null;
        try {
            Connection conn = getConn();
            admin = conn.getAdmin();
            admin.deleteColumn(TableName.valueOf(tableName), family.getBytes());
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                if (admin != null) {
                    admin.close();// 关闭释放资源
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return false;
    }

    /**
     * @param deletes
     * @param tableName
     * @return
     */
    public static boolean batchDelete( String tableName,List<Delete> deletes) {
        Table table = null;
        Connection conn = getConn();
        try {
            table = conn.getTable(TableName.valueOf(tableName));
            table.delete(deletes);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                if (table != null) {
                    table.close();// 关闭释放资源
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return false;
    }
    /**
     * @param delete
     * @param tableName
     * @return
     */
    public static boolean delete(Delete delete, String tableName) {
        Table table = null;
        Connection conn = getConn();
        try {
            table = conn.getTable(TableName.valueOf(tableName));
            table.delete(delete);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                if (table != null) {
                    table.close();// 关闭释放资源
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return false;
    }

    public static void close() {
        if (hConnection != null) {
            try {
                hConnection.close();
            } catch (IOException e) {
                log.error("close hbase connection failed! msg is : " + e.getMessage(), e);
            }
        }
    }

    /**
     * 获得相等过滤器。相当于SQL的 [字段] = [值]
     *
     * @param cf  列族名
     * @param col 列名
     * @param val 值
     * @return 过滤器
     */
    public static Filter eqFilter(String cf, String col, byte[] val) {
        SingleColumnValueFilter f = new SingleColumnValueFilter(cf.getBytes(), col.getBytes(), CompareFilter.CompareOp.EQUAL, val);
        f.setLatestVersionOnly(true);
        f.setFilterIfMissing(true);
        return f;
    }

    /**
     * 获得大于过滤器。相当于SQL的 [字段] > [值]
     *
     * @param cf  列族名
     * @param col 列名
     * @param val 值
     * @return 过滤器
     */
    public static Filter gtFilter(String cf, String col, byte[] val) {
        SingleColumnValueFilter f = new SingleColumnValueFilter(cf.getBytes(), col.getBytes(), CompareFilter.CompareOp.GREATER, val);
        f.setLatestVersionOnly(true);
        f.setFilterIfMissing(true);
        return f;
    }

    /**
     * 获得大于等于过滤器。相当于SQL的 [字段] >= [值]
     *
     * @param cf  列族名
     * @param col 列名
     * @param val 值
     * @return 过滤器
     */
    public static Filter gteqFilter(String cf, String col, byte[] val) {
        SingleColumnValueFilter f = new SingleColumnValueFilter(cf.getBytes(), col.getBytes(), CompareFilter.CompareOp.GREATER_OR_EQUAL, val);
        f.setLatestVersionOnly(true);
        f.setFilterIfMissing(true);
        return f;
    }

    /**
     * 获得小于过滤器。相当于SQL的 [字段] < [值]
     *
     * @param cf  列族名
     * @param col 列名
     * @param val 值
     * @return 过滤器
     */
    public static Filter ltFilter(String cf, String col, byte[] val) {
        SingleColumnValueFilter f = new SingleColumnValueFilter(cf.getBytes(), col.getBytes(), CompareFilter.CompareOp.LESS, val);
        f.setLatestVersionOnly(true);
        f.setFilterIfMissing(true);
        return f;
    }

    /**
     * 获得小于等于过滤器。相当于SQL的 [字段] <= [值]
     *
     * @param cf  列族名
     * @param col 列名
     * @param val 值
     * @return 过滤器
     */
    public static Filter lteqFilter(String cf, String col, byte[] val) {
        SingleColumnValueFilter f = new SingleColumnValueFilter(cf.getBytes(), col.getBytes(), CompareFilter.CompareOp.LESS_OR_EQUAL, val);
        f.setLatestVersionOnly(true);
        f.setFilterIfMissing(true);
        return f;
    }

    /**
     * 获得不等于过滤器。相当于SQL的 [字段] != [值]
     *
     * @param cf  列族名
     * @param col 列名
     * @param val 值
     * @return 过滤器
     */
    public static Filter neqFilter(String cf, String col, byte[] val) {
        SingleColumnValueFilter f = new SingleColumnValueFilter(cf.getBytes(), col.getBytes(), CompareFilter.CompareOp.NOT_EQUAL, val);
        f.setLatestVersionOnly(true);
        f.setFilterIfMissing(true);
        return f;
    }

    /**
     * 和过滤器 相当于SQL的 的 and
     *
     * @param filters 多个过滤器
     * @return 过滤器
     */
    public static Filter andFilter(Filter... filters) {
        FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
        if (filters != null && filters.length > 0) {
            if (filters.length > 1) {
                for (Filter f : filters) {
                    filterList.addFilter(f);
                }
            }
            if (filters.length == 1) {
                return filters[0];
            }
        }
        return filterList;
    }

    /**
     * 和过滤器 相当于SQL的 的 and
     *
     * @param filters 多个过滤器
     * @return 过滤器
     */
    public static Filter andFilter(Collection<Filter> filters) {
        return andFilter(filters.toArray(new Filter[0]));
    }


    /**
     * 或过滤器 相当于SQL的 or
     *
     * @param filters 多个过滤器
     * @return 过滤器
     */
    public static Filter orFilter(Filter... filters) {
        FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ONE);
        if (filters != null && filters.length > 0) {
            for (Filter f : filters) {
                filterList.addFilter(f);
            }
        }
        return filterList;
    }

    /**
     * 或过滤器 相当于SQL的 or
     *
     * @param filters 多个过滤器
     * @return 过滤器
     */
    public static Filter orFilter(Collection<Filter> filters) {
        return orFilter(filters.toArray(new Filter[0]));
    }

    /**
     * 非空过滤器 相当于SQL的 is not null
     *
     * @param cf  列族
     * @param col 列
     * @return 过滤器
     */
    public static Filter notNullFilter(String cf, String col) {
        SingleColumnValueFilter filter = new SingleColumnValueFilter(cf.getBytes(), col.getBytes(), CompareFilter.CompareOp.NOT_EQUAL, new NullComparator());
        filter.setFilterIfMissing(true);
        filter.setLatestVersionOnly(true);
        return filter;
    }

    /**
     * 空过滤器 相当于SQL的 is null
     *
     * @param cf  列族
     * @param col 列
     * @return 过滤器
     */
    public static Filter nullFilter(String cf, String col) {
        SingleColumnValueFilter filter = new SingleColumnValueFilter(cf.getBytes(), col.getBytes(), CompareFilter.CompareOp.EQUAL, new NullComparator());
        filter.setFilterIfMissing(false);
        filter.setLatestVersionOnly(true);
        return filter;
    }

    /**
     * 子字符串过滤器 相当于SQL的 like '%[val]%'
     *
     * @param cf  列族
     * @param col 列
     * @param sub 子字符串
     * @return 过滤器
     */
    public static Filter subStringFilter(String cf, String col, String sub) {
        SingleColumnValueFilter filter = new SingleColumnValueFilter(cf.getBytes(), col.getBytes(), CompareFilter.CompareOp.EQUAL, new SubstringComparator(sub));
        filter.setFilterIfMissing(true);
        filter.setLatestVersionOnly(true);
        return filter;
    }

    /**
     * 正则过滤器 相当于SQL的 rlike '[regex]'
     *
     * @param cf    列族
     * @param col   列
     * @param regex 正则表达式
     * @return 过滤器
     */
    public static Filter regexFilter(String cf, String col, String regex) {
        SingleColumnValueFilter filter = new SingleColumnValueFilter(cf.getBytes(), col.getBytes(), CompareFilter.CompareOp.EQUAL, new RegexStringComparator(regex));
        filter.setFilterIfMissing(true);
        filter.setLatestVersionOnly(true);
        return filter;
    }

}
