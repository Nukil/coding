package com.netposa.poseidon.human.service;

import com.netposa.HbaseUtil;
import com.netposa.poseidon.human.bean.*;
import com.netposa.poseidon.human.rpc.inrpc.RecentDataBean;
import com.netposa.poseidon.human.rpc.inrpc.SetRecentDataInputRecord;
import com.netposa.poseidon.human.rpc.inrpc.SetRecentDataResponse;
import com.netposa.poseidon.human.rpc.inrpc.StatusCode;
import com.netposa.poseidon.human.util.Byte2FloatUtil;
import com.netposa.poseidon.human.util.HashAlgorithm;
import com.netposa.poseidon.human.util.ParseBytesUtil;
import com.netposa.poseidon.kafka.bean.RowMessage;
import com.netposa.poseidon.kafka.consumer.DirectKafkaConsumer;
import kafka.common.TopicAndPartition;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Put;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class FetchMessageService extends Thread {
    // 日志对象
    private Logger logger = Logger.getLogger(this.getClass());
    // 初始化提供的配置
    private Properties properties;
    // kafka数据消费对象
    private DirectKafkaConsumer consumer;
    // 程序终止指示
    private boolean stopped = false;
    // 服务debug开启标志
    private boolean debug;
    // kafka数据拉取间隔
    private int fetchInterval;
    // offsets信息
    private final Map<TopicAndPartition, Long> offsets = new HashMap<>();

    // Hbase表名
    private String tableName;
    // 特征版本号长度
    private int featureVersionSize;
    // 特征长度
    private int featureSize;
    // 特征尾长度
    private int featureTailSize;
    // 特征总长度
    private int fullFeatureSize;
    // float长度
    private int floatSize = 4;
    private int featRawDim;
    // kafka数据解析对象
    private ParseBytesUtil parseBytesUtils = new ParseBytesUtil();
    // Rpc管理单例对象
    private RpcManagerService rpcManagerService = RpcManagerService.getInstance();

    // kafka拉取数据总量
    private int kafkaTotalCount = 0;
    // 存储hbase数据总量
    private int hbaseTotalCount = 0;
    // 发送给server数据总量
    private int serverTotalCount = 0;
    // 总处理数据量
    private int totalprocessCount = 0;
    // 单次输出数据量
    private int outputDataCount = 0;


    public FetchMessageService(Properties serverProperties, Properties kafkaProperties) {
        this.properties = kafkaProperties;
        this.debug = Boolean.parseBoolean(serverProperties.getProperty("service.process.debug.enable"));
        this.fetchInterval = Integer.parseInt(serverProperties.getProperty("service.fetch.messages.interval.ms"));
        this.tableName = serverProperties.getProperty("hbase.table.name");
        this.featureVersionSize = Integer.parseInt(serverProperties.getProperty("human.version.size"));
        this.featureSize = Integer.parseInt(serverProperties.getProperty("human.feature.size"));
        this.featureTailSize = Integer.parseInt(serverProperties.getProperty("human.tail.size"));
        fullFeatureSize = featureVersionSize + featureSize + featureTailSize;
        featRawDim = featureSize / floatSize;
    }

    @Override
    public void run() {
        // hbase表不存在时，创建表
        if (!HbaseUtil.tableIsExists(tableName)) {
            HbaseUtil.createTable(tableName, new String[]{"cf"}, HashAlgorithm.getSplitKeys());
        }

        while (!stopped && consumer == null) {
            try {
                consumer = new DirectKafkaConsumer(properties);
            } catch (Exception e) {
                logger.error("can not create kafka consumer, sleep 10s and try again");
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (Exception e1) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        while (!stopped) {
            try {
                List<RowMessage> messages = consumer.fetchMessages();
                // 统计数据拉取总量
                kafkaTotalCount += messages.size();
                if (messages.size() > 0) {
                    executeBatch(messages, fullFeatureSize);
                }
                logger.info("kafka this batch size is : " + messages.size());
                logger.info(String.format("streaming data statistics [service_name:%s,total_input:%s,current_input:%s,total_process:%s,current_process:%s,total_output:%s,current_output:%s]",
                        "human_comparsion_master", kafkaTotalCount, messages.size(), totalprocessCount, messages.size(), serverTotalCount, outputDataCount));
                if (debug) {
                    logger.info("all cluster connection count : " + rpcManagerService.getAvailableServerNum());
                }
                if (messages.size() < 500) {
                    TimeUnit.MILLISECONDS.sleep(fetchInterval);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void executeBatch(List<RowMessage> messages, int size) {
        List<HumanFeature> list = new ArrayList<>();
        List<RecentDataBean> rpcList = new ArrayList<>();
        for (RowMessage message : messages) {
            HumanFeature humanFeature = new HumanFeature();
            try {
                humanFeature = parseBytesUtils.dataForETL2PB(message.message(), size);
            } catch (Exception e) {
                logger.error(String.format("can not parse message %s, %s",  e.getMessage(), e));
            }
            if (null != humanFeature && !StringUtils.isEmpty(humanFeature.getJlbh())
                    && !StringUtils.isEmpty(humanFeature.getCameraId())
                    && !StringUtils.isEmpty(humanFeature.getGatherTime())
                    && null != humanFeature.getFeature()
                    && humanFeature.getFeature().length == fullFeatureSize) {
                byte[] feature = new byte[featureSize];
                System.arraycopy(humanFeature.getFeature(), featureVersionSize, feature, 0, featureSize);
                humanFeature.setFeature(feature);
                list.add(humanFeature);

                // 将特征转换为float，准备发送给server
                List<Double> featureFloat = new ArrayList<>();
                for (int i = 0; i < featRawDim; i++) {
                    featureFloat.add((double)Byte2FloatUtil.byte2float(feature, i * floatSize));
                }
                rpcList.add(new RecentDataBean(humanFeature.getJlbh(),humanFeature.getCameraId(), Long.parseLong(humanFeature.getGatherTime()), featureFloat));
            }

            // 读取本批kafka数据offset信息
            synchronized (offsets) {
                if (!offsets.containsKey(message.topicAndPartition()) || message.offset() > offsets.get(message.topicAndPartition())) {
                    offsets.put(message.topicAndPartition(), message.offset());
                }
            }
        }
        // 统计数据处理总量
        totalprocessCount += list.size();
        //保存数据到hbase
        boolean flag = writeData2Hbase(list);
        while (!flag) {
            logger.error("can not save data to hbase! try again after 10s");
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
            flag = writeData2Hbase(list);
        }
        // 统计保存到hbase数据总量
        hbaseTotalCount += list.size();
        // 将数据发送给server
        sendData2Server(rpcList);
        // 统计发送到server数据总量
        serverTotalCount += list.size();
        // 统计本批次输出数据量
        outputDataCount = list.size();
        // 提交kafka offset
        consumer.commitOffsets(offsets);
        // 清空offset信息
        offsets.clear();
    }

    /**
     * 将数据保存到hbase
     * @param list 数据集
     * @return true or false
     */
    private boolean writeData2Hbase(List<HumanFeature> list) {
        List<Put> puts = new ArrayList<>();
        if (list.isEmpty()) {
            logger.warn("data list is empty");
            return true;
        } else {
            for (HumanFeature humanFeature : list) {
                Put put = new Put(HashAlgorithm.hash(humanFeature.getGatherTime() + humanFeature.getJlbh()).getBytes());
                put.addColumn("cf".getBytes(), "logNum".getBytes(), humanFeature.getJlbh().getBytes());
                put.addColumn("cf".getBytes(), "cameraId".getBytes(), humanFeature.getCameraId().getBytes());
                put.addColumn("cf".getBytes(), "gatherTime".getBytes(), humanFeature.getGatherTime().getBytes());
                put.addColumn("cf".getBytes(), "feature".getBytes(), humanFeature.getFeature());
                puts.add(put);
            }
        }
        return HbaseUtil.save(puts, tableName);
    }

    /**
     * 通过rpc将数据发送给server
     * @param list 数据集
     */
    private void sendData2Server(List<RecentDataBean> list) {
        if (list.isEmpty()) {
            logger.warn("data list is empty");
        } else {
            //对数据进行分组
            Map<ConnectionManagerKey, SetRecentDataInputRecord> dataMap = splitDataList(list);
            Iterator entries = dataMap.entrySet().iterator();
            // 找到当前可用的server

            while (entries.hasNext()) {
                Map.Entry entry = (Map.Entry)entries.next();
                SetRecentDataInputRecord record = (SetRecentDataInputRecord)entry.getValue();
                if (null == record || null == record.getDataList() || record.getDataList().size() <= 0) {
                    entries.remove();
                    continue;
                }
                boolean flag = true;
                String uuid = UUID.randomUUID().toString();
                while (flag) {
                    // 先尝试获取一个对应server的连接, 有可能拿到别的节点的连接
                    AvailableConnectionBean connectionBean = rpcManagerService.getAvailableConnection((ConnectionManagerKey) entry.getKey(), true);
                    if (null != connectionBean) {
                        ConnectionManagerKey key = connectionBean.getKey();
                        ClientStatus cs = connectionBean.getCs();
                        if (cs.getStatusCode() == ConnectionStatusCode.OK) {
                            try {
                                SetRecentDataResponse response = cs.getConnection().setRecentData(record, uuid);
                                if (response.getRCode() == StatusCode.OK) {
                                    entries.remove();
                                } else {
                                    logger.error("id : " + uuid + ", save data failed : " + response.getMessage());
                                }
                                if (debug) {
                                    logger.info(String.format("id : %s, data was sent to %s : %s", uuid, key.getIp(), key.getPort()));
                                }
                                rpcManagerService.putConnection(key, cs);
                            } catch (TException e) {
                                logger.error(String.format("id : %s, send data to server error %s, %s", uuid, e.getMessage(), e));
                                cs.setStatusCode(ConnectionStatusCode.DISCONNECTED);
                                rpcManagerService.putConnection(key, cs);
                            }
                        }
                    }
                    flag = dataMap.containsKey((ConnectionManagerKey)entry.getKey());
                    if (flag) {
                        logger.warn("id : " + uuid + " ,send data failed, server may all dead, retry after 3s");
                        try {
                            TimeUnit.SECONDS.sleep(3);
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    /**
     * 对数据进行切分
     * @param list List<RecentDataBean>
     */
    private Map<ConnectionManagerKey, SetRecentDataInputRecord> splitDataList(List<RecentDataBean> list) {
        Set<ConnectionManagerKey> clusterKeySet = rpcManagerService.getCluster().keySet();
        Map<ConnectionManagerKey, SetRecentDataInputRecord> map = new HashMap<>();

        for (RecentDataBean recentDataBean : list) {
            int hashValue = HashAlgorithm.BKDRHash(recentDataBean.getLogNum()) % HashAlgorithm.BKDRHashValue;
            // 根据hash值将数据分配到Map的对应位置
            for (ConnectionManagerKey key : clusterKeySet) {
                if (hashValue >= key.getStartHashValue() && hashValue <= key.getEndHashValue()) {
                    if (map.containsKey(key)) {
                        map.get(key).getDataList().add(recentDataBean);
                    } else {
                        List<RecentDataBean> dataList = new ArrayList<>();
                        dataList.add(recentDataBean);
                        map.put(key, new SetRecentDataInputRecord().setDataList(dataList));
                    }
                }
            }
        }
        return map;
    }

    public void shutDown() {
        this.stopped = true;
        consumer.shutDown();
    }
}