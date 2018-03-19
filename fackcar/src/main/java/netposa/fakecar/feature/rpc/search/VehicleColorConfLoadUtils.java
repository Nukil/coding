package netposa.fakecar.feature.rpc.search;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 加载classpath中的vehicle_color_conf.xml配置文件,并生成SimilarVehicleTableDef实例
 * @author dl
 *
 */
public class VehicleColorConfLoadUtils {
	
	private static final Logger LOG = LoggerFactory.getLogger(VehicleColorConfLoadUtils.class);
	
	private static final String conf_name = "vehicle_color_conf.xml";
	
	public static Map<String,String> loadConfigByClassPath(){
		Map<String,String> map = new HashMap<String, String>();
		InputStream inStream = null;
		try {
			inStream = VehicleColorConfLoadUtils.class.getClassLoader().getResourceAsStream(conf_name);
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(inStream);
			NodeList list = doc.getElementsByTagName("VehicleColor");
			if (list.getLength() < 1) {
				throw new IOException("input vehicle_color_conf.xml not found VehicleColor element");
			}
			Node VehicleColor = list.item(0);
			NodeList attrList = VehicleColor.getChildNodes();
			int size = attrList.getLength();
			
			for(int i=0; i<size; i++) {
				Node attr = attrList.item(i);
				if (attr.getNodeName().startsWith("#")) {
					continue;
				}
				if (attr.getNodeName().equals("filed")) {
					NamedNodeMap nameNode = attr.getAttributes();
					String pcc = null;
					String vim = null;
					for(int j=0;j<nameNode.getLength();j++){
						Node node = nameNode.item(j);
						if(node != null){
							if(node.getNodeName().equals("pcc")){
								pcc = node.getNodeValue();
							}else if(node.getNodeName().equals("vim")){
								vim = node.getNodeValue();
							}
						}
					}
					if(pcc != null & vim != null){
						map.put(vim, pcc);
					}
				}
			}
			
			LOG.info("load config file content success !");
			
		} catch (Exception e) {
			LOG.warn("Runing error => " + e.getMessage(),e);
			e.printStackTrace();
		}finally {
			if (null != inStream) {
				try {
					inStream.close();
				} catch (IOException e) {
					LOG.warn("close stream error => " + e.getMessage(),e);
				}
			}
		}
	
		return map;
	}
	
}
