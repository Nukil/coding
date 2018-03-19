package com.netposa.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class GJsonUtil {
	private static Gson gson = null;
	static {
		gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
	}
	public static String toJson(Object obj) {
		if(obj == null) {
			return null;
		}
		return gson.toJson(obj);
	}
	
	public static <T>T jsonToObject(String json, Class<T> clz) {
		return gson.fromJson(json, clz);
	}
	
	public static <T> List<T> jsonToList(String json, Class<T> clz) {
		if(StringUtils.isBlank(json)) {
			return null;
		}
		List<Object> objList = gson.fromJson(json, (new TypeToken<List<T>>(){}).getType());
		List<T> listOfT = new ArrayList<T>();
		for (Object obj : objList) {
			listOfT.add(gson.fromJson(toJson(obj), clz));
		}
		return listOfT;
	}
	
	public static <T> Map<String, T> jsonToMap(String json, Class<T> clz) {
		if(StringUtils.isBlank(json)) {
			return null;
		} 
		Map<String, Object> objMap = gson.fromJson(json, (new TypeToken<Map<String, T>>(){}).getType());
		Map<String, T> resultMap = new HashMap<String, T>();
		for(String key : objMap.keySet()) {
			resultMap.put(key, gson.fromJson(toJson(objMap.get(key)), clz));
		}
		return resultMap;
	}
	

}
