package netposa.realtime.blacklist.server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FliterBlackList {
	
	private static final Log logger = LogFactory.getLog(FliterBlackList.class);
	
	public static boolean compare(String plateNum,String regex) {
		
		// *鍙疯浆鎹㈡垚.+ , ?鍙疯浆鎴�
		if(!regex.contains("*") && !regex.contains("?")) {
			logger.debug("regex is not pattern... ... ...");
			return plateNum.equals(regex);
		}
		logger.debug("regex matcher... ... ...");
		regex = regex.replace("*", ".+");
		regex = regex.replace("?", ".");
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(plateNum);
		return m.matches();
	}

	public static void main(String[] args) {
		System.out.println(FliterBlackList.compare("陕A12345", "陕*"));
	}
	
}
