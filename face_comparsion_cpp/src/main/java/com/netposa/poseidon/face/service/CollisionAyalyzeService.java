package com.netposa.poseidon.face.service;

import com.netposa.HbaseUtil;
import com.netposa.poseidon.face.bean.FaceFeature;
import com.netposa.poseidon.face.rpc.outrpc.RegionRecord;
import com.netposa.poseidon.face.util.LoadPropers;
import org.apache.hadoop.hbase.HRegionLocation;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.RegionLocator;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * 碰撞分析
 * 
 * @author Y.yong 2016年11月28日
 */
public class CollisionAyalyzeService {
	private static Properties properties = LoadPropers.getSingleInstance().getProperties("server");
	private static final String tableName = properties.getProperty("hbase.table.name", "face_feature").trim();// 表名
	private static final Logger log = LoggerFactory.getLogger(CollisionAyalyzeService.class);
	private static String cameraIdType = properties.getProperty("camera.id.type", "string");
    private static Connection conn = null;
	/**
	 * 通过区域条件查找人脸集合
	 * @param regionRecord
	 * @return
	 */

	public static List<FaceFeature> findFaceByRegionRecord(RegionRecord regionRecord, ExecutorService pools) {
		final Long startTime = regionRecord.getStartTime();
		final Long endTime = regionRecord.getEndTime();
		List<FaceFeature> list = new ArrayList<>();
		List<Future<PriorityQueue<FaceFeature>>> futures = new ArrayList<>();
		try {
			for (String c : regionRecord.getCameraIds()) {
				final String camera = cameraIdType.equalsIgnoreCase("long") ? String.format("%010d", Integer.parseInt(c)) : c;
				while (conn == null || conn.isClosed()) {
					conn = HbaseUtil.getConn();
					if (conn == null || conn.isClosed()) {
						try {
							Thread.sleep(1000);
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
						log.error("hbase conn is null");
					}
				}
				RegionLocator regionLocator = conn.getRegionLocator(TableName.valueOf(tableName.getBytes()));
				if (!regionLocator.getAllRegionLocations().isEmpty()) {
					List<HRegionLocation> hRegions = regionLocator.getAllRegionLocations();
					for (HRegionLocation hRegion : hRegions) {
						String key = Bytes.toString(hRegion.getRegionInfo().getStartKey());
						final String startKey = "".equals(key) ? "00" : key;
						Future<PriorityQueue<FaceFeature>> future = pools.submit(new Callable<PriorityQueue<FaceFeature>>() {
							@Override
							public PriorityQueue<FaceFeature> call() throws Exception {
								PriorityQueue<FaceFeature> queue = new PriorityQueue<FaceFeature>();
								String startRow = startKey + startTime;
								String stopRow = startKey + endTime;
								List<Result> results = HbaseUtil.getRows(tableName, startRow, stopRow);
								for (Result result : results) {
								    if (camera.equals(Bytes.toString(result.getValue("cf".getBytes(), "cameraId".getBytes())))) {
                                        String logNum = Bytes.toString(result.getValue("cf".getBytes(), "logNum".getBytes()));
                                        byte[] feature = result.getValue("cf".getBytes(), "feature".getBytes());
                                        String cameraId = Bytes.toString(result.getValue("cf".getBytes(), "cameraId".getBytes()));
                                        String gatherTime = Bytes.toString(result.getValue("cf".getBytes(), "gatherTime".getBytes()));
                                        FaceFeature faceFeature = new FaceFeature(logNum, gatherTime, cameraId, feature);
                                        queue.offer(faceFeature);
                                    }
								}
								return queue;
							}
						});
						futures.add(future);
					}
				}
			}
			for (Future<PriorityQueue<FaceFeature>> future : futures) {
				PriorityQueue<FaceFeature> queue = future.get();
				Iterator<FaceFeature> itor = queue.iterator();
				while (itor.hasNext()) {
					list.add(itor.next());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * 矩阵转换为列表
	 * 
	 * @param matrix
	 * @return
	 */
	public static List<List<String>> getSrcMap(Map<String, List<String>> matrix) {
		List<List<String>> list = new ArrayList<List<String>>();
		for (Entry<String, List<String>> e : matrix.entrySet()) {
			String key = e.getKey();
			for (String s : e.getValue()) {
				List<String> temp = new ArrayList<String>();
				temp.add(key);
				temp.add(s);
				list.add(temp);
			}
		}
		return list;
	}

	/**
	 * N阶变N+1阶
	 * 
	 * @param matrix
	 *            初始二维矩阵
	 * @param srcList
	 *            N阶完全子图
	 * @param currrntRegionSize
	 *            当前阶
	 * @param recordRegionSize
	 *            最少阶数阈值
	 * @return
	 */
	public static List<List<String>> convert(Map<String, List<String>> matrix, List<List<String>> srcList, int currrntRegionSize, int recordRegionSize) {
		List<List<String>> resultList = new ArrayList<List<String>>();
		Set<List<String>> removeSet = new HashSet<List<String>>();
		for (List<String> list : srcList) {
			if (list == null || list.size() == 0) {
				continue;
			}
			String lastString = list.get(list.size() - 1);
			List<String> lastList = new ArrayList<String>();
			if(lastString == null){
				continue;
			}
			if (matrix.containsKey(lastString)) {
				lastList.addAll(matrix.get(lastString));
				for (int i = 0; i < list.size() - 1; i++) {
					lastList.retainAll(matrix.get(list.get(i)));
					if (lastList.size() == 0) {
						break;
					}
				}
			}
			for (int i = 0; i < lastList.size(); i++) {
				List<String> temp = new ArrayList<String>();
				temp.addAll(list);
				temp.add(lastList.get(i));
				resultList.add(temp);
			}
		}
		// 删除可以拼接为高阶完全图的低阶子图，如果当前阶达不到阈值，则不用操作。
		if (currrntRegionSize >= recordRegionSize) {
			for (List<String> list : resultList) {
				for (int i = 0; i < list.size(); i++) {
					List<String> tempList = new ArrayList<String>();
					tempList.addAll(list);
					tempList.remove(tempList.get(i));
					removeSet.add(tempList);
				}
			}
			srcList.removeAll(removeSet);
		}
		return resultList;
	}
}
