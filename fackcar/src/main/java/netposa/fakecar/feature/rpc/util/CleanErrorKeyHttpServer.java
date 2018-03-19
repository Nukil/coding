package netposa.fakecar.feature.rpc.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.concurrent.Executors;

import netposa.fakecar.feature.rpc.FeatureCalculatorServiceImpl;
import netposa.fakecar.feature.rpc.KeyRecord;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class CleanErrorKeyHttpServer {
	private static final Logger LOG = LoggerFactory
		      .getLogger(CleanErrorKeyHttpServer.class);
	
	private FeatureCalculatorServiceImpl fakecarServerHandler;
	private int port = 30078;
	
	public CleanErrorKeyHttpServer(FeatureCalculatorServiceImpl fakecarServerHandler) {
		this.fakecarServerHandler  = fakecarServerHandler;
	}
	
	public CleanErrorKeyHttpServer(FeatureCalculatorServiceImpl fakecarServerHandler, int port) {
		this.fakecarServerHandler  = fakecarServerHandler;
		this.port = port;
	}
	
	public void start() throws Exception {
		if(fakecarServerHandler == null) {
			throw new Exception("fakecarServerHandler is null, http start failure");
		}
		InetSocketAddress addr = new InetSocketAddress(port);  
        HttpServer server = HttpServer.create(addr, 0);  
  
        server.createContext("/fakeserver/cleanErrorKey", new CleanErrorKeyHandler());  
        server.setExecutor(Executors.newCachedThreadPool());  
        server.start();
        
        LOG.debug("clean error key http server started!");
	}
	
	class CleanErrorKeyHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			ResultVO resultVO = new ResultVO();
			LOG.debug("http server request!");
			String queryStr = exchange.getRequestURI().getRawQuery();
			String[] params = queryStr.split("&");
			String plateNumber = null;
			String plateColor = null;
			int headRear = Byte.MIN_VALUE;
			for(String param : params) {
				if(StringUtils.isNotBlank(param)) {
					String[] kv = param.split("=");
					if("plateNumber".equals(kv[0])) {
						plateNumber = kv[1];
					} else if("plateColor".equals(kv[0])) {
						plateColor = kv[1];
					} else if("headRear".equals(kv[0])) {
						headRear = Integer.valueOf(kv[1]);
					}
				}
			}
			plateNumber = URLDecoder.decode(plateNumber, "utf-8");
			if(StringUtils.isNotBlank(plateNumber)) {
				if(plateColor == null) {
					plateColor = "";
				}
				KeyRecord key = fakecarServerHandler.getKeyRecord(headRear, plateNumber.getBytes(), plateColor.getBytes());
				int hash_code = Math.abs(key.hashCode()) % fakecarServerHandler.vehicles.length;
			    if (fakecarServerHandler.vehicles[hash_code] != null) {
			    	fakecarServerHandler.vehicles[hash_code].remove(key);
			    	resultVO.setCode(200);
			    } else {
			    	resultVO.setCode(400);
			    	resultVO.setErrorMsg("can not find map!!");
			    }
			} else {
				resultVO.setCode(400);
		    	resultVO.setErrorMsg("plateNumber is null");
			}
			
			response(resultVO, exchange);
		}
		
		private void response(ResultVO resultVO, HttpExchange exchange) throws IOException {
			Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, 0);  
  
            OutputStream responseBody = exchange.getResponseBody();  
            responseBody.write(JsonUtil.toJson(resultVO).getBytes());  
            responseBody.close(); 
		}
		
	}
	
	class ResultVO {
		private Integer code = 200;
		private String errorMsg;
		public Integer getCode() {
			return code;
		}
		public void setCode(Integer code) {
			this.code = code;
		}
		public String getErrorMsg() {
			return errorMsg;
		}
		public void setErrorMsg(String errorMsg) {
			this.errorMsg = errorMsg;
		}
	}
}
