package netposa.firstincity.analysis.utils;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AnalysisUtil {
	
	private static long HOUR = 60 * 60 * 1000;
	private static SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private static long[] split(long numSplits, long minVal, long maxVal) {
		List<Long> list = new ArrayList<Long>();
		long splitSize = (maxVal - minVal) / numSplits;
		if (splitSize < 1) splitSize = 1;
		long curVal = minVal;
		while(curVal <= maxVal) {
			list.add(curVal);
			curVal += splitSize;
		}
		int size = list.size();
		if (list.get(size-1) != maxVal || size == 1) {
			list.add(maxVal);
		}
		long[] splits = new long[list.size()];
		int i=0;
		for(Long cur : list) {
			splits[i] = cur.longValue();
			i ++;
		}
		if (splits.length > 1 && (splits[splits.length-1] - splits[splits.length-2]) < HOUR) {
			splits[splits.length-2] = splits[splits.length-1];
			long[] newArr = new long[splits.length-1];
			System.arraycopy(splits, 0, newArr, 0, newArr.length);
			return newArr;
		}
		
		return splits;
	}
	
	public static List<String> getSplits(String searchDate,int tasks) throws IOException, InterruptedException {	
		
		List<String> splits = new ArrayList<String>();
		Date begin = null;
		Date end = null;
		try {
			begin = SDF.parse(searchDate + " 00:00:00");
			end = new Date(begin.getTime()+1000*60*60*24);
		} catch (ParseException e) {
			throw new IOException(e.getMessage(),e);
		}
		
		long[] splitPoints = split(tasks, begin.getTime(), end.getTime());
		
		long start = splitPoints[0];
		Date startDate = new Date(start);
		splits.add(SDF.format(startDate));
		
		for(int i=1; i<splitPoints.length; i++) {
			long stop = splitPoints[i];
			Date stopDate = new Date(stop);
			splits.add(SDF.format(stopDate));
			start = stop;
			startDate = stopDate;
		}
		
		return splits;
	}
	
}
