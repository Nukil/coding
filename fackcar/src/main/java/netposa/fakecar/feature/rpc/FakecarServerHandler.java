package netposa.fakecar.feature.rpc;

import java.util.HashMap;
import java.util.Map;

import netposa.fakecar.feature.rpc.bdk.BdkCheckProcessor;
import netposa.fakecar.feature.rpc.search.SimilarVehicleSearchThread;
import netposa.fakecar.feature.rpc.util.ByteUtils;
import netposa.fakecar.feature.rpc.util.CalcHammingDistUtil;
import netposa.fakecar.feature.rpc.util.FeatureResultDBOutputThread;

import org.apache.commons.collections.MapUtils;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 接收到rpc的请求数据后,执行套牌分析的处理逻辑
 *
 * @author hongs.yang
 */
public class FakecarServerHandler extends FeatureCalculatorServiceImpl {
    private static final Logger LOG = LoggerFactory
            .getLogger(FakecarServerHandler.class);
    private int totalOutput = 0;
    static final Object obj = new Object();
    /**
     * 加载基础配置信息
     *
     * @throws Exception
     */
    public FakecarServerHandler() throws Exception {
        super();

        //如果开启找原车，那么启动找原车服务
        if (similar_vehicle_search_enable) {
            if (searchThread == null) {
                searchThread = new SimilarVehicleSearchThread(result_count, result_distence, searchDate, host, port, process_debug, result_id_only, idLength, featureSize);
                searchThread.setDaemon(true);
                searchThread.start();
            }
        }
    }

    @Override
    public void sendRecordBuffer(InputRecordBuffer record) throws TException {
        // 对接收到的数据输出到日志中
        if (receive_debug) {
            LOG.info(String.format("input record data is => {%s}", toString(record)));
        }

        byte[] licence_plate = record.getLicence_plate();
        byte[] licence_plate_color = record.getLicence_plate_color();
        double input_confidence_level = record.getConfidence_level();

        float face_value = 0.0f;
        try {

            int weight = record.getVehicle_rigth_x() - record.getVehicle_left_x();
            int height = record.getVehicle_rigth_y() - record.getVehicle_left_y();

            if (height > 0 && weight > 0) {
                face_value = Float.valueOf(weight) / Float.valueOf(height);
            }

            //face_value = FeatureJsonParseUtils.parseVehicleFacePoint(buffer);
        } catch (Exception e) {
            LOG.warn("parse vehicle face point error => " + e.getMessage(), e);
        }

        if (bdk_check_enable) {
            if (!bdkTransform(licence_plate, licence_plate_color)) {
                BdkCheckProcessor.execute(record);
            } else {
                LOG.warn("bdk is ignore！！");
            }
        }

        // 对数据进行清洗
        long begin_time = System.nanoTime();
        boolean exclude = transform(licence_plate, licence_plate_color,
                record.getSource_type(), input_confidence_level,
                record.getTraffic_time(), record.getPlate_confidence(), record.getLogo_confidence(),
                record.getVehicle_feature(), face_value, record.getVehicle_logo(), record.getVehicle_child_logo(),
                record.getHead_rear());
        if (exectime_debug) {
            long execute_time = System.nanoTime() - begin_time;
            LOG.info(String.format("==>transform receive record execute time is [%d] ns", execute_time));
        }

        // 如果数据需要被排除,不执行后面的流程,直接结束当前函数
        if (exclude) {
            if (process_debug) {
                LOG.info("process stop, this record transform exclude," + " this method call return......");
            }
            // 记录接收到的总记录数,在指定的时间段内
            calculatorReceiveCount(false);
            LOG.info(String.format("streaming data statistics [service_name:%s,total_input:%d,current_input:%d,total_process:%d,current_process:%d,total_output:%d,current_output:%d]",
                    "FakecarServer", totalInput, 1, totalProcess, 0, totalOutput, 0));
            return;
        }

        // 记录接收到的总记录数,在指定的时间段内
        calculatorReceiveCount(true);

        // 执行套牌计算,先生成key信息
        KeyRecord key = getKeyRecord(record.getHead_rear(), licence_plate, licence_plate_color);
        if (null == key) {
            LOG.warn("skip this record calculator,method call return..............");
            return;
        }

        int hash_code = Math.abs(key.hashCode()) % vehicles.length;
        if (process_debug) {
            LOG.info("this record hash code is " + hash_code);
        }

        if (null == vehicles[hash_code]) {
            vehicles[hash_code] = new HashMap<KeyRecord, ValueRecord>();
            if (process_debug) {
                LOG.info("new hash code [" + hash_code + "] map by hashmap.");
            }
        }

        // 执行套牌计算
        begin_time = System.nanoTime();
        calculatorFakecarBuffer(vehicles[hash_code], key, record, licence_plate_color, hash_code);
        if (exectime_debug) {
            long execute_time = System.nanoTime() - begin_time;
            LOG.info(String.format("==>calculatorFakecar receive record execute time is [%d] ns", execute_time));
        }
    }

    /**
     * 对每一个hash后的桶进行套牌计算,此部分为了考虑到线程安全问题,需要对比对进行加锁
     *
     * @param key
     * @param record
     */
    private void calculatorFakecarBuffer(Map<KeyRecord, ValueRecord> vehiclesMap,
                                         KeyRecord key, InputRecordBuffer record, byte[] licence_plate_color, int hash_code) {
        if (process_debug) {
            LOG.info("input record key is " + key.toString());
        }
        byte[] vehicle_color = record.getVehicle_color();
        byte[] vehicle_type = record.getVehicle_type();
        byte[] vehicle_logo = record.getVehicle_logo();
        byte[] vehicle_child_logo = record.getVehicle_child_logo();
        byte[] vehicle_style = record.getVehicle_style();
        byte[] record_id = record.getRecord_id();
        byte[] old_plate = record.getOld_licence_plate();
        byte[] vehicle_feature = record.getVehicle_feature();

    /*
     * 无论何时都不能把特征去掉，特征要用于找原车
     * // 0=同时满足结构化与特征比对都不相同才认定为套牌,
    // 1=满足结构化或特征比对有一样不相同就认定为套牌,
    // 2=只比对结构化的信息,
    // 3=只比对特征信息
    switch (compareRelation) {
    case 0:
    case 1:
    case 3:
      if (record.vehicle_feature != null) {
        vehicle_feature = record.getVehicle_feature();
      }
      break;
    default:
      break;
    }*/

        if (vehicleStyleEnable) {
            if (process_debug) {
                LOG.info("switch vehicleStyleEnable is enable");
            }
        }
        if (vehicleColorEnable) {
            if (process_debug) {
                LOG.info("switch vehicleColorEnable is enable");
            }
        }
        if (vehicleTypeEnable) {
            if (process_debug) {
                LOG.info("switch vehicleTypeEnable is enable");
            }
        }
        if (process_debug) {
            LOG.info("switch compareRelation is [" + compareRelation
                    + "] get all struct attribute");
        }

        ValueRecord value = new ValueRecord();
        value.setLicence_plate_color(licence_plate_color);
        value.setVehicle_color(vehicle_color);
        value.setVehicle_type(vehicle_type);
        value.setVehicle_logo(vehicle_logo);
        value.setVehicle_child_logo(vehicle_child_logo);
        value.setVehicle_style(vehicle_style);
        value.setVehicle_feature_buffer(vehicle_feature);// 二进制版本
        value.setRecord_id(record_id);
        value.setOld_licence_plate(old_plate);
        value.setKkbh(record.getTraffic_kkbh());
        value.setXzqh(record.getTraffic_xzqh());
        value.setHeadrear(Integer.valueOf(record.getHead_rear()).byteValue());

        synchronized (vehiclesMap) {
            if (!vehiclesMap.containsKey(key)) {
                if (plate_similarity_enable && value.getOld_licence_plate() != null
                        && value.getOld_licence_plate().length > 0) {
                    // 比对车牌号的相似度,如果相似度在一个指定的范围内,直接丢弃掉此条数据
                    float similarity = levenshtein(key.getLicence_plate(), value.getOld_licence_plate());

                    if (similarity < plate_similarity_min_value || similarity > 100) {
                        if (process_debug) {
                            LOG.info("plate_similarity_enable is true, skip this record plate similarity is "
                                    + similarity);
                        }
                    } else {
                        vehiclesMap.put(key, value);
                        modifyFlagSet.add(hash_code);
                        if (process_debug) {
                            LOG.info("add new vehicle by map in plate_similarity_enable, key is "
                                    + key.toString());
                        }
                    }
                } else {
                    // 不进行车牌相似度比对,直接添加到容器中
                    vehiclesMap.put(key, value);
                    modifyFlagSet.add(hash_code);
                    if (process_debug) {
                        LOG.info("add new vehicle by map in not plate_similarity_enable, key is "
                                + key.toString());
                    }
                }

                // vehiclesMap.put(key, value);
                // LOG.info("add new vehicle by map, key is " + key.toString());
            } else {
                // 执行计算
                float similarity = -1;
                ValueRecord oldValue = vehiclesMap.get(key);
                boolean flag;
                if (headrearMustSame && oldValue.getHeadrear() != value.getHeadrear()) {
                    LOG.info("headrear not same!");
                    flag = false;
                } else {
                    flag = executeBuffer(key, oldValue, value);
                }
                if (flag) {
                    if (plate_similarity_enable && value.getOld_licence_plate() != null && value.getOld_licence_plate().length > 0) {
                        similarity = levenshtein(key.getLicence_plate(), value.getOld_licence_plate());
                    }

                    if ((plate_similarity_enable && similarity >= plate_similarity_min_value && similarity <= 100)
                            || (!plate_similarity_enable && similarity <= 0)) {
                        boolean skip = false;
                        if (ByteUtils.equals(oldValue.getVehicle_logo(), value.getVehicle_logo())
                                && mutexBrand != null) {
                            for (byte[] mb : mutexBrand) {
                                if (ByteUtils.equals(mb, value.getVehicle_logo())) {
                                    LOG.info("logo is mutex brand=" + String.valueOf(mb) + ", skip++++++");
                                    skip = true;
                                    break;
                                }
                            }
                        }
                        if (!skip && MapUtils.isNotEmpty(childLogoMutexMap)) {
                            byte[] bt1 = oldValue.getVehicle_child_logo();
                            byte[] bt2 = value.getVehicle_child_logo();
                            String metuxKey = getSubBrandMutexKey(String.valueOf(ByteUtils.parseInt(bt1, 0, bt1.length)), String.valueOf(ByteUtils.parseInt(bt2, 0, bt2.length)));
                            if (metuxKey != null && childLogoMutexMap.containsKey(metuxKey)) {
                                byte[][] childLogoBts = childLogoMutexMap.get(metuxKey);
                                if ((ByteUtils.equals(bt1, childLogoBts[0]) && ByteUtils.equals(bt2, childLogoBts[1]))
                                        || (ByteUtils.equals(bt1, childLogoBts[1]) && ByteUtils.equals(bt2, childLogoBts[0]))) {
                                    LOG.info("childlogo is mutex brand=" + String.valueOf(metuxKey) + ", skip++++++");
                                    skip = true;
                                }
                            }
                        }
                        if (!skip) {
                            ResultRecord resultRecord = new ResultRecord(key, oldValue, value);
                            // TODO 写入数据库
                            if (flushThread == null) {
                                flushThread = new FeatureResultDBOutputThread(this, tableDef, trafficTableDef, flushInterval, flushNumber, cleanDay, process_debug);

                                flushThread.setDaemon(true);
                                flushThread.start();
                            }
                            synchronized (Integer.valueOf(totalOutput)) {
                                totalOutput ++;
                            }
                            LOG.info(String.format("streaming data statistics [service_name:%s,total_input:%d,current_input:%d,total_process:%d,current_process:%d,total_output:%d,current_output:%d]",
                                    "FakecarServer", totalInput, 1, totalProcess, 1, totalOutput, 1));
                            flushThread.addValue(resultRecord);
                            //找原车
                            if (similar_vehicle_search_enable) {
                      /*if (searchThread == null) {
                  		searchThread = new SimilarVehicleSearchThread(result_count, result_distence, searchDate, host, port, process_debug, result_id_only,idLength,featureSize);
                  		searchThread.setDaemon(true);
                  		searchThread.start();
                  	}*/
                                searchThread.addValue(resultRecord);
                            }

                            if (process_debug) {
                                LOG.info("fakecar calculator result key is " + key.toString());
                            }
                        }
                    } else {
                        if (process_debug) {
                            LOG.info("plate_similarity_enable is true, skip this compact plate similarity is "
                                    + similarity);
                        }
                    }//end plate_similarity_min_value if
                } else {
                    LOG.info(String.format("streaming data statistics [service_name:%s,total_input:%d,current_input:%d,total_process:%d,current_process:%d,total_output:%d,current_output:%d]",
                            "FakecarServer", totalInput, 1, totalProcess, 1, totalOutput, 0));
                }

                if (!plate_similarity_enable) {
                    // 更新map中的内容,不执行车牌相似度比对
                    if (!headrearMustSame || value.getHeadrear() == 0) {
                        vehiclesMap.put(key, value);
                        modifyFlagSet.add(hash_code);
                    }
                } else {
                    if ((similarity < plate_similarity_min_value || similarity >= 100) && (!headrearMustSame || value.getHeadrear() == 0)) {
                        // 更新map中的内容,执行车牌相似度比对,并不在指定的相似范围内
                        vehiclesMap.put(key, value);
                        modifyFlagSet.add(hash_code);
                    }
                }// end if plate_similarity
            }
        }// end if else
    }

    /**
     * 执行套牌计算,是套牌返回true,否则返回false
     *
     * @param oldValue
     * @param newValue
     * @return
     */
    private boolean executeBuffer(KeyRecord key, ValueRecord oldValue,
                                  ValueRecord newValue) {
        boolean flag = false;
        switch (compareRelation) {
            case 0:
                // 先比对结构化信息数据,如果结构化信息数据不相同,再比对特征向量
                if (!compareStruct(key, oldValue, newValue)
                        && !compareFeatureBuffer(key, oldValue, newValue)) {
                    flag = true;
                }
                break;
            case 1:
                // 比对结构化信息数据与特征向量相似度,如果有一个不相同,表示套牌
                if (!compareStruct(key, oldValue, newValue)
                        || !compareFeatureBuffer(key, oldValue, newValue)) {
                    flag = true;
                }
                break;
            case 2:
                // 只比对结构化数据
                if (!compareStruct(key, oldValue, newValue)) {
                    flag = true;
                }
                break;
            case 3:
                // 只比对特征相似度
                if (!compareFeatureBuffer(key, oldValue, newValue)) {
                    flag = true;
                }
                break;
            default:
                break;
        }

        return flag;
    }

    /**
     * 比对两条记录的特征是否相似,相似度的阀值通过fakecar.feature.max.distance配置,默认值38<br/>
     * 返回true表示特征相似,返回false表示特征距离超过阀值
     *
     * @param oldValue
     * @param newValue
     * @return
     */
    private boolean compareFeatureBuffer(KeyRecord key, ValueRecord oldValue,
                                         ValueRecord newValue) {
        long begin_time = System.nanoTime();

        byte[] feature1 = oldValue.getVehicle_feature_buffer();
        byte[] feature2 = newValue.getVehicle_feature_buffer();
        boolean flag = true;

        if (feature1 == feature2) {
            if (process_debug) {
                LOG.info("compare Vehicle_feature result is equals, return true");
            }
            return true;
        }
        if (feature1 == null || feature2 == null) {
            if (process_debug) {
                LOG.info("compare Vehicle_feature is null, return false");
            }
            return false;
        }

        // TODO 此处进行距离计算,
        float distance = 0;
        if (maxDistance > 55) {
            distance = CalcHammingDistUtil.CalcHammingDist(feature1, feature2, dissimilarity_count);
        } else {
            distance = CalcHammingDistUtil.CalcHammingDist(feature1, feature2);
        }

        // 比对相似度是否超过指定的阀值,此处修改成了评分的方式,分数越高,是同一款车的概率越大,
        if (distance < maxDistance) {
            flag = false;
        }
        if (exectime_debug) {
            long execute_time = System.nanoTime() - begin_time;
            LOG.info(String.format(
                    "==>compareFeatureBuffer receive record execute time is [%d] ns",
                    execute_time));
        }
        if (process_debug) {
            LOG.info("compare key " + key.toString() + " feature distance is "
                    + distance);
        }
        return flag;
    }

    /**
     * 把接收到的InputRecord转换成uft-8的字符串
     *
     * @param record
     * @return
     */
    private String toString(InputRecordBuffer record) {
        StringBuilder sb = new StringBuilder("InputRecord(");
        boolean first = true;

        sb.append("licence_plate:");
        if (record.licence_plate == null) {
            sb.append("null");
        } else {
            sb.append(new String(record.getLicence_plate(), charset));
        }
        first = false;
        if (!first)
            sb.append(", ");
        sb.append("licence_plate_color:");
        if (record.licence_plate_color == null) {
            sb.append("null");
        } else {
            sb.append(new String(record.getLicence_plate_color(), charset));
        }
        first = false;
        if (!first)
            sb.append(", ");
        sb.append("vehicle_type:");
        if (record.vehicle_type == null) {
            sb.append("null");
        } else {
            sb.append(new String(record.getVehicle_type(), charset));
        }
        first = false;
        if (!first)
            sb.append(", ");
        sb.append("vehicle_color:");
        if (record.vehicle_color == null) {
            sb.append("null");
        } else {
            sb.append(new String(record.getVehicle_color(), charset));
        }
        first = false;
        if (!first)
            sb.append(", ");
        sb.append("vehicle_logo:");
        if (record.vehicle_logo == null) {
            sb.append("null");
        } else {
            sb.append(new String(record.getVehicle_logo(), charset));
        }
        first = false;
        if (!first)
            sb.append(", ");
        sb.append("logo_confidence:");
        sb.append(record.logo_confidence);

        first = false;
        if (!first)
            sb.append(", ");
        sb.append("vehicle_child_logo:");
        if (record.vehicle_child_logo == null) {
            sb.append("null");
        } else {
            sb.append(new String(record.getVehicle_child_logo(), charset));
        }
        first = false;
        if (!first)
            sb.append(", ");
        sb.append("vehicle_style:");
        if (record.vehicle_style == null) {
            sb.append("null");
        } else {
            sb.append(new String(record.getVehicle_style(), charset));
        }
        first = false;
        if (!first)
            sb.append(", ");
        sb.append("record_id:");
        if (record.record_id == null) {
            sb.append("null");
        } else {
            sb.append(new String(record.getRecord_id(), charset));
        }
        first = false;
        if (!first)
            sb.append(", ");
        sb.append("source_type:");
        sb.append(record.source_type);
        first = false;
        if (!first)
            sb.append(", ");
        sb.append("confidence_level:");
        sb.append(record.confidence_level);

        first = false;
        if (!first)
            sb.append(", ");
        sb.append("plate_confidence:");
        sb.append(record.plate_confidence);
        first = false;
        if (!first)
            sb.append(", ");
        sb.append("vehicle_left_x:");
        sb.append(record.vehicle_left_x);
        first = false;
        if (!first)
            sb.append(", ");
        sb.append("vehicle_left_y:");
        sb.append(record.vehicle_left_y);
        first = false;
        if (!first)
            sb.append(", ");
        sb.append("vehicle_rigth_x:");
        sb.append(record.vehicle_rigth_x);
        first = false;
        if (!first)
            sb.append(", ");
        sb.append("vehicle_rigth_y:");
        sb.append(record.vehicle_rigth_y);
        first = false;
        if (!first)
            sb.append(", ");
        sb.append("old_licence_plate:");
        if (record.old_licence_plate == null) {
            sb.append("null");
        } else {
            sb.append(new String(record.getOld_licence_plate(), charset));
        }
        first = false;
        if (!first)
            sb.append(", ");
        sb.append("traffic_time:");
        if (record.traffic_time == null) {
            sb.append("null");
        } else {
            sb.append(new String(record.getTraffic_time(), charset));
        }

        first = false;
        if (!first)
            sb.append(", ");
        sb.append("vehicle_feature:");
        if (record.vehicle_feature == null) {
            sb.append("null");
        } else {
            org.apache.thrift.TBaseHelper.toString(record.vehicle_feature, sb);
        }
        first = false;
        if (!first)
            sb.append(", ");
        sb.append("head_rear:").append(String.valueOf(record.head_rear));
        first = false;
        sb.append(")");
        return sb.toString();
    }

}
