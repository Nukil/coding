package com.netposa.poseidon.fakecardetect.utils;//package com.netposa.poseidon.fakecardetect.utils;
//
//import com.netposa.poseidon.fakecardetect.bean.ValueRecord;
//import com.netposa.poseidon.fakecardetect.main.FakeCarDetectMainServer;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.*;
//import java.nio.ByteBuffer;
//import java.nio.channels.FileChannel;
//import java.util.Timer;
//import java.util.TimerTask;
//import java.util.concurrent.ConcurrentHashMap;
//
//
//public class OperateHdfsUtil {
//    private static final Logger LOG = LoggerFactory
//            .getLogger(OperateHdfsUtil.class);
//
//    //持久化线程数
//    private static final int thread_num = 8;
////    private static String save_dir_path = "fakecardump/";
//
//    private static String save_dir_path = FakeCarDetectMainServer.propConf().save_dir_path();
//
//    /**
//     * 启动写入文件
//     */
//    public static void startDump(ConcurrentHashMap<String,ValueRecord> tempBroad) throws Exception {
//        initDir();//初始化文件目录
//        LOG.info("dump processor start......");
//        (new Timer()).schedule(new TimerTask() {
//            @Override
//            public void run() {
//                LOG.info("dump task run......");
//                try {
//                    ConcurrentHashMap<String,ValueRecord> vehicles = tempBroad;
//                    if(vehicles.size()==0){
//                         return;
//                    }else {
//                        vehicles.forEach(vehicles.size()/thread_num,(k,v)->{ try {
//                            ConcurrentHashMap<String, ValueRecord> vMap = vehicles;
//                            String fileName = "vehicleInfo";
//                            String tempFileName = fileName + ".tmp";
//                            File tmpFile = new File(save_dir_path + tempFileName);
//                            FileOutputStream fos = null;
//                            FileChannel fc_out = null;
//                            ByteArrayOutputStream bo = null;
//                            ObjectOutputStream oo = null;
//                            try {
//                                bo = new ByteArrayOutputStream();
//                                oo = new ObjectOutputStream(bo);
//                                oo.writeObject(vMap);
//                                byte[] bts = bo.toByteArray();
//
//                                fos = new FileOutputStream(tmpFile);
//                                fc_out = fos.getChannel();
//                                ByteBuffer buf = ByteBuffer.wrap(bts);
//                                fc_out.write(buf);
//                                fos.flush();
//                                fc_out.force(true);
//                            } finally {
//                                if (null != fc_out) {
//                                    fc_out.close();
//                                }
//                                if (null != fos) {
//                                    fos.close();
//                                }
//                                if(oo != null) {
//                                    oo.close();
//                                }
//                                if(bo != null) {
//                                    bo.close();
//                                }
//                            }
//                            if(tmpFile.length() > 0) {
//                                File oldFile = new File(save_dir_path + fileName);
//                                tmpFile.renameTo(oldFile);
//                            } else {
//                                if(tmpFile.exists()) {
//                                    tmpFile.delete();
//                                }
//                            }
//                        } catch(Exception e) {
//                            e.printStackTrace();
//                        }
//                        });
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }, FakeCarDetectMainServer.propConf().dumpPeriodTime(), FakeCarDetectMainServer.propConf().dumpPeriodTime());
//    }
//
//
//    /**
//     * 从文件加载数据
//     */
//    public static void load() {
////        System.out.println(FakeCarDetectMainServer.propConf().save_dir_path());
//        File dir = new File(save_dir_path);
//        File[] files = dir.listFiles();
//        if(files != null && files.length > 0) {
//            int size = files.length;
//            int start = 0;
//            int stepSize = size / thread_num;
//            if(size % thread_num > 0) {
//                stepSize += 1;
//            }
//            //int currentThreadNum = 0;
//            for(int i=0;i<thread_num;i++) {
//                if(start >= size) {
//                    break;
//                }
//                int tmpIdx = start;
//                int end = tmpIdx + stepSize;
//                if(end >= size) {
//                    end = size - 1;
//                }
//                readFile(tmpIdx, end, files);
//                start = (end + 1);
//            }
//        }
//    }
//
//    /**
//     * 从文件反序列化，并生成MAP
////     * @param start
////     * @param end
////     * @param files
////     * @param finishSignal
//     */
//    @SuppressWarnings("unchecked")
//    private static void readFile(final int start, final int end, final File[] files) {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                for(int i=start; i<=end; i++) {
//                    File file = files[i];
//                    if(file.getName().endsWith(".tmp")) {
//                        file.delete();
//                        continue;
//                    }
//                    try {
//                        if(file == null || file.length() <= 0) {
//                            continue;
//                        }
//                        FileInputStream fis = null;
//                        FileChannel channel = null;
//
//                        ByteArrayInputStream bi = null;
//                        ObjectInputStream oi = null;
//                        try {
//                            fis = new FileInputStream(file);
//                            channel = fis.getChannel();
//                            ByteBuffer buffer=ByteBuffer.allocate((int)channel.size());
//                            channel.read(buffer);
//
//                            bi = new ByteArrayInputStream(buffer.array());
//                            oi = new ObjectInputStream(bi);
//
//                            Object obj = oi.readObject();
//                            if(obj != null) {
//                                ConcurrentHashMap<String,ValueRecord> tmpMap= (ConcurrentHashMap<String,ValueRecord>)obj;
//                                FakeCarDetectMainServer.tempBroad().putAll(tmpMap);
//                            }
//                        } catch(Exception e) {
//                            e.printStackTrace();
//                        } finally {
//                            if(oi != null) {
//                                oi.close();
//                            }
//                            if(bi != null) {
//                                bi.close();
//                            }
//                            if(channel != null) {
//                                channel.close();
//                            }
//                            if(fis != null) {
//                                fis.close();
//                            }
//                        }
//                    } catch(Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                //finishSignal.countDown();
//            }
//        }).start();
//
//    }
//
//    /**
//     * 创建文件存放目录
//     */
//    private static void initDir() {
//        File dir = new File(save_dir_path);
//        if(dir.exists()) {
//            return;
//        }
//        dir.mkdirs();
//    }
//
//    public static void main(String[] args) throws Exception {
////        File dir = new File(save_dir_path);
////        dir.mkdirs();
////        String fileName = "vehicleInfo";
////        String tempFileName = fileName + ".tmp";
////        File tmpFile = new File(save_dir_path + tempFileName);
////        FileOutputStream foss = null;
////        FileChannel fc_outs = null;
////
////        ByteArrayOutputStream bos = null;
////        ObjectOutputStream oo = null;
////        ConcurrentHashMap<String, String> m = new ConcurrentHashMap<String, String>();
////        for(int i=0;i<10000;i++) {
////            m.put("ZRT" + i, "ZRT" + i);
////        }
////        System.out.println(m);
////        try {
////            bos = new ByteArrayOutputStream();
////            oo = new ObjectOutputStream(bos);
////            oo.writeObject(m);
////            byte[] bts = bos.toByteArray();
////
////            foss = new FileOutputStream(tmpFile);
////            fc_outs = foss.getChannel();
////            ByteBuffer buf = ByteBuffer.wrap(bts);
////            fc_outs.write(buf);
////            foss.flush();
////            fc_outs.force(true);
////        } finally {
////            if (null != fc_outs) {
////                fc_outs.close();
////            }
////            if (null != foss) {
////                foss.close();
////            }
////            if(bos != null) {
////                bos.close();
////            }
////            if(oo != null) {
////                oo.close();
////            }
////        }
////        File oldFile = new File(save_dir_path + fileName);
////        if(tmpFile.length() > 0) {
////            if(oldFile.exists()) {
////                oldFile.delete();
////            }
////            tmpFile.renameTo(oldFile);
////        } else {
////            if(tmpFile.exists()) {
////                tmpFile.delete();
////            }
////        }
////
////        FileInputStream fis = null;
////        FileChannel channel = null;
////
////        ByteArrayInputStream bi = null;
////        ObjectInputStream oi = null;
////        try {
////            fis = new FileInputStream(oldFile);
////            channel = fis.getChannel();
////            ByteBuffer buffer=ByteBuffer.allocate((int)channel.size());
////            channel.read(buffer);
////
////            bi = new ByteArrayInputStream(buffer.array());
////            oi = new ObjectInputStream(bi);
////
////            Object obj = oi.readObject();
////            ConcurrentHashMap mm = (ConcurrentHashMap<String, String>)obj;
////            System.out.println(mm);
////        } catch(Exception e) {
////            e.printStackTrace();
////        } finally {
////            if(oi != null) {
////                oi.close();
////            }
////            if(bi != null) {
////                bi.close();
////            }
////            if(channel != null) {
////                channel.close();
////            }
////            if(fis != null) {
////                fis.close();
////            }
////        }
//    }
//}
