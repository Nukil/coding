package netposa.fakecar.feature.rpc.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class JsonUtil {
	private static Gson gson = new Gson();
	
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
		List<Object> objList = gson.fromJson(json, (new TypeToken<List<Object>>(){}).getType());
		List<T> listOfT = new ArrayList<>();
		for (Object obj : objList) {
			listOfT.add(gson.fromJson(toJson(obj), clz));
		}
		return listOfT;
	}
	
	public static <T> Map<String, T> jsonToMap(String json, Class<T> clz) {
		if(StringUtils.isBlank(json)) {
			return null;
		}
		Map<String, Object> objMap = gson.fromJson(json, (new TypeToken<Map<String, Object>>(){}).getType());
		Map<String, T> resultMap = new HashMap<String, T>();
		for(String key : objMap.keySet()) {
			resultMap.put(key, gson.fromJson(toJson(objMap.get(key)), clz));
		}
		return resultMap;
	}
	
	public static void main(String[] args) {
		List<A> aList = new ArrayList<A>();
		A a = new A();
		a.setName("a");
		a.setAddr("b");
		aList.add(a);
		System.out.println(toJson(aList));
		List<A> tempList = jsonToList(toJson(aList), A.class);
		System.out.println(tempList.get(0).getName());
	}
	
	static class A {
		private String name;
		private String addr;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getAddr() {
			return addr;
		}
		public void setAddr(String addr) {
			this.addr = addr;
		}
		
	}
}
