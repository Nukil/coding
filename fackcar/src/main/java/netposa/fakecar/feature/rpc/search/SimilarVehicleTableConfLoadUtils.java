package netposa.fakecar.feature.rpc.search;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 加载classpath中的similar_vehicle_table_conf.xml配置文件,并生成SimilarVehicleTableDef实例
 * @author dl
 *
 */
public class SimilarVehicleTableConfLoadUtils {
	private static final Logger LOG = LoggerFactory.getLogger(SimilarVehicleTableConfLoadUtils.class);
	
	private static final String conf_name = "similar_vehicle_table_conf.xml";
	
	public static SimilarVehicleTableDef loadConfigByClassPath(){
		InputStream inStream = null;
		SimilarVehicleTableDef tableDef = new SimilarVehicleTableDef();
		try {
			inStream = SimilarVehicleTableConfLoadUtils.class.getClassLoader()
					.getResourceAsStream(conf_name);
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(inStream);
			NodeList list = doc.getElementsByTagName("tabledef");
			if (list.getLength() < 1) {
				throw new IOException("input similar_vehicle_table_conf.xml not found tabledef element");
			}
			Node tabledef = list.item(0);
			NodeList attrList = tabledef.getChildNodes();
			int size = attrList.getLength();
			
			for(int i=0; i<size; i++) {
				Node attr = attrList.item(i);
				if (attr.getNodeName().startsWith("#")) {
					continue;
				}
				if (attr.getNodeName().contains("table")) {
					loadResultConfTableAttr(attr,tableDef);
				}else if (attr.getNodeName().contains("fields")) {
					loadResultConfFieldsAttr(attr,tableDef);
				}else if (attr.getNodeName().contains("connection")) {
					loadResultConfConnectAttr(attr,tableDef);
				}
			}
			
			LOG.info("load config file content is => "+ tableDef.toString());
			
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
		
		return tableDef;
	}
	
	
	/**
	 * 加载配置文件中fields配置中的信息
	 * @param fields
	 * @param areaConf
	 */
	private static void loadResultConfFieldsAttr(Node fields, SimilarVehicleTableDef obj) {
		NodeList list = fields.getChildNodes();
		int size = list.getLength();
		for(int i=0; i<size; i++) {
			Node prop = list.item(i);
			if (prop.getNodeName().equals("firstTrafficId")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String firstTrafficId = StringUtils.trimToNull(attr.getTextContent());
					obj.setFirstTrafficId(firstTrafficId);
				}
			}else if (prop.getNodeName().equals("firstSimilarTrafficId")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String firstSimilarTrafficId = StringUtils.trimToNull(attr.getTextContent());
					obj.setFirstSimilarTrafficId(firstSimilarTrafficId);
				}
			}else if (prop.getNodeName().equals("secondTrafficId")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String secondTrafficId = StringUtils.trimToNull(attr.getTextContent());
					obj.setSecondTrafficId(secondTrafficId);
				}
			}else if (prop.getNodeName().equals("secondSimilarTrafficId")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String secondSimilarTrafficId = StringUtils.trimToNull(attr.getTextContent());
					obj.setSecondSimilarTrafficId(secondSimilarTrafficId);
				}
			}
		}
	}
	
	/**
	 * 加载配置文件中table属性的内容
	 * @param table
	 * @param areaConf
	 */
	private static void loadResultConfTableAttr(Node table, SimilarVehicleTableDef obj) {
		NodeList list = table.getChildNodes();
		int size = list.getLength();
		for(int i=0; i<size; i++) {
			Node prop = list.item(i);
			if (!prop.getNodeName().contains("name")) {
				continue;
			}
			String tableName = StringUtils.trimToNull(prop.getTextContent());
			obj.setTableName(tableName);
			break;
		}
	}
	
	/**
	 * 加载配置文件中connection部分的配置信息
	 * @param connection
	 * @param areaConf
	 */
	private static void loadResultConfConnectAttr(Node connection, SimilarVehicleTableDef obj) {
		NodeList list = connection.getChildNodes();
		int size = list.getLength();
		for(int i=0; i<size; i++) {
			Node prop = list.item(i);
			if (prop.getNodeName().contains("driverClass")) {
				obj.setDriverClass(StringUtils.trimToNull(prop.getTextContent()));
			}else if (prop.getNodeName().contains("url")) {
				obj.setUrl(StringUtils.trimToNull(prop.getTextContent()));
			}else if (prop.getNodeName().contains("user")) {
				obj.setUser(StringUtils.trimToNull(prop.getTextContent()));
			}else if (prop.getNodeName().contains("password")) {
				obj.setPassword(StringUtils.trimToNull(prop.getTextContent()));
			}
		}
	}

}
