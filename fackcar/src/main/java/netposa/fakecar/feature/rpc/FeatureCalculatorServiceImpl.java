package netposa.fakecar.feature.rpc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import netposa.fakecar.feature.rpc.bdk.BdkCheckProcessor;
import netposa.fakecar.feature.rpc.bdk.SimilarVehicleHttpServer;
import netposa.fakecar.feature.rpc.error.InsertErrorSqlToDbThread;
import netposa.fakecar.feature.rpc.search.SimilarVehicleSearchThread;
import netposa.fakecar.feature.rpc.util.ByteUtils;
import netposa.fakecar.feature.rpc.util.CalcHammingDistUtil;
import netposa.fakecar.feature.rpc.util.CleanErrorKeyHttpServer;
import netposa.fakecar.feature.rpc.util.DateFormatDefine;
import netposa.fakecar.feature.rpc.util.FeatureResultDBOutputThread;
import netposa.fakecar.feature.rpc.util.FeatureTableConfLoadUtils;
import netposa.fakecar.feature.rpc.util.FeatureTableDef;
import netposa.fakecar.feature.rpc.util.TrafficTableConfLoadUtils;
import netposa.fakecar.feature.rpc.util.TrafficTableDef;

import org.apache.commons.lang.StringUtils;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 定义rpc的数据接收接口,用于接收pcc的二次识别服务推送过来的数据,并执行套牌比较操作
 *
 * @author hongs.yang
 */
public abstract class FeatureCalculatorServiceImpl implements
        FeatureCalculatorService.Iface {
    private static final Logger LOG = LoggerFactory
            .getLogger(FeatureCalculatorServiceImpl.class);

    // 应用程序编码格式
    protected static final Charset charset = Charset.forName("UTF-8");
    protected static final byte symbol = '|';

    // 配置文件的名称
    public static final String conf_name = "fakecar_config.properties";

    // 车牌排除条件定义
    protected byte[][] startWith;
    protected byte[][] endWith;
    protected byte[][] contains;
    protected byte[][] equals;

    protected byte[][] mutexBrand;

    // 车牌颜色排除条件定义
    protected byte[][] color_equals;

    // 是否只启用了本地车牌比对功能
    protected boolean onlyLocalEnable = false;
    // 本地牌照的前缀,如果为空,不做控制
    protected byte[] localPlateStartWith;

    protected boolean isLogoConfidenceEnable = true;
    protected double logoConfidenceMinValue = 98;
    // 是否启用对二次识别置信度的过滤
    protected boolean isConfidenceLevelEnable = false;
    // 过滤置信度的最小置信度的值
    protected double confidenceLevelMinValue = 80;

    // 对结构化的二次识别信息比对时是否启用车辆颜色与车辆类型/车辆年款属性
    protected boolean vehicleColorEnable = false;
    protected boolean vehicleTypeEnable = false;
    protected boolean vehicleStyleEnable = false;

    // 是否开启车头车尾分离对比
    protected boolean frontTailSeparate = false;

    public long dumpPeriodTime = 600000;

    // 套牌分析中key的规则,0表示车牌+车牌颜色;1表示只使用车牌
    protected byte keyRelation = 0;

    // 套牌分析中比对规则的定义,
    // 0=同时满足结构化与特征比对都不相同才认定为套牌,
    // 1=满足结构化或特征比对有一样不相同就认定为套牌,
    // 2=只比对结构化的信息,
    // 3=只比对特征信息
    protected byte compareRelation = 2;
    // 特征相似度阀值
    protected float maxDistance = 38;
    public final int dissimilarity_count = CalcHammingDistUtil.getDistenceDic(maxDistance);

    // 数据来源的排除规则,0=不排除任何数据,1=排除交警(电警)卡口数据,2=排除公安卡口数据
    protected byte excluteDataSource = 1;
    // 交警(电警)卡口数据来源的类型
    protected byte traffic_checkpoint_type = 1;
    // 公安卡口数据来源的类型
    protected byte other_checkpoint_type = 0;

    // 上一次输出接收到的总记录数的时间,更新频率通过task.receive.count.interval.minute配置
    protected long countInterval = 10 * 60 * 1000;
    // 是否启用接收到的记录统计
    protected boolean countIntervalEnable = false;
    // 上一次输出接收到的总记录数的时间
    protected long lastCountTime = 0;
    // 在一个打印的输出间隔中,接收到的数据总量值
    protected long receiveCount = 0;
    // 线程安全锁
    protected Lock lock = new ReentrantLock(false);

    // 是否打印对应的接收数据与执行时间的debug信息
    protected boolean receive_debug = false;
    protected boolean exectime_debug = false;
    protected boolean process_debug = false;

    // 向数据库写入的表配置信息
    protected FeatureTableDef tableDef;
    protected TrafficTableDef trafficTableDef;
    protected int flushInterval = 3 * 1000;
    protected int flushNumber = 100;
    protected int cleanDay = -1;// 删除缓存中过期的车牌数据的时长
    protected FeatureResultDBOutputThread flushThread;

    protected boolean enableNative = true;// 是否启用本地库

    protected final int hash_size = 16384;

    //车头
    protected byte head_rear_head = 0;
    //车尾
    protected byte head_rear_rear = 1;
    //未识别
    protected byte head_rear_unknown = -1;

    @SuppressWarnings("unchecked")
    public Map<KeyRecord, ValueRecord>[] vehicles = new Map[hash_size];

    public Set<Integer> modifyFlagSet = new HashSet<Integer>();

    // 排除小时规则的定义信息
    protected Map<DateFormatDefine, int[]> date_pattern = null;
    protected boolean enable_hour_flag = false;
    protected int begin_hour = 19;
    protected int end_hour = 6;

    //排除二次识别车牌与原始车牌相似的数据
    protected boolean plate_similarity_enable = false;
    protected float plate_similarity_min_value = 70;

    //排除车牌置信度的配置
    protected boolean plate_confidence_enable = true;
    protected float plate_confidence_minvalue = 80;

    //排除车辆在图像边界的数据配置
    protected boolean border_filter_enable = false;
    protected float border_other_checkpoint_min = 0.9f;
    protected float border_other_checkpoint_max = 1.1f;
    //电警的边界确定,默认情况下不启用,
    protected float border_traffic_checkpoint_min = 1.0f;
    protected float border_traffic_checkpoint_max = 1.2f;

    //品牌排除
    protected boolean logo_exclude_enable = true;
    protected byte[][] logo_exclude;
    protected boolean childlogo_exclude_enable = true;
    protected byte[][] childlogo_exclude;

    //特征搜车
    public boolean similar_vehicle_search_enable = true;
    public SimilarVehicleSearchThread searchThread;
    protected InsertErrorSqlToDbThread errorThread;
    //protected Map<String,String> vehicleColor;
    //protected Map<String,String> vehicleBrand;
    //protected SimilarVehicleTableDef svTableDef;
    protected String host = "0.0.0.0";
    protected int port = 30056;
    protected int result_count = 50;
    protected int result_distence = 55;
    protected int searchDate = 50;
    protected boolean result_id_only = false;
    protected int idLength = 20;
    protected int featureSize = 288;

    //是否开启八大库对比
    protected boolean bdk_check_enable = false;

    //是否开启子品牌名称相似度过滤
    protected boolean subbrandname_filter_enable = true;
    //车辆子品牌字典表
    Map<String, String> vehicleSubBrandMap = new HashMap<String, String>();
    //车辆子品牌组过滤
    Map<String, byte[][]> childLogoMutexMap = new HashMap<String, byte[][]>();
    //dump文件的存储路径
    public String dumpDirPath = null;

    //headrear必须相同过滤
    public boolean headrearMustSame = false;

    protected int totalInput = 0;
    protected int totalProcess = 0;

    protected boolean isCompareChildlogo = true;

    /**
     * 接收PCC服务传过来的信息,并执行清洗与套牌车计算
     */
    @Override
    public void sendRecord(InputRecord record) throws TException {
        // 对接收到的数据输出到日志中
        if (receive_debug) {
            LOG.info(String.format("input record data is => {%s}", toString(record)));
        }

        byte[] licence_plate = record.getLicence_plate();
        byte[] licence_plate_color = record.getLicence_plate_color();
        double input_confidence_level = record.getConfidence_level();

        float face_value = 0.0f;
        try {

            int height = record.getVehicle_rigth_y() - record.getVehicle_left_y();
            int weight = record.getVehicle_rigth_x() - record.getVehicle_left_x();

            if (height > 0 && weight > 0) {
                face_value = weight / height;
            }

            //face_value = FeatureJsonParseUtils.parseVehicleFacePoint(buffer);
        } catch (Exception e) {
            LOG.warn("parse vehicle face point error => " + e.getMessage(), e);
        }

        // 对数据进行清洗
        long begin_time = System.nanoTime();
        boolean exclude = transform(licence_plate, licence_plate_color,
                record.getSource_type(), input_confidence_level,
                record.getTraffic_time(), record.getPlate_confidence(), record.getLogo_confidence(), null, face_value, record.getVehicle_logo(), record.getVehicle_child_logo(),
                record.getHead_rear());
        if (exectime_debug) {
            long execute_time = System.nanoTime() - begin_time;
            LOG.info(String.format(
                    "==>transform receive record execute time is [%d] ns", execute_time));
        }

        // 如果数据需要被排除,不执行后面的流程,直接结束当前函数
        if (exclude) {
            if (process_debug) {
                LOG.info("process stop, this record transform exclude,"
                        + " this method call return......");
            }
            // 记录接收到的总记录数,在指定的时间段内
            calculatorReceiveCount(false);
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
        calculatorFakecar(vehicles[hash_code], key, record, licence_plate_color);
        if (exectime_debug) {
            long execute_time = System.nanoTime() - begin_time;
            LOG.info(String.format(
                    "==>calculatorFakecar receive record execute time is [%d] ns",
                    execute_time));
        }
    }

    public KeyRecord getKeyRecord(int head_rear, byte[] licence_plate, byte[] licence_plate_color) {
        KeyRecord key = null;
        if (frontTailSeparate) {
            switch (keyRelation) {
                case 0:
                    // 此处是车牌号+车牌颜色共同组成key
                    key = new KeyRecord(licence_plate, licence_plate_color, head_rear);
                    break;
                case 1:
                    // 此处是只用车牌号组成key
                    key = new KeyRecord(licence_plate, head_rear);
                    break;
                default:
                    break;
            }
        } else {
            switch (keyRelation) {
                case 0:
                    // 此处是车牌号+车牌颜色共同组成key
                    key = new KeyRecord(licence_plate, licence_plate_color);
                    break;
                case 1:
                    // 此处是只用车牌号组成key
                    key = new KeyRecord(licence_plate);
                    break;
                default:
                    break;
            }
        }
        return key;
    }

    public FeatureCalculatorServiceImpl() throws Exception {
        LOG.info("handler prepard begin ....................");
        InputStream inStream = null;
        try {
            LOG.info("handler load config properties file by class path");
            inStream = this.getClass().getClassLoader()
                    .getResourceAsStream(conf_name);

            Properties props = new Properties();
            props.load(inStream);

            isCompareChildlogo = Boolean.parseBoolean(props.getProperty("fakecar.child_logo.compare.enable"));

            LOG.info("resolve config file value to the class attribute");

            // ============================================================================================//
            loadLicencePlateByProps(props);
            // ============================================================================================//
            loadVehicleLogoExcludeProps(props);
            // ============================================================================================//
            loadLicencePlateColorByProps(props);
            // ============================================================================================//
            loadDebugByProps(props);
            // ============================================================================================//
            loadStructCompareRuleByProps(props);
            // ============================================================================================//
            loadDataSourceConfByProps(props);
            // ============================================================================================//
            loadThreadConfByProps(props);
            // ============================================================================================//
            loadHourExcludeConfByProps(props);
            // ============================================================================================//
            loadBorderExcludeConfByProps(props);
            // ============================================================================================//
            loadSimilarVehicleSearchServiceByProps(props);
            // =============================load config attribute

            //load VehicleSubBrand
            loadVehicleSubBrand();

            //load SubBrandMetux
            loadSubBrandMutex();
            // end======================================//

        } finally {
            if (null != inStream) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    LOG.warn("close stream error => " + e.getMessage(), e);
                }
            }
        }

        LOG.info("init hash map by partition 16384.....");
        for (int i = 0; i < vehicles.length; i++) {
            vehicles[i] = new HashMap<KeyRecord, ValueRecord>();
        }

        LOG.info("load feature table config file by class path");
        tableDef = FeatureTableConfLoadUtils.loadConfigByClassPath();

        //加载OPAQ配置
        trafficTableDef = TrafficTableConfLoadUtils.loadConfigByClassPath(tableDef);

        LOG.info("init result flush to database thread......");
        flushThread = new FeatureResultDBOutputThread(this, tableDef, trafficTableDef, flushInterval, flushNumber, cleanDay, process_debug);
        flushThread.setDaemon(true);
        flushThread.start();
        LOG.info("init result flush to database thread......");

        //找原车
        if (similar_vehicle_search_enable) {
            LOG.info("init similar vehicle search enable ......");
            searchThread = new SimilarVehicleSearchThread(result_count, result_distence, searchDate, host, port, process_debug, result_id_only, idLength, featureSize);
            searchThread.setDaemon(true);
            searchThread.start();
        }

        errorThread = new InsertErrorSqlToDbThread(searchThread, true);
        errorThread.setDaemon(true);
        errorThread.start();

        LOG.info("handler prepard end   ....................");
    }

    /**
     * 对每一个hash后的桶进行套牌计算,此部分为了考虑到线程安全问题,需要对比对进行加锁
     *
     * @param key
     * @param record
     */
    private void calculatorFakecar(Map<KeyRecord, ValueRecord> vehiclesMap,
                                   KeyRecord key, InputRecord record, byte[] licence_plate_color) {
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
        float[] vehicle_feature = null;

        // 0=同时满足结构化与特征比对都不相同才认定为套牌,
        // 1=满足结构化或特征比对有一样不相同就认定为套牌,
        // 2=只比对结构化的信息,
        // 3=只比对特征信息
        switch (compareRelation) {
            case 0:
            case 1:
            case 3:
                if (record.vehicle_feature != null) {
                    vehicle_feature = new float[record.vehicle_feature.size()];
                    for (int i = 0; i < vehicle_feature.length; i++) {
                        vehicle_feature[i] = record.vehicle_feature.get(i).floatValue();
                    }
                }
                break;
            default:
                break;
        }

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
        value.setVehicle_feature(vehicle_feature);
        value.setRecord_id(record_id);
        value.setOld_licence_plate(old_plate);

        synchronized (vehiclesMap) {
            if (!vehiclesMap.containsKey(key)) {
                if (plate_similarity_enable && value.getOld_licence_plate() != null
                        && value.getOld_licence_plate().length > 0) {
                    //比对车牌号的相似度,如果相似度在一个指定的范围内,直接丢弃掉此条数据
                    float similarity = levenshtein(key.getLicence_plate(), value.getOld_licence_plate());
                    if (similarity >= plate_similarity_min_value && similarity < 100) {
                        if (process_debug) {
                            LOG.info("plate_similarity_enable is true, skip this record plate similarity is " + similarity);
                        }
                    } else {
                        vehiclesMap.put(key, value);
                        if (process_debug) {
                            LOG.info("add new vehicle by map in plate_similarity_enable, key is " + key.toString());
                        }
                    }
                } else {
                    //不进行车牌相似度比对,直接添加到容器中
                    vehiclesMap.put(key, value);
                    if (process_debug) {
                        LOG.info("add new vehicle by map in not plate_similarity_enable, key is " + key.toString());
                    }
                }
            } else {
                // 执行计算
                float similarity = -1;
                ValueRecord oldValue = vehiclesMap.get(key);
                boolean flag = execute(key, oldValue, value);
                if (flag) {
                    if (plate_similarity_enable && value.getOld_licence_plate() != null
                            && value.getOld_licence_plate().length > 0) {
                        similarity = levenshtein(key.getLicence_plate(),
                                value.getOld_licence_plate());
                    }

                    if (similarity < plate_similarity_min_value || similarity >= 100) {
                        // TODO 写入数据库
                        if (flushThread == null) {
                            flushThread = new FeatureResultDBOutputThread(this, tableDef, trafficTableDef,
                                    flushInterval, flushNumber, cleanDay, process_debug);

                            flushThread.setDaemon(true);
                            flushThread.start();
                        }
                        flushThread.addValue(new ResultRecord(key, oldValue, value));

                        if (process_debug) {
                            LOG.info("fakecar calculator result key is " + key.toString());
                        }
                    } else {
                        if (process_debug) {
                            LOG.info("plate_similarity_enable is true, skip this compact plate similarity is " + similarity);
                        }
                    }
                }

                if (!plate_similarity_enable) {
                    // 更新map中的内容,不执行车牌相似度比对
                    vehiclesMap.put(key, value);
                } else {
                    if (similarity < plate_similarity_min_value || similarity >= 100) {
                        // 更新map中的内容,执行车牌相似度比对,并不在指定的相似范围内
                        vehiclesMap.put(key, value);
                    }
                }//end if plate_similarity
            }
        }
    }

    /**
     * 执行套牌计算,是套牌返回true,否则返回false
     *
     * @param oldValue
     * @param newValue
     * @return
     */
    private boolean execute(KeyRecord key, ValueRecord oldValue,
                            ValueRecord newValue) {
        boolean flag = false;
        switch (compareRelation) {
            case 0:
                // 先比对结构化信息数据,如果结构化信息数据不相同,再比对特征向量
                if (!compareStruct(key, oldValue, newValue)
                        && !compareFeature(key, oldValue, newValue)) {
                    flag = true;
                }
                break;
            case 1:
                // 比对结构化信息数据与特征向量相似度,如果有一个不相同,表示套牌
                if (!compareStruct(key, oldValue, newValue)
                        || !compareFeature(key, oldValue, newValue)) {
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
                if (!compareFeature(key, oldValue, newValue)) {
                    flag = true;
                }
                break;
            default:
                break;
        }
        return flag;
    }

    /**
     * 比对两条记录的特征是否相似,相似度的阀值通过fakecar.feature.max.distance配置,默认值250<br/>
     * 返回true表示特征相似,返回false表示特征距离超过阀值
     *
     * @param oldValue
     * @param newValue
     * @return
     */
    private boolean compareFeature(KeyRecord key, ValueRecord oldValue,
                                   ValueRecord newValue) {
        long begin_time = System.nanoTime();

        float[] feature1 = oldValue.getVehicle_feature();
        float[] feature2 = newValue.getVehicle_feature();
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
        if (feature1.length != feature2.length) {
            if (process_debug) {
                LOG.info("compare Vehicle_feature length not equals, return false");
            }
            return false;
        }
        if (feature1.length == 0) {
            if (process_debug) {
                LOG.info("compare Vehicle_feature length is 0, return true");
            }
            return true;
        }
        // TODO 此处进行距离计算
        // dist(feature1, feature2) = sqrt( (a1-b1)*(a1-b1) + (a2-b2)*(a2-b2) ... +
        // (aN-bN)*(aN-bN) )
        double distance = 0;
        for (int i = 0; i < feature1.length; i++) {
            distance += ((feature1[i] - feature2[i]) * (feature1[i] - feature2[i]));
        }
        distance = Math.sqrt(distance);

        // 比对相似度是否超过指定的阀值
        if (distance > maxDistance) {
            flag = false;
        }
        if (exectime_debug) {
            long execute_time = System.nanoTime() - begin_time;
            LOG.info(String.format(
                    "==>compareFeature receive record execute time is [%d] ns",
                    execute_time));
        }
        if (process_debug) {
            LOG.info("compare key " + key.toString() + " feature distance is "
                    + distance);
        }
        return flag;
    }

    /**
     * 比较结构化信息,返回true表示比对结果相同,flase表示比对结果不相同
     *
     * @param oldValue
     * @param newValue
     * @return
     */
    protected boolean compareStruct(KeyRecord key, ValueRecord oldValue,
                                    ValueRecord newValue) {
        long begin_time = System.nanoTime();

        boolean flag = true;
        // 比较车辆品牌与子品牌,必须项
        flag = ByteUtils.equals(oldValue.getVehicle_logo(),
                newValue.getVehicle_logo());
        if (process_debug) {
            LOG.info("compare Vehicle_logo result is " + flag);
        }

        if (flag) {
            if (isCompareChildlogo) {
                flag = ByteUtils.equals(oldValue.getVehicle_child_logo(),
                        newValue.getVehicle_child_logo());
                if (process_debug) {
                    LOG.info("compare Vehicle_child_logo result is " + flag);
                }

                if (subbrandname_filter_enable && oldValue.getVehicle_child_logo() != null && newValue.getVehicle_child_logo() != null && !flag) {
                    String oldChildLogoName = vehicleSubBrandMap.get(new String(oldValue.getVehicle_child_logo(), charset));
                    String newChildLogoName = vehicleSubBrandMap.get(new String(newValue.getVehicle_child_logo(), charset));
                    if (oldChildLogoName != null && newChildLogoName != null) {
                        if (oldChildLogoName.equals(newChildLogoName)) {
                            flag = true;
                        } else if (oldChildLogoName.contains(newChildLogoName)) {
                            flag = true;
                        } else if (newChildLogoName.contains(oldChildLogoName)) {
                            flag = true;
                        }
                    }
                    if (process_debug) {
                        LOG.info("compare Vehicle_child_logo by name result is " + flag);
                    }
                }
                //判断子品牌名称相似度，避免pcc在相近子品牌之间识别错误

                if (process_debug) {
                    LOG.info("compare Vehicle_child_logo result is " + flag);
                }
            }
        }
        // 如果需要比对年款,对年款进行包含的比对
        if (flag && vehicleStyleEnable) {
            flag = checkVehicleStyle(oldValue.getVehicle_style(),
                    newValue.getVehicle_style());
            if (process_debug) {
                LOG.info("compare Vehicle_style result is " + flag);
            }
        }
        // 如果需要比对车辆类型,对车辆类型进行比对
        if (flag && vehicleTypeEnable) {
            flag = ByteUtils.equals(oldValue.getVehicle_type(),
                    newValue.getVehicle_type());
            if (process_debug) {
                LOG.info("compare Vehicle_type result is " + flag);
            }
        }
        // 如果需要比对车辆颜色,执行如下执行
        if (flag && vehicleColorEnable) {
            flag = ByteUtils.equals(oldValue.getVehicle_color(),
                    newValue.getVehicle_color());
            if (process_debug) {
                LOG.info("compare Vehicle_color result is " + flag);
            }
        }

        if (exectime_debug) {
            long execute_time = System.nanoTime() - begin_time;
            LOG.info(String.format(
                    "==>compareStruct receive record execute time is [%d] ns",
                    execute_time));
        }
        if (process_debug) {
            LOG.info("compare key " + key.toString() + " struct result is " + flag);
        }

        return flag;
    }

    /**
     * 检查车辆年款是否包含,true表示包含,false表示不包含
     *
     * @param first
     * @param second
     * @return
     */
    private boolean checkVehicleStyle(byte[] first, byte[] second) {
        if (first == second)
            return true;
        if (first == null || second == null)
            return false;
        for (int i = 0; i < first.length; ) {
            if (first[i] == symbol) {
                i += 1;
                continue;
            }
            int len = 0;
            for (int j = i; j < first.length; j++) {
                if (first[j] == symbol) {
                    break;
                }
                len++;
            }
            if (len > 0) {
                for (int x = 0; x < second.length; ) {
                    if (second[x] == symbol) {
                        x += 1;
                        continue;
                    }
                    int x_len = 0;
                    for (int y = x; y < second.length; y++) {
                        if (second[y] == symbol) {
                            break;
                        }
                        x_len++;
                    }
                    if (x_len > 0) {
                        boolean flag = ByteUtils.compareTo(first, i, len, second, x, x_len) == 0;
                        if (flag) {
                            return true;
                        }
                        x += x_len;
                    } else {
                        x++;
                    }
                    if (x >= second.length - 1) {
                        break;
                    }
                }// end for x
                i += len;
            } else {
                i++;
            }
            if (i >= first.length - 1) {
                break;
            }
        }// end for i
        return false;
    }

    /**
     * 八大库识别清洗
     *
     * @param licence_plate       号牌号码
     * @param licence_plate_color 号牌颜色
     * @return
     */
    protected boolean bdkTransform(byte[] licence_plate, byte[] licence_plate_color) {
        try {
            if (!ByteUtils.startsWith(licence_plate, localPlateStartWith)) {
                return true;
            }
            if (licence_plate == null || licence_plate.length == 0) {
                return true;
            }
            if (licence_plate_color == null || licence_plate_color.length == 0) {
                return true;
            }
        } catch (Exception e) {
            LOG.warn("exclude error => " + e.getMessage(), e);
            return true;
        }

        return false;
    }

    /**
     * 计算传入数据是否是需要排除计算的数据,(车牌/车牌颜色/数据来源)如果是返回true,否则返回false
     *
     * @param licence_plate        号牌号码
     * @param licence_plate_color  号牌颜色
     * @param data_source          数据来源
     * @param inputConfidenceLevel 整体置信度
     * @param traffic_time         经过时间
     * @param plateConfidence      号牌置信度
     * @param logoConfidence       品牌置信度
     * @param buffer               特征
     * @param face_value           长宽比例
     * @param logo                 品牌
     * @param child_logo           子品牌
     * @return
     */
    protected boolean transform(byte[] licence_plate, byte[] licence_plate_color,
                                byte data_source, double inputConfidenceLevel, byte[] traffic_time,
                                double plateConfidence, double logoConfidence, byte[] buffer, float face_value, byte[] logo, byte[] child_logo,
                                int head_rear) {
        try {
            // 对车牌排除条件进行比对========================================
            if (licence_plate == null || licence_plate.length == 0) {
                return true;
            }
            //号牌颜色为空过滤掉
            if (licence_plate_color == null || licence_plate_color.length == 0) {
                return true;
            }
            //品牌子品牌为空过滤掉
            if (logo == null || logo.length <= 0 || child_logo == null || child_logo.length <= 0) {
                return true;
            }
            //特征为空过滤掉
            if ((compareRelation == 0 || compareRelation == 3) && (buffer == null || buffer.length <= 0)) {
                return true;
            }
            // 如果启用了本地车牌比对,首先看是否是本地车辆
            if (onlyLocalEnable && localPlateStartWith != null
                    && localPlateStartWith.length > 0) {
                // 如果不是本地车牌,直接过滤掉此条数据
                if (!ByteUtils.startsWith(licence_plate, localPlateStartWith)) {
                    return true;
                }
            }

            // 是否启用了品牌置信度的检查,如果启用,检查传入的置信度是否大于最小置信度
            if (isLogoConfidenceEnable && logoConfidence >= 0) {
                // 最小置信度大于传入的值,直接过滤掉此条数据
                if (logoConfidenceMinValue > logoConfidence) {
                    return true;
                }
            }

            // 是否启用了置信度的检查,如果启用,检查传入的置信度是否大于最小置信度
            if (isConfidenceLevelEnable && inputConfidenceLevel >= 0) {
                // 最小置信度大于传入的值,直接过滤掉此条数据
                if (confidenceLevelMinValue > inputConfidenceLevel) {
                    return true;
                }
            }

            //是否启用了车牌的置信度的检查,如果启用,检查传入的置信度是否大于最小的置信度设置
            if (plate_confidence_enable && plateConfidence >= 0) {
                // 最小置信度大于传入的值,直接过滤掉此条数据
                if (plate_confidence_minvalue > plateConfidence) {
                    return true;
                }
            }

            // 是否启用了夜间时间段的过滤,如果启用,检查小时是否在指定的时间范围内
            if (enable_hour_flag && date_pattern != null) {
                if (traffic_time != null && traffic_time.length > 0) {
                    try {
                        int hour = ByteUtils.parseBytesHour(traffic_time, date_pattern);

                        if (hour >= 0) {
                            if (hour >= begin_hour || hour < end_hour) {
                                return true;
                            }
                        } else {
                            LOG.warn("parse traffic time hour integer error, input value is "
                                    + new String(traffic_time));
                        }
                    } catch (Exception e) {
                        LOG.warn(
                                "parse traffic time hour integer error, input value is '"
                                        + new String(traffic_time) + "',error msg is "
                                        + e.getMessage(), e);
                    }
                }
            }

            if (startWith != null) {
                for (byte[] tmp : startWith) {
                    if (ByteUtils.startsWith(licence_plate, tmp)) {
                        return true;
                    }
                }
            }
            if (contains != null) {
                for (byte[] tmp : contains) {
                    if (ByteUtils.contains(licence_plate, tmp)) {
                        return true;
                    }
                }
            }
            if (equals != null) {
                for (byte[] tmp : equals) {
                    if (ByteUtils.equals(licence_plate, tmp)) {
                        return true;
                    }
                }
            }
            if (endWith != null) {
                for (byte[] tmp : endWith) {
                    if (ByteUtils.endsWith(licence_plate, tmp)) {
                        return true;
                    }
                }
            }
            // 车牌比对部分结束========================================

            // 对车牌颜色部分进行比对===================================
            if (color_equals != null) {
                for (byte[] tmp : color_equals) {
                    if (ByteUtils.equals(licence_plate_color, tmp)) {
                        return true;
                    }
                }
            }
            // 车牌颜色比对部分结束=====================================

            // 对数据来源进行比对=======================================
            //只有当不开启车头车尾分别对比时，才会根据数据来源过滤
            if (!frontTailSeparate) {
                // TODO 此处需要先确定数据来源的值具体是什么值
                switch (excluteDataSource) {
                    case 1:
                        // 只保留公安卡口数据,过滤掉交警/电警等其它卡口数据
                        if (data_source != other_checkpoint_type) {
                            return true;
                        }
                        break;
                    case 2:
                        // 只保留交警/电警卡口数据,过滤掉公安等其它卡口数据
                        if (data_source != traffic_checkpoint_type) {
                            return true;
                        }
                        break;
                    default:
                        break;
                }
            } else {
                //如果开启车头车尾识别，那么如果是未识别车头车尾的，直接过滤掉
                if (head_rear <= head_rear_unknown) {
                    return true;
                }
            }
            // 数据来源比对部分结束=====================================

            // 对车辆图像是否完整进行过滤====================================
            if (border_filter_enable && buffer != null && buffer.length > 0) {
                switch (excluteDataSource) {
                    case 1:
                        //保留前拍数据
                        if (face_value < border_other_checkpoint_min
                                || face_value > border_other_checkpoint_max) {
                            return true;
                        }
                        break;
                    case 2:
                        //保留后拍数据
                        if (face_value < border_traffic_checkpoint_min
                                || face_value > border_traffic_checkpoint_max) {
                            return true;
                        }
                        break;
                    default:
                        //保留所有数据
                        if (data_source == other_checkpoint_type) {
                            //前拍数据
                            if (face_value < border_other_checkpoint_min
                                    || face_value > border_other_checkpoint_max) {
                                return true;
                            }
                        } else if (data_source == traffic_checkpoint_type) {
                            //后拍数据
                            if (face_value < border_traffic_checkpoint_min
                                    || face_value > border_traffic_checkpoint_max) {
                                return true;
                            }
                        }
                        break;
                }
            }
            // 对车辆图像是否完整进行过滤部分结束=====================================

            // 对车辆品牌进行过滤部分开始=====================================
            if (logo_exclude_enable && logo_exclude.length > 0) {
                for (byte[] tmp : logo_exclude) {
                    if (ByteUtils.equals(logo, tmp)) {
                        return true;
                    }
                }
            }
            if (childlogo_exclude_enable && childlogo_exclude.length > 0) {
                for (byte[] tmp : childlogo_exclude) {
                    if (ByteUtils.equals(child_logo, tmp)) {
                        return true;
                    }
                }
            }
            // 对车辆品牌进行过滤部分结束=====================================

        } catch (Exception e) {
            LOG.warn("exclude error => " + e.getMessage(), e);
            return true;
        }

        return false;
    }

    /**
     * 统计一段时间内的接收到的数据的总量
     */
    protected void calculatorReceiveCount(boolean isSuccess) {
        // 记录接收到的总记录数,在指定的时间段内
        if (countIntervalEnable) {
            lock.lock();
            if (lastCountTime == 0) {
                lastCountTime = System.currentTimeMillis();
            }
            receiveCount ++;
            totalInput ++;
            if (isSuccess) {
                totalProcess ++;
            }
            if (System.currentTimeMillis() > (lastCountTime + countInterval)) {
                String msg = String
                        .format(
                                "receive count debug last print time is [%d], current print time is [%d], receive count is [%d]",
                                lastCountTime, System.currentTimeMillis(), receiveCount);
                lastCountTime = System.currentTimeMillis();
                receiveCount = 0;
                LOG.info(msg);
            }
            lock.unlock();
        }
    }

    /**
     * 把接收到的InputRecord转换成uft-8的字符串
     *
     * @param record
     * @return
     */
    private String toString(InputRecord record) {
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
            sb.append(record.vehicle_feature.toString());
        }
        first = false;
        sb.append(")");
        return sb.toString();
    }

    /**
     * 特征搜车服务配置信息
     *
     * @param props
     */
    private void loadSimilarVehicleSearchServiceByProps(Properties props) {

        //similar.vehicle.search.enable
        String tmp = StringUtils.trimToNull(props.getProperty("similar.vehicle.search.enable"));
        if (null != tmp) {
            LOG.info(String.format("bolt load similar.vehicle.search.enable values is %s", tmp));
            similar_vehicle_search_enable = Boolean.valueOf(tmp);
        } else {
            LOG.info(String.format("bolt default similar.vehicle.search.enable values is %s", "true"));
        }

        //similar.vehicle.search.service.host
        tmp = StringUtils.trimToNull(props.getProperty("similar.vehicle.search.service.host"));
        if (null != tmp) {
            LOG.info(String.format("bolt load similar.vehicle.search.service.host values is %s", tmp));
            host = tmp;
        } else {
            LOG.info(String.format("bolt default similar.vehicle.search.service.host values is %s", "0.0.0.0"));
        }

        //similar.vehicle.search.service.port
        tmp = StringUtils.trimToNull(props.getProperty("similar.vehicle.search.service.port"));
        if (null != tmp) {
            LOG.info(String.format("bolt load similar.vehicle.search.service.port values is %s", tmp));
            port = Integer.valueOf(tmp);
        } else {
            LOG.info(String.format("bolt default similar.vehicle.search.service.port values is %s", "30056"));
        }

        //similar.vehicle.search.result.count
        tmp = StringUtils.trimToNull(props.getProperty("similar.vehicle.search.result.count"));
        if (null != tmp) {
            LOG.info(String.format("bolt load similar.vehicle.search.result.count values is %s", tmp));
            result_count = Integer.valueOf(tmp);
        } else {
            LOG.info(String.format("bolt default similar.vehicle.search.result.count values is %s", "50"));
        }

        //similar.vehicle.search.result.count
        tmp = StringUtils.trimToNull(props.getProperty("similar.vehicle.search.similarity.min_value"));
        if (null != tmp) {
            LOG.info(String.format("bolt load similar.vehicle.search.similarity.min_value values is %s", tmp));
            result_distence = Integer.valueOf(tmp);
        } else {
            LOG.info(String.format("bolt default similar.vehicle.search.similarity.min_value values is %s", "55"));
        }

        //similar.vehicle.search.date
        tmp = StringUtils.trimToNull(props.getProperty("similar.vehicle.search.date"));
        if (null != tmp) {
            LOG.info(String.format("bolt load similar.vehicle.search.date values is %s", tmp));
            searchDate = Integer.valueOf(tmp);
        } else {
            LOG.info(String.format("bolt default similar.vehicle.search.date values is %s", "30"));
        }

        //search.result.id.only
        tmp = StringUtils.trimToNull(props.getProperty("search.result.id.only"));
        if (null != tmp) {
            LOG.info(String.format("bolt load search.result.id.only values is %s", tmp));
            result_id_only = Boolean.valueOf(tmp);
        } else {
            LOG.info(String.format("bolt default search.result.id.only values is %s", "true"));
        }

    }

    /**
     * 加载车辆在图像边界的过滤的配置信息, by 2015-05-27
     *
     * @param props
     */
    private void loadBorderExcludeConfByProps(Properties props) {
        //fakecar.image.border.filter.enable=true
        String tmp = StringUtils.trimToNull(props.getProperty(
                "fakecar.image.border.filter.enable", "false"));
        if (null != tmp) {
            LOG.info(String.format(
                    "bolt load fakecar.image.border.filter.enable values is %s", tmp));
            border_filter_enable = Boolean.parseBoolean(tmp);
        } else {
            LOG.info(String.format(
                    "bolt default fakecar.image.border.filter.enable values is %s",
                    "true"));
        }
        //fakecar.image.border.scale.other_checkpoint.min=0.9
        tmp = StringUtils.trimToNull(props.getProperty(
                "fakecar.image.border.scale.other_checkpoint.min", "0.9"));
        if (null != tmp) {
            LOG.info(String.format(
                    "bolt load fakecar.image.border.scale.other_checkpoint.min values is %s", tmp));
            border_other_checkpoint_min = Float.parseFloat(tmp);
        } else {
            LOG.info(String.format(
                    "bolt default fakecar.image.border.scale.other_checkpoint.min values is %s",
                    "0.9"));
        }
        //fakecar.image.border.scale.other_checkpoint.max=1.1
        tmp = StringUtils.trimToNull(props.getProperty(
                "fakecar.image.border.scale.other_checkpoint.max", "1.1"));
        if (null != tmp) {
            LOG.info(String.format(
                    "bolt load fakecar.image.border.scale.other_checkpoint.max values is %s", tmp));
            border_other_checkpoint_max = Float.parseFloat(tmp);
        } else {
            LOG.info(String.format(
                    "bolt default fakecar.image.border.scale.other_checkpoint.max values is %s",
                    "1.1"));
        }
        //fakecar.image.border.scale.traffic_checkpoint.min=1.0
        tmp = StringUtils.trimToNull(props.getProperty(
                "fakecar.image.border.scale.traffic_checkpoint.min", "1.0"));
        if (null != tmp) {
            LOG.info(String.format(
                    "bolt load fakecar.image.border.scale.traffic_checkpoint.min values is %s", tmp));
            border_traffic_checkpoint_min = Float.parseFloat(tmp);
        } else {
            LOG.info(String.format(
                    "bolt default fakecar.image.border.scale.traffic_checkpoint.min values is %s",
                    "1.0"));
        }
        //fakecar.image.border.scale.traffic_checkpoint.max=1.2
        tmp = StringUtils.trimToNull(props.getProperty(
                "fakecar.image.border.scale.traffic_checkpoint.max", "1.2"));
        if (null != tmp) {
            LOG.info(String.format(
                    "bolt load fakecar.image.border.scale.traffic_checkpoint.min values is %s", tmp));
            border_traffic_checkpoint_max = Float.parseFloat(tmp);
        } else {
            LOG.info(String.format(
                    "bolt default fakecar.image.border.scale.traffic_checkpoint.max values is %s",
                    "1.2"));
        }
    }

    /**
     * 加载需要排除时间段的配置信息,by 2015-05-25
     *
     * @param props
     */
    private void loadHourExcludeConfByProps(Properties props) {
        String tmp = StringUtils.trimToNull(props.getProperty(
                "input.traffic.time.format", "yyyyMMddHHmmss"));
        if (null != tmp) {
            LOG.info(String.format(
                    "bolt load input.traffic.time.format values is %s", tmp));
            date_pattern = ByteUtils.buildDatePattern(tmp);
        } else {
            LOG.info(String.format(
                    "bolt default input.traffic.time.format values is %s",
                    "yyyyMMddHHmmss"));
            date_pattern = ByteUtils.buildDatePattern("yyyyMMddHHmmss");
        }

        tmp = StringUtils.trimToNull(props.getProperty(
                "fakecar.execute.right.enable", "true"));
        if (null != tmp) {
            LOG.info(String.format(
                    "bolt load fakecar.execute.right.enable values is %s", tmp));
            enable_hour_flag = Boolean.parseBoolean(tmp);
        } else {
            LOG.info(String.format(
                    "bolt default fakecar.execute.right.enable values is %s", "true"));
        }

        tmp = StringUtils.trimToNull(props.getProperty("fakecar.right.begin.hour",
                "19"));
        if (null != tmp) {
            LOG.info(String.format("bolt load fakecar.right.begin.hour values is %s",
                    tmp));
            begin_hour = Integer.parseInt(tmp);
        } else {
            LOG.info(String.format(
                    "bolt default fakecar.right.begin.hour values is %s", "19"));
        }

        tmp = StringUtils.trimToNull(props.getProperty("fakecar.right.end.hour",
                "6"));
        if (null != tmp) {
            LOG.info(String.format("bolt load fakecar.right.end.hour values is %s",
                    tmp));
            end_hour = Integer.parseInt(tmp);
        } else {
            LOG.info(String.format(
                    "bolt default fakecar.right.end.hour values is %s", "6"));
        }
    }

    /**
     * 加载与线程相关的配置参数
     *
     * @param props
     */
    private void loadThreadConfByProps(Properties props) {
        String less_common = StringUtils.trimToNull(props.getProperty(
                "fakecar.delete.less_common.record.day", "30"));
        if (null != less_common) {
            LOG.info(String.format(
                    "bolt load fakecar.delete.less_common.record.day values is %s",
                    less_common));
            cleanDay = Integer.parseInt(less_common) * 24 * 60 * 60 * 1000;
        } else {
            LOG.info("bolt load fakecar.delete.less_common.record.day values is null"
                    + " set default value is -1");
            cleanDay = -1;
        }

        String interval = StringUtils.trimToNull(props.getProperty(
                "fakecar.result.flush.interval", "3"));
        if (null != interval) {
            LOG.info(String.format(
                    "bolt load fakecar.result.flush.interval values is %s", interval));
            flushInterval = Integer.parseInt(interval) * 1000;
        } else {
            LOG.info("bolt load fakecar.result.flush.interval values is null"
                    + " set default value is 3");
        }

        String number = StringUtils.trimToNull(props.getProperty(
                "fakecar.result.flush.number", "100"));
        if (null != number) {
            LOG.info(String.format(
                    "bolt load fakecar.result.flush.number values is %s", number));
            flushNumber = Integer.parseInt(number);
        } else {
            LOG.info("bolt load fakecar.result.flush.number values is null"
                    + " set default value is 100");
        }
    }

    /**
     * 加载数据来源的过滤规则定义
     *
     * @param props
     * @throws Exception
     */
    private void loadDataSourceConfByProps(Properties props) throws Exception {
        String datasource_str = StringUtils.trimToNull(props.getProperty(
                "include.data.source.type", "1"));
        if (null != datasource_str) {
            LOG.info(String.format("bolt load include.data.source.type values is %s",
                    datasource_str));
            excluteDataSource = Byte.parseByte(datasource_str);
        } else {
            LOG.info("bolt load include.data.source.type values is null set default value is 1");
        }
        boolean flag = false;
        switch (excluteDataSource) {
            case 0:
            case 1:
            case 2:
                flag = true;
                break;
            default:
                break;
        }
        if (!flag) {
            throw new Exception(
                    "include.data.source.type config value not contains [0,1,2]");
        }

        String traffic_checkpoint_str = StringUtils.trimToNull(props.getProperty(
                "traffic.record.traffic_checkpoint.source_type", "1"));
        if (null != traffic_checkpoint_str) {
            LOG.info(String
                    .format(
                            "bolt load traffic.record.traffic_checkpoint.source_type values is %s",
                            traffic_checkpoint_str));
            traffic_checkpoint_type = Byte.parseByte(traffic_checkpoint_str);
        } else {
            LOG.info("bolt load traffic.record.traffic_checkpoint.source_type"
                    + " values is null set default value is 1");
        }

        String other_checkpoint_str = StringUtils.trimToNull(props.getProperty(
                "traffic.record.other_checkpoint.source_type", "0"));
        if (null != other_checkpoint_str) {
            LOG.info(String.format(
                    "bolt load traffic.record.other_checkpoint.source_type values is %s",
                    other_checkpoint_str));
            other_checkpoint_type = Byte.parseByte(other_checkpoint_str);
        } else {
            LOG.info("bolt load traffic.record.other_checkpoint.source_type"
                    + " values is null set default value is 1");
        }
    }

    /**
     * 加载properties配置文件中的结构化二次识别信息比较规则,目前只有是否启用对车辆颜色与车辆类型的比较
     *
     * @param props
     * @throws Exception
     */
    private void loadStructCompareRuleByProps(Properties props) throws Exception {
        String vehicle_color_str = StringUtils.trimToNull(props.getProperty(
                "fakecar.struct.vehicle.color.enable", "false"));

        if (null != vehicle_color_str) {
            LOG.info(String.format(
                    "bolt load fakecar.struct.vehicle.color.enable values is %s",
                    vehicle_color_str));
            vehicleColorEnable = Boolean.parseBoolean(vehicle_color_str);
        } else {
            LOG.info("bolt load fakecar.struct.vehicle.color.enable values is null set default value is false");
        }

        String vehicle_type_str = StringUtils.trimToNull(props.getProperty(
                "fakecar.struct.vehicle.type.enable", "false"));

        if (null != vehicle_type_str) {
            LOG.info(String.format(
                    "bolt load fakecar.struct.vehicle.type.enable values is %s",
                    vehicle_type_str));
            vehicleTypeEnable = Boolean.parseBoolean(vehicle_type_str);
        } else {
            LOG.info("bolt load fakecar.struct.vehicle.type.enable values is null set default value is false");
        }

        String vehicle_style_str = StringUtils.trimToNull(props.getProperty(
                "fakecar.struct.vehicle.style.enable", "false"));
        if (null != vehicle_style_str) {
            LOG.info(String.format(
                    "bolt load fakecar.struct.vehicle.style.enable values is %s",
                    vehicle_style_str));
            vehicleStyleEnable = Boolean.parseBoolean(vehicle_style_str);
        } else {
            LOG.info("bolt load fakecar.struct.vehicle.style.enable values is null set default value is false");
        }

        String compare_relation_str = StringUtils.trimToNull(props.getProperty(
                "fakecar.feature.struct.compare.relation", "2"));
        if (null != compare_relation_str) {
            LOG.info(String.format(
                    "bolt load fakecar.feature.struct.compare.relation values is %s",
                    compare_relation_str));
            compareRelation = Byte.parseByte(compare_relation_str);
        } else {
            LOG.info("bolt load fakecar.feature.struct.compare.relation values is null set default value is 0");
        }
        boolean flag = false;
        switch (compareRelation) {
            case 0:
            case 1:
            case 2:
            case 3:
                flag = true;
                break;
            default:
                break;
        }
        if (!flag) {
            throw new Exception(
                    "fakecar.feature.struct.compare.relation config value not contains [0,1,2,3]");
        }

        String key_relation_str = StringUtils.trimToNull(props.getProperty(
                "fakecar.key.relation", "0"));
        if (null != key_relation_str) {
            LOG.info(String.format("bolt load fakecar.key.relation values is %s",
                    key_relation_str));
            keyRelation = Byte.parseByte(key_relation_str);
        } else {
            LOG.info("bolt load fakecar.key.relation values is null set default value is 0");
        }
        flag = false;
        switch (keyRelation) {
            case 0:
            case 1:
                flag = true;
                break;
            default:
                break;
        }
        if (!flag) {
            throw new Exception(
                    "fakecar.key.relation config value not contains [0,1]");
        }

        String distance_str = StringUtils.trimToNull(props.getProperty(
                "fakecar.feature.max.distance", "38"));
        if (null != distance_str) {
            LOG.info(String.format(
                    "bolt load fakecar.feature.max.distance values is %s", distance_str));
            maxDistance = Float.parseFloat(distance_str);
        } else {
            LOG.info("bolt load fakecar.feature.max.distance values is null set default value is 250");
        }

        String native_enable = StringUtils.trimToNull(props.getProperty(
                "server.load.native.lib.enable", "true"));
        if (null != native_enable) {
            LOG.info(String.format("bolt load server.load.native.lib.enable is %s",
                    native_enable));
            enableNative = Boolean.parseBoolean(native_enable);
        } else {
            LOG.info("bolt load server.load.native.lib.enable is null set default value is true");
        }
    }

    /**
     * 加载properties配置文件中的流程debug是否开启的配置
     *
     * @param props
     */
    private void loadDebugByProps(Properties props) {
        String receive_debug_str = StringUtils.trimToNull(props.getProperty(
                "task.process.receive.msg.debug", "false"));

        if (null != receive_debug_str) {
            LOG.info(String.format(
                    "bolt load task.process.receive.msg.debug values is %s",
                    receive_debug_str));
            receive_debug = Boolean.parseBoolean(receive_debug_str);
        } else {
            LOG.info("bolt load task.process.receive.msg.debug values is null set default value is false");
        }

        String exectime_debug_str = StringUtils.trimToNull(props.getProperty(
                "task.process.execute.time.debug", "false"));

        if (null != exectime_debug_str) {
            LOG.info(String.format(
                    "bolt load task.process.execute.time.debug values is %s",
                    exectime_debug_str));
            exectime_debug = Boolean.parseBoolean(exectime_debug_str);
        } else {
            LOG.info("bolt load task.process.execute.time.debug values is null set default value is false");
        }

        String process_debug_str = StringUtils.trimToNull(props.getProperty(
                "task.process.calculator.debug", "false"));

        if (null != process_debug_str) {
            LOG.info(String.format(
                    "bolt load task.process.calculator.debug values is %s",
                    process_debug_str));
            process_debug = Boolean.parseBoolean(process_debug_str);
        } else {
            LOG.info("bolt load task.process.calculator.debug values is null set default value is false");
        }

        String interval_enable_str = StringUtils.trimToNull(props.getProperty(
                "task.receive.count.interval.enable", "false"));
        if (null != interval_enable_str) {
            LOG.info(String.format(
                    "bolt load task.receive.count.interval.enable values is %s",
                    interval_enable_str));
            countIntervalEnable = Boolean.parseBoolean(interval_enable_str);
        } else {
            LOG.info("bolt load task.receive.count.interval.enable values is null set default value is false");
        }
        String count_interval_str = StringUtils.trimToNull(props.getProperty(
                "task.receive.count.interval.minute", "10"));
        if (null != count_interval_str) {
            LOG.info(String.format(
                    "bolt load task.receive.count.interval.minute values is %s",
                    count_interval_str));
            countInterval = Integer.parseInt(count_interval_str) * 60 * 1000;
        } else {
            LOG.info("bolt load task.receive.count.interval.minute values is null set default value is 10");
        }
    }

    /**
     * 加载properties配置文件中的排除车牌颜色条件配置
     *
     * @param props
     */
    private void loadLicencePlateColorByProps(Properties props) {
        String color_equals_str = StringUtils.trimToNull(props
                .getProperty("exclude.licence_plate_color.equals"));

        if (null != color_equals_str) {
            LOG.info(String.format(
                    "bolt load exclude.licence_plate_color.equals values is %s",
                    color_equals_str));
            String[] arr = color_equals_str.split(",");
            List<String> list = new ArrayList<String>();
            for (String t : arr) {
                t = StringUtils.trimToNull(t);
                if (t == null) {
                    continue;
                }
                list.add(t);
            }
            color_equals = new byte[list.size()][];
            for (int i = 0; i < list.size(); i++) {
                color_equals[i] = list.get(i).getBytes(charset);
            }
        } else {
            LOG.info("bolt load exclude.licence_plate_color.equals values is null skip this config");
        }
    }

    /**
     * 加载properties配置文件中的排除车牌条件配置
     *
     * @param props
     */
    private void loadLicencePlateByProps(Properties props) {
        String startwiths = StringUtils.trimToNull(props
                .getProperty("exclude.licence_plate.startwiths"));

        if (null != startwiths) {
            LOG.info(String
                    .format("bolt load exclude.licence_plate.startwiths values is %s",
                            startwiths));
            String[] arr = startwiths.split(",");
            List<String> list = new ArrayList<String>();
            for (String t : arr) {
                t = StringUtils.trimToNull(t);
                if (t == null) {
                    continue;
                }
                list.add(t);
            }
            startWith = new byte[list.size()][];
            for (int i = 0; i < list.size(); i++) {
                startWith[i] = list.get(i).getBytes(charset);
            }
        } else {
            LOG.info("bolt load exclude.licence_plate.startwiths values is null skip this config");
        }

        String endwiths = StringUtils.trimToNull(props
                .getProperty("exclude.licence_plate.endwiths"));

        if (null != endwiths) {
            LOG.info(String.format(
                    "bolt load exclude.licence_plate.endwiths values is %s", endwiths));
            String[] arr = endwiths.split(",");
            List<String> list = new ArrayList<String>();
            for (String t : arr) {
                t = StringUtils.trimToNull(t);
                if (t == null) {
                    continue;
                }
                list.add(t);
            }
            endWith = new byte[list.size()][];
            for (int i = 0; i < list.size(); i++) {
                endWith[i] = list.get(i).getBytes(charset);
            }
        } else {
            LOG.info("bolt load exclude.licence_plate.endwiths values is null skip this config");
        }

        String contains_str = StringUtils.trimToNull(props
                .getProperty("exclude.licence_plate.contains"));

        if (null != contains_str) {
            LOG.info(String
                    .format("bolt load exclude.licence_plate.contains values is %s",
                            contains_str));
            String[] arr = contains_str.split(",");
            List<String> list = new ArrayList<String>();
            for (String t : arr) {
                t = StringUtils.trimToNull(t);
                if (t == null) {
                    continue;
                }
                list.add(t);
            }
            contains = new byte[list.size()][];
            for (int i = 0; i < list.size(); i++) {
                contains[i] = list.get(i).getBytes(charset);
            }
        } else {
            LOG.info("bolt load exclude.licence_plate.contains values is null skip this config");
        }

        String equals_str = StringUtils.trimToNull(props
                .getProperty("exclude.licence_plate.equals"));

        if (null != equals_str) {
            LOG.info(String.format(
                    "bolt load exclude.licence_plate.equals values is %s", equals_str));
            String[] arr = equals_str.split(",");
            List<String> list = new ArrayList<String>();
            for (String t : arr) {
                t = StringUtils.trimToNull(t);
                if (t == null) {
                    continue;
                }
                list.add(t);
            }
            equals = new byte[list.size()][];
            for (int i = 0; i < list.size(); i++) {
                equals[i] = list.get(i).getBytes(charset);
            }
        } else {
            LOG.info("bolt load exclude.licence_plate.equals values is null skip this config");
        }

        // fakecar.only_include.local.enable
        String only_include_str = StringUtils.trimToNull(props
                .getProperty("fakecar.only_include.local.enable"));
        if (null != only_include_str) {
            LOG.info(String.format(
                    "bolt load fakecar.only_include.local.enable values is %s",
                    only_include_str));
            onlyLocalEnable = Boolean.parseBoolean(only_include_str);
        } else {
            LOG.info("bolt load fakecar.only_include.local.enable values is false skip this config");
        }

        // only_include.licence_plate.startwiths
        String only_plate_str = StringUtils.trimToNull(props
                .getProperty("only_include.licence_plate.startwiths"));
        if (null != only_plate_str) {
            LOG.info(String.format(
                    "bolt load only_include.licence_plate.startwiths values is %s",
                    only_plate_str));
            localPlateStartWith = only_plate_str.getBytes(charset);
        } else {
            LOG.info("bolt load only_include.licence_plate.startwiths values is false skip this config");
        }

        // fakecar.logo_confidence.enable
        String lc_enable_str = StringUtils.trimToNull(props
                .getProperty("fakecar.logo_confidence.enable"));
        if (null != lc_enable_str) {
            LOG.info(String.format("bolt load fakecar.logo_confidence.enable values is %s",
                    lc_enable_str));
            isLogoConfidenceEnable = Boolean.parseBoolean(lc_enable_str);
        } else {
            LOG.info("bolt load fakecar.logo_confidence.enable values is true skip this config");
        }

        // fakecar.logo_confidence.value.min
        String lc_minvalue_str = StringUtils.trimToNull(props
                .getProperty("fakecar.logo_confidence.value.min"));
        if (null != lc_minvalue_str) {
            LOG.info(String.format("bolt load fakecar.logo_confidence.value.min values is %s",
                    lc_minvalue_str));
            logoConfidenceMinValue = Double.parseDouble(lc_minvalue_str);
        } else {
            LOG.info("bolt load fakecar.logo_confidence.value.min values is 50 skip this config");
        }

        // fakecar.confidence_level.enable
        String cl_enable_str = StringUtils.trimToNull(props
                .getProperty("fakecar.confidence_level.enable"));
        if (null != cl_enable_str) {
            LOG.info(String.format(
                    "bolt load fakecar.confidence_level.enable values is %s",
                    cl_enable_str));
            isConfidenceLevelEnable = Boolean.parseBoolean(cl_enable_str);
        } else {
            LOG.info("bolt load fakecar.confidence_level.enable values is true skip this config");
        }

        // fakecar.confidence_level.value.min
        String cl_minvalue_str = StringUtils.trimToNull(props
                .getProperty("fakecar.confidence_level.value.min"));
        if (null != cl_minvalue_str) {
            LOG.info(String.format(
                    "bolt load fakecar.confidence_level.value.min values is %s",
                    cl_minvalue_str));
            confidenceLevelMinValue = Double.parseDouble(cl_minvalue_str);
        } else {
            LOG.info("bolt load fakecar.confidence_level.value.min values is 50 skip this config");
        }

        //fakecar.plate.similarity.enable
        String tmp = StringUtils.trimToNull(props
                .getProperty("fakecar.plate.similarity.enable", "false"));
        if (null != tmp) {
            LOG.info(String.format(
                    "bolt load fakecar.plate.similarity.enable values is %s", tmp));
            plate_similarity_enable = Boolean.parseBoolean(tmp);
        } else {
            LOG.info("bolt load fakecar.plate.similarity.enable values is true skip this config");
        }

        //fakecar.plate.similarity.min_value
        tmp = StringUtils.trimToNull(props
                .getProperty("fakecar.plate.similarity.min_value", "70"));
        if (null != tmp) {
            LOG.info(String.format(
                    "bolt load fakecar.plate.similarity.min_value values is %s", tmp));
            plate_similarity_min_value = Float.parseFloat(tmp);
        } else {
            LOG.info("bolt load fakecar.plate.similarity.min_value values is 70 skip this config");
        }

        //fakecar.plate.confidence_level.enable
        tmp = StringUtils.trimToNull(props
                .getProperty("fakecar.plate.confidence_level.enable", "true"));
        if (null != tmp) {
            LOG.info(String.format(
                    "bolt load fakecar.plate.confidence_level.enable values is %s", tmp));
            plate_confidence_enable = Boolean.parseBoolean(tmp);
        } else {
            LOG.info("bolt load fakecar.plate.confidence_level.enable values is true skip this config");
        }
        //fakecar.plate.confidence_level.value.min
        tmp = StringUtils.trimToNull(props
                .getProperty("fakecar.plate.confidence_level.value.min", "80"));
        if (null != tmp) {
            LOG.info(String.format(
                    "bolt load fakecar.plate.confidence_level.value.min values is %s", tmp));
            plate_confidence_minvalue = Float.parseFloat(tmp);
        } else {
            LOG.info("bolt load fakecar.plate.confidence_level.value.min values is 75 skip this config");
        }
    }

    /**
     * 加载properties配置文件中的排除车辆品牌条件配置
     *
     * @param props
     */
    private void loadVehicleLogoExcludeProps(Properties props) throws Exception {

        String tmp = StringUtils.trimToNull(props.getProperty("fakecar.vehicle.logo.exclude.enable"));
        if (null != tmp) {
            LOG.info(String.format("bolt load fakecar.vehicle.logo.exclude.enable values is %s", tmp));
            logo_exclude_enable = Boolean.parseBoolean(tmp);
        } else {
            LOG.info("bolt load fakecar.vehicle.logo.exclude.enable values is false");
        }

        tmp = StringUtils.trimToNull(props.getProperty("fakecar.vehicle.logo.exclude"));
        if (null != tmp) {
            LOG.info(String.format("bolt load fakecar.vehicle.logo.exclude values is %s", tmp));
            String[] arr = tmp.split(",");

            List<String> list = new ArrayList<String>();
            for (String t : arr) {
                t = StringUtils.trimToNull(t);
                if (t == null) {
                    continue;
                }
                list.add(t);
            }
            logo_exclude = new byte[list.size()][];
            for (int i = 0; i < list.size(); i++) {
                logo_exclude[i] = list.get(i).getBytes(charset);
            }

        } else {
            logo_exclude = new byte[1][];
            logo_exclude[0] = "-1".getBytes(charset);
            LOG.info("bolt load fakecar.vehicle.logo.exclude values is false");
        }

        tmp = StringUtils.trimToNull(props.getProperty("fakecar.vehicle.childlogo.exclude.enable"));
        if (null != tmp) {
            LOG.info(String.format("bolt load fakecar.vehicle.childlogo.exclude.enable values is %s", tmp));
            childlogo_exclude_enable = Boolean.parseBoolean(tmp);
        } else {
            LOG.info("bolt load fakecar.vehicle.childlogo.exclude.enable values is false");
        }

        tmp = StringUtils.trimToNull(props.getProperty("fakecar.vehicle.childlogo.exclude"));
        if (null != tmp) {
            LOG.info(String.format("bolt load fakecar.vehicle.childlogo.exclude values is %s", tmp));

            String[] arr = tmp.split(",");

            List<String> list = new ArrayList<String>();
            for (String t : arr) {
                t = StringUtils.trimToNull(t);
                if (t == null) {
                    continue;
                }
                list.add(t);
            }
            childlogo_exclude = new byte[list.size()][];
            for (int i = 0; i < list.size(); i++) {
                childlogo_exclude[i] = list.get(i).getBytes(charset);
            }
        } else {
            childlogo_exclude = new byte[1][];
            childlogo_exclude[0] = "-1".getBytes(charset);
            LOG.info("bolt load fakecar.vehicle.childlogo.exclude values is false");
        }

        tmp = StringUtils.trimToNull(props.getProperty("fakecar.vehicle.subbrandname.filter.enable"));
        if (null != tmp) {
            LOG.info(String.format("bolt load fakecar.vehicle.subbrandname。filter。enable is %s", tmp));
            subbrandname_filter_enable = Boolean.parseBoolean(tmp);
        } else {
            LOG.info("bolt load fakecar.vehicle.fronttail.separate.enable values is false");
        }

        tmp = StringUtils.trimToNull(props.getProperty("fakecar.vehicle.fronttail.separate.enable"));
        if (null != tmp) {
            LOG.info(String.format("bolt load fakecar.vehicle.fronttail.separate.enable values is %s", tmp));
            frontTailSeparate = Boolean.parseBoolean(tmp);
        } else {
            LOG.info("bolt load fakecar.vehicle.fronttail.separate.enable values is false");
        }

        tmp = StringUtils.trimToNull(props.getProperty("fakecar.vehicle.dump.period.time"));
        if (null != tmp) {
            LOG.info(String.format("bolt load fakecar.vehicle.dump.period.time values is %s", tmp));
            dumpPeriodTime = Long.parseLong(tmp);
        } else {
            LOG.info("bolt load fakecar.vehicle.dump.period.time values is false");
        }

        tmp = StringUtils.trimToNull(props.getProperty("fakecar.vehicle.dump.dirpath"));
        if (null != tmp) {
            LOG.info(String.format("bolt load fakecar.vehicle.dump.dirpath values is %s", tmp));
            dumpDirPath = tmp;
        } else {
            LOG.info("bolt load fakecar.vehicle.dump.dirpath values is null");
        }

        tmp = StringUtils.trimToNull(props.getProperty("fakecar.vehicle.bdk.check.enable"));
        if (null != tmp) {
            LOG.info(String.format("bolt load fakecar.vehicle.bdk.check.enable values is %s", tmp));
            bdk_check_enable = Boolean.parseBoolean(tmp);
            if (bdk_check_enable) {
                String ip = StringUtils.trimToNull(props.getProperty("fakecar.vehicle.bdk.ip"));
                int port = Integer.valueOf(StringUtils.trimToNull(props.getProperty("fakecar.vehicle.bdk.port")));
                int timeout = Integer.valueOf(StringUtils.trimToNull(props.getProperty("fakecar.vehicle.bdk.timeout")));
                BdkCheckProcessor.start(ip, port, timeout);

                int httpserverport = Integer.valueOf(StringUtils.trimToNull(props.getProperty("fakecar.vehicle.http.serverport")));
                (new SimilarVehicleHttpServer(this, httpserverport)).start();
            }
        } else {
            LOG.info("bolt load fakecar.vehicle.bdk.check.enable is false");
        }

        //headrearMustSame
        tmp = StringUtils.trimToNull(props.getProperty("fakecar.headrear.must.same"));
        if (null != tmp) {
            LOG.info(String.format("bolt load fakecar.headrear.must.same values is %s", tmp));
            headrearMustSame = Boolean.parseBoolean(tmp);
        } else {
            LOG.info("bolt load fakecar.headrear.must.same values is false");
        }

        ///tmp = fakecar.vehicle.brand.mutex
        tmp = StringUtils.trimToNull(props.getProperty("fakecar.vehicle.brand.mutex"));
        if (tmp != null) {
            String[] mutexBrands = tmp.split(",");
            List<String> list = new ArrayList<String>();
            for (String t : mutexBrands) {
                t = StringUtils.trimToNull(t);
                if (t == null) {
                    continue;
                }
                list.add(t);
            }
            mutexBrand = new byte[list.size()][];
            for (int i = 0; i < list.size(); i++) {
                mutexBrand[i] = list.get(i).getBytes(charset);
            }
        }

        //启动清除错误key http server
        tmp = StringUtils.trimToNull(props.getProperty("fakecar.vehicle.clean.errorkey.port"));
        CleanErrorKeyHttpServer cleanErrorKeyServer = null;
        if (tmp != null) {
            cleanErrorKeyServer = new CleanErrorKeyHttpServer(this, Integer.valueOf(tmp));
        } else {
            cleanErrorKeyServer = new CleanErrorKeyHttpServer(this);
        }
        cleanErrorKeyServer.start();
    }

    /**
     * 关闭任务执行
     */
    public void shutdown() {
        if (flushThread != null) {
            flushThread.close();
        }
    }

    // 得到传入的多个参数中最小的一个值
    private static int min(int... is) {
        int min = Integer.MAX_VALUE;
        for (int i : is) {
            if (min > i) {
                min = i;
            }
        }
        return min;
    }

    /**
     * 比对两个车牌号码的相似度,并返回一个0-100之间的数值
     *
     * @param arr1
     * @param arr2
     * @return
     */
    public static float levenshtein(byte[] arr1, byte[] arr2) {
        // 计算两个字符串的长度。
        int len1 = arr1.length;
        int len2 = arr2.length;
        // 建立上面说的数组，比字符长度大一个空间
        int[][] dif = new int[len1 + 1][len2 + 1];
        // 赋初值，步骤B。
        for (int a = 0; a <= len1; a++) {
            dif[a][0] = a;
        }
        for (int a = 0; a <= len2; a++) {
            dif[0][a] = a;
        }
        // 计算两个字符是否一样，计算左上的值
        int temp;
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (arr1[i - 1] == arr2[j - 1]) {
                    temp = 0;
                } else {
                    temp = 1;
                }
                // 取三个值中最小的
                dif[i][j] = min(dif[i - 1][j - 1] + temp, dif[i][j - 1] + 1,
                        dif[i - 1][j] + 1);
            }
        }

        // 计算相似度
        int max_length = Math.max(arr1.length, arr2.length);
        float similarity = (1 - (float) dif[len1][len2] / max_length) * 100;
        return similarity;
    }

    /**
     * 加载车辆子品牌字典表
     */
    private void loadVehicleSubBrand() {
        if (!subbrandname_filter_enable) {
            return;
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader()
                    .getResourceAsStream("VehicleSubBrand.txt")));
            String line = null;
            while ((line = br.readLine()) != null) {
                try {
                    String[] args = line.trim().split(",");
                    String name = args[2];
                    if (name.contains("（")) {
                        name = name.substring(0, name.indexOf("（"));
                    }
                    vehicleSubBrandMap.put(args[1], name);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            LOG.error("load VehicleSubBrand error", e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 加载车辆子品牌组过滤字典
     */
    private void loadSubBrandMutex() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader()
                    .getResourceAsStream("ChildBrandMutex.txt")));
            String line = null;
            while ((line = br.readLine()) != null) {
                try {
                    String[] args = line.trim().split("-");
                    String childLogo1 = args[0];
                    String childLogo2 = args[1];
                    childLogo1 = childLogo1.substring(0, childLogo1.indexOf("("));
                    childLogo2 = childLogo2.substring(0, childLogo2.indexOf("("));
                    byte[][] logoBts = new byte[2][];
                    logoBts[0] = childLogo1.getBytes(charset);
                    logoBts[1] = childLogo2.getBytes(charset);
                    String key = getSubBrandMutexKey(childLogo1, childLogo2);
                    if (key != null)
                        childLogoMutexMap.put(key, logoBts);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            LOG.error("load ChildBrandMutex error", e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected String getSubBrandMutexKey(String logo1, String logo2) {
        int h1 = Integer.valueOf(String.valueOf(logo1));
        int h2 = Integer.valueOf(String.valueOf(logo2));

        if (h1 > h2) {
            return logo1 + "_" + logo2;
        } else if (h1 < h2) {
            return logo2 + "_" + logo1;
        }

        return null;
    }
}
