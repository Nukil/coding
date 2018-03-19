package netposa.fakecar.feature.rpc.bdk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.concurrent.Executors;

import netposa.fakecar.feature.rpc.FeatureCalculatorServiceImpl;
import netposa.fakecar.feature.rpc.ValueRecord;
import netposa.fakecar.feature.rpc.util.JsonUtil;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.BASE64Decoder;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * 找原车http服务
 * 目前只给八大库回调
 * @author guojiawei
 */
public class SimilarVehicleHttpServer {
	private static final Logger LOG = LoggerFactory
		      .getLogger(SimilarVehicleHttpServer.class);
	
	private FeatureCalculatorServiceImpl fakecarServerHandler;
	//服务端口
	private int port;
	
	public SimilarVehicleHttpServer(FeatureCalculatorServiceImpl fakecarServerHandler, int port) {
		this.fakecarServerHandler = fakecarServerHandler;
		this.port = port;
	}
	
	/**
	 * 启动http服务
	 * @throws Exception
	 */
	public void start() throws Exception {
		if(fakecarServerHandler == null) {
			throw new Exception("fakecarServerHandler is null, http start failure");
		}
		InetSocketAddress addr = new InetSocketAddress(port);  
        HttpServer server = HttpServer.create(addr, 0);  
  
        server.createContext("/fakeserver/bdkSimilarVehicle", new SimilarVehicleHttpHandler());  
        server.setExecutor(Executors.newCachedThreadPool());  
        server.start();
        
        LOG.debug("http server started!");
	}
	
	/**
	 * http请求处理
	 */
	class SimilarVehicleHttpHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			ResultVO resultVO = new ResultVO();
			String requestMethod = exchange.getRequestMethod();
			if(requestMethod.equalsIgnoreCase("POST")) {
				LOG.debug("http server request!");
				//找原车
	            if(fakecarServerHandler.similar_vehicle_search_enable){
	            	InputStream in = exchange.getRequestBody();
	            	if(in == null) {
	            		resultVO.setErrorMsg("param error");
	            		response(resultVO, exchange);
	            		return;
	            	}
	            	byte[] b = new byte[4096];
	            	StringBuffer out = new StringBuffer();
					for (int n; (n = in.read(b)) != -1;) {
						out.append(new String(b, 0, n));
					}
	            	
	            	String vehicleInfo = out.toString();
	            	vehicleInfo = URLDecoder.decode(vehicleInfo, "utf-8");
	            	TaskContent content = null;
	            	try {
	            		content = JsonUtil.jsonToObject(vehicleInfo, TaskContent.class);
	            	} catch(Exception e) {
	            		e.printStackTrace();
	            	}
	            	
	            	if(content == null || StringUtils.isBlank(content.getPlateNumber())) {
	            		resultVO.setErrorMsg("platenumber is null");
	            		response(resultVO, exchange);
	            		return;
	            	}
	            	
	            	ValueRecord record = new ValueRecord();
	            	if(StringUtils.isNotBlank(content.getPlateColor())) {
	            		record.setLicence_plate_color(content.getPlateColor().getBytes());
	            	}
	            	if(StringUtils.isNotBlank(content.getOldPlate())) {
	            		record.setOld_licence_plate(content.getOldPlate().getBytes());
	            	}
	            	if(StringUtils.isNotBlank(content.getTrafficId())) {
	            		record.setRecord_id(content.getTrafficId().getBytes());
	            	}
	            	if(StringUtils.isNotBlank(content.getVehicleColor())) {
	            		record.setVehicle_color(content.getVehicleColor().getBytes());
	            	}
	            	if(StringUtils.isNotBlank(content.getVehicleFeatureBuffer())) {
	            		BASE64Decoder decoder = new BASE64Decoder();
	            		record.setVehicle_feature_buffer(decoder.decodeBuffer(content.getVehicleFeatureBuffer()));
	            	}
	            	if(StringUtils.isNotBlank(content.getVehicleLogo())) {
	            		record.setVehicle_logo(content.getVehicleLogo().getBytes());
	            	}
	            	if(StringUtils.isNotBlank(content.getVehicleStyle())) {
	            		record.setVehicle_style(content.getVehicleStyle().getBytes());
	            	}
	            	if(StringUtils.isNotBlank(content.getVehicleSubLogo())) {
	            		record.setVehicle_child_logo(content.getVehicleSubLogo().getBytes());
	            	}
	            	if(StringUtils.isNotBlank(content.getVehicleType())) {
	            		record.setVehicle_type(content.getVehicleType().getBytes());
	            	}
	            	
	            	fakecarServerHandler.searchThread.addBdkValue(record);
	            }
			} else {
				resultVO.setErrorMsg("GET is forbidden");
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
