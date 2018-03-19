package netposa.fakecar.feature.rpc.bdk;

import java.nio.charset.Charset;
import java.util.concurrent.LinkedBlockingQueue;

import netposa.fakecar.feature.rpc.InputRecordBuffer;
import netposa.fakecar.feature.rpc.util.JsonUtil;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.BASE64Encoder;

/**
 * 八大库对比套牌服务
 * @author guojiawei
 */
public class BdkCheckProcessor {
	
	private static final Logger LOG = LoggerFactory
		      .getLogger(BdkCheckProcessor.class);
	
	private static final Charset charset = Charset.forName("UTF-8");
	private static final int thread_num = 8;
	private static final int MAX_QUEUE_LENGHT = 10000000;
	private static final LinkedBlockingQueue<InputRecordBuffer> queue = new LinkedBlockingQueue<InputRecordBuffer>();
	
	//private static FakeVehicleCheckService.Client client = null;
	//private static TTransport transport = null;
	private static String ip;
	private static int port;
	private static int timeout;
	/**
	 * 启动八大库处理
	 */
	public static void start(String ip_addr, int p, int t) throws Exception {
		ip = ip_addr;
		port = p;
		timeout = t;
		
		for(int i=0;i<thread_num;i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					TTransport transport = new TSocket(ip, port, timeout);
					transport = new TFramedTransport(transport);
					TProtocol protocol = new TCompactProtocol(transport);
					FakeVehicleCheckService.Client client = new FakeVehicleCheckService.Client(protocol);
					
					while(true) {
						try {
							InputRecordBuffer record = queue.take();
							if(record != null) {
								try {
									handler(record, transport, client);
								} catch(Exception e) {
									e.printStackTrace();
								}
							}
						} catch (Exception e) {
							LOG.error("send record to bdk error", e);
						}
					}
				}
			}).start();
		}
		//自检测线程
		//防止数据积压，导致套牌分析崩溃
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					int size = queue.size();
					LOG.debug("bdk queue current size is: " + size);
					if(size >= (MAX_QUEUE_LENGHT / 2)) {
						LOG.error("bdk message is blocking!!!!size=" + size);
					}
					if(size >= MAX_QUEUE_LENGHT) {
						queue.clear();
					}
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
	/**
	 * 添加数据源
	 * @param record
	 */
	public static void execute(InputRecordBuffer record) {
		queue.add(record);
	}
	
	/**
	 * 处理数据源
	 * @param record
	 */
	private static void handler(InputRecordBuffer record, TTransport transport, FakeVehicleCheckService.Client client) throws Exception {
		TaskContent taskContent = new TaskContent();
		taskContent.setPlateNumber(new String(record.getLicence_plate(), charset));
		if(record.getLicence_plate_color() != null && record.getLicence_plate_color().length > 0)
			taskContent.setPlateColor(new String(record.getLicence_plate_color(), charset));
		if(record.getVehicle_color() != null && record.getVehicle_color().length > 0)
			taskContent.setVehicleColor(new String(record.getVehicle_color(), charset));
		if(record.getVehicle_logo() != null && record.getVehicle_logo().length > 0)
			taskContent.setVehicleLogo(new String(record.getVehicle_logo(), charset));
		if(record.getVehicle_child_logo() != null && record.getVehicle_child_logo().length > 0)
			taskContent.setVehicleSubLogo(new String(record.getVehicle_child_logo(), charset));
		if(record.getVehicle_style() != null && record.getVehicle_style().length > 0)
			taskContent.setVehicleStyle(new String(record.getVehicle_style(), charset));
		if(record.getVehicle_type() != null && record.getVehicle_type().length > 0)
			taskContent.setVehicleType(new String(record.getVehicle_type(), charset));
		if(record.getRecord_id() != null && record.getRecord_id().length > 0)
			taskContent.setTrafficId(new String(record.getRecord_id(), charset));
		if(record.getOld_licence_plate() != null && record.getOld_licence_plate().length > 0)
			taskContent.setOldPlate(new String(record.getOld_licence_plate(), charset));
		if(record.getVehicle_feature() != null && record.getVehicle_feature().length > 0) {
			BASE64Encoder encoder = new BASE64Encoder();
			taskContent.setVehicleFeatureBuffer(encoder.encode(record.getVehicle_feature()));
		}
		taskContent.setPlateConfidence(record.getPlate_confidence());
		taskContent.setBrandConfidence(record.getLogo_confidence());
		
		try {
			transport.open();
			client.checkFakeVehicle(JsonUtil.toJson(taskContent), "套牌", "0");
		} catch(TException e) {
			e.printStackTrace();
		} finally {
			transport.close();
		}
	}
}
