package com.netposa.poseidon.library.util;
import com.netposa.poseidon.library.bean.TableStatusCode;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class FileMemUtil {
    private static Properties properties = LoadPropers.getSingleInstance().getProperties("server");

    private static Logger logger = Logger.getLogger(FileMemUtil.class);
    private static String filename = "table";
    /**
     * 内存数据写入磁盘
     * @param table 内存里维护的表状态
     * @param filePath 持久化磁盘文件路径
     * @return true or false
     */
    public static boolean memDump2File(Map<String, Map<String, TableStatusCode>> table, String filePath) {
        try {
            File tmpFile = new File(filePath + filename + "_tmp");
            File parentFile = tmpFile.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            if (tmpFile.exists()) {
                tmpFile.delete();
            }

            FileOutputStream out = new FileOutputStream(filePath + filename + "_tmp");
            OutputStreamWriter outWriter = new OutputStreamWriter(out, "UTF-8");
            BufferedWriter bufWrite = new BufferedWriter(outWriter);
            for (String key : table.keySet()) {
                for (Map.Entry<String, TableStatusCode> entry : table.get(key).entrySet()) {
                    bufWrite.write(key + " " + entry.getKey() + " " + entry.getValue() + "\r\n");
                }
            }
            bufWrite.close();
            outWriter.close();
            out.close();

            File file = new File(filePath + filename);
            if (file.exists()) {
                file.delete();
            }

            File newFile = new File(filePath + filename + "_tmp");
            if (newFile.exists()) {
                newFile.renameTo(file);
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    /**
     * 磁盘文件读入内存
     * @param filePath 文件路径
     * @return Map<String, Map<ConnectionManagerKey, TableStatusCode>>
     */
    public static Map<String, Map<String, TableStatusCode>> fileRead2Mem(String filePath) {
        Map<String, Map<String, TableStatusCode>> table = new HashMap<>();
        try {
            File file = new File(filePath + filename);
            if (!file.exists()) {
                return table;
            }
            FileInputStream in = new FileInputStream(filePath + filename);
            InputStreamReader inReader = new InputStreamReader(in, "UTF-8");
            BufferedReader bufReader = new BufferedReader(inReader);
            String line = null;
            while((line = bufReader.readLine()) != null) {
                String[] splits = line.split(" ");
                if (splits.length == 3) {
                    if (!table.containsKey(splits[0])) {
                        table.put(splits[0], new HashMap<String, TableStatusCode>());
                    }
                    if ("IN_USING".equals(splits[2])) {
                        table.get(splits[0]).put(splits[1], TableStatusCode.IN_USING);
                    } else if ("WAITTING_DELETED".equals(splits[2])) {
                        table.get(splits[0]).put(splits[1], TableStatusCode.WAITTING_DELETED);
                    } else {
                        table.get(splits[0]).put(splits[1], TableStatusCode.WAITTING_CREATED);
                    }
                }
            }
            bufReader.close();
            inReader.close();
            in.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return table;
    }
}
