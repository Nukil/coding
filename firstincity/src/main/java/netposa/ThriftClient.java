package netposa;

import java.util.Map;

import netposa.firstincity.rpc.FisrtIntoSearchRpcService;
import netposa.firstincity.rpc.InputRecord;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
public class ThriftClient {

	public static final Log log = LogFactory.getLog(ThriftClient.class);
 
	/**
	 *
	 * @param record
	 */
	public static Map<Integer, Boolean> startClient(InputRecord record) {
		TTransport transport = null;
		String ip = "192.168.61.58";
		String port = "30097";
		String timeout = "10000";
		try {
			transport = new TSocket(ip, Integer.parseInt(port), Integer.parseInt(timeout));
			// 协议要和服务端一致
			TProtocol protocol = new TBinaryProtocol(transport);
			FisrtIntoSearchRpcService.Client client = new FisrtIntoSearchRpcService.Client(protocol);
			transport.open();
			Map<Integer, Boolean> result = client.judgeFirstInto(record);
			log.debug("Thrify client result =: " + result);
			return result;
		} catch (Exception e) {
			log.error("connect thrift server error,server=="+ip+":"+port,e);
		} finally {
			if (null != transport) {
				transport.close();
			}
		}
		return null;
	}
 
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		InputRecord r = new InputRecord();
		r.setLicence_plate("沪AA2M513".getBytes());
		r.setLicence_plate_color("2".getBytes());
		r.setPass_time("2016-06-05 12:12:01".getBytes());
		startClient(r);
	}


	public Map<Integer, Boolean> checkFirstInCity(String plateNumber, String plateColor) {
		InputRecord r = new InputRecord();
		r.setLicence_plate(plateNumber.getBytes());
		r.setLicence_plate_color(plateColor.getBytes());
		return startClient(r);
	}
}
