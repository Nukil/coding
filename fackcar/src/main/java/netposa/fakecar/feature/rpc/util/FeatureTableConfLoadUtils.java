package netposa.fakecar.feature.rpc.util;

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
 * 加载classpath中的feature_table_conf.xml配置文件,并生成FeatureTableDef实例
 * @author hongs.yang
 *
 */
public class FeatureTableConfLoadUtils {
	private static final Logger LOG = LoggerFactory.getLogger(FeatureTableConfLoadUtils.class);
	
	private static final String conf_name = "feature_table_conf.xml";
	
	public static FeatureTableDef loadConfigByClassPath() throws Exception{
		InputStream inStream = null;
		try {
			inStream = FeatureTableConfLoadUtils.class.getClassLoader()
					.getResourceAsStream(conf_name);
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(inStream);
			NodeList list = doc.getElementsByTagName("tabledef");
			if (list.getLength() < 1) {
				throw new IOException("input feature_table_conf.xml not found tabledef element");
			}
			Node tabledef = list.item(0);
			NodeList attrList = tabledef.getChildNodes();
			int size = attrList.getLength();
			
			FeatureTableDef table = new FeatureTableDef();
			for(int i=0; i<size; i++) {
				Node attr = attrList.item(i);
				if (attr.getNodeName().startsWith("#")) {
					continue;
				}
				if (attr.getNodeName().contains("table")) {
					loadResultConfTableAttr(attr,table);
				}else if (attr.getNodeName().contains("fields")) {
					loadResultConfFieldsAttr(attr,table);
				}else if (attr.getNodeName().contains("connection")) {
					loadResultConfConnectAttr(attr,table);
				}
			}
			
			LOG.info("load config file content is => "+ table.toString());
			
			return table;
		}finally {
			if (null != inStream) {
				try {
					inStream.close();
				} catch (IOException e) {
					LOG.warn("close stream error => " + e.getMessage(),e);
				}
			}
		}
	}
	
	
	/**
	 * 加载配置文件中fields配置中的信息
	 * @param fields
	 * @param areaConf
	 */
	private static void loadResultConfFieldsAttr(Node fields, FeatureTableDef obj) {
		NodeList list = fields.getChildNodes();
		int size = list.getLength();
		for(int i=0; i<size; i++) {
			Node prop = list.item(i);
			if (prop.getNodeName().equals("licencePlate")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String licencePlate = StringUtils.trimToNull(attr.getTextContent());
					obj.setLicencePlate(licencePlate);
				}
			}else if (prop.getNodeName().equals("firstLicencePlateColor")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String firstLicencePlateColor = StringUtils.trimToNull(attr.getTextContent());
					obj.setFirstLicencePlateColor(firstLicencePlateColor);
				}
			}else if (prop.getNodeName().equals("secondLicencePlateColor")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String secondLicencePlateColor = StringUtils.trimToNull(attr.getTextContent());
					obj.setSecondLicencePlateColor(secondLicencePlateColor);
				}
			}else if (prop.getNodeName().equals("firstVehicleColor")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String firstVehicleColor = StringUtils.trimToNull(attr.getTextContent());
					obj.setFirstVehicleColor(firstVehicleColor);
				}
			}else if (prop.getNodeName().equals("secondVehicleColor")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String secondVehicleColor = StringUtils.trimToNull(attr.getTextContent());
					obj.setSecondVehicleColor(secondVehicleColor);
				}
			}else if (prop.getNodeName().equals("firstVehicleType")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String firstVehicleType = StringUtils.trimToNull(attr.getTextContent());
					obj.setFirstVehicleType(firstVehicleType);
				}
			}else if (prop.getNodeName().equals("secondVehicleType")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String secondVehicleType = StringUtils.trimToNull(attr.getTextContent());
					obj.setSecondVehicleType(secondVehicleType);
				}
			}else if (prop.getNodeName().equals("firstVehicleLogo")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String firstVehicleLogo = StringUtils.trimToNull(attr.getTextContent());
					obj.setFirstVehicleLogo(firstVehicleLogo);
				}
			}else if (prop.getNodeName().equals("secondVehicleLogo")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String secondVehicleLogo = StringUtils.trimToNull(attr.getTextContent());
					obj.setSecondVehicleLogo(secondVehicleLogo);
				}
			}else if (prop.getNodeName().equals("firstVehicleChildLogo")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String firstVehicleChildLogo = StringUtils.trimToNull(attr.getTextContent());
					obj.setFirstVehicleChildLogo(firstVehicleChildLogo);
				}
			}else if (prop.getNodeName().equals("secondVehicleChildLogo")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String secondVehicleChildLogo = StringUtils.trimToNull(attr.getTextContent());
					obj.setSecondVehicleChildLogo(secondVehicleChildLogo);
				}
			}else if (prop.getNodeName().equals("firstVehicleStyle")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String firstVehicleStyle = StringUtils.trimToNull(attr.getTextContent());
					obj.setFirstVehicleStyle(firstVehicleStyle);
				}
			}else if (prop.getNodeName().equals("secondVehicleStyle")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String secondVehicleStyle = StringUtils.trimToNull(attr.getTextContent());
					obj.setSecondVehicleStyle(secondVehicleStyle);
				}
			}else if (prop.getNodeName().equals("firstTrafficId")) {
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
			}else if (prop.getNodeName().contains("storeTime")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String storeTime = StringUtils.trimToNull(attr.getTextContent());
					obj.setStoreTime(storeTime);
				}
			}else if (prop.getNodeName().contains("type")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String type = StringUtils.trimToNull(attr.getTextContent());
					obj.setType(type);
				}
			}else if(prop.getNodeName().contains("firstTrafficMonitorId")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String monitorId = StringUtils.trimToNull(attr.getTextContent());
					obj.setFirstMonitorId(monitorId);
				}
			}else if(prop.getNodeName().contains("firstTrafficOrgId")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String orgId = StringUtils.trimToNull(attr.getTextContent());
					obj.setFirstOrgId(orgId);
				}
			}else if(prop.getNodeName().contains("secondTrafficMonitorId")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String monitorId = StringUtils.trimToNull(attr.getTextContent());
					obj.setSecondMonitorId(monitorId);
				}
			}else if(prop.getNodeName().contains("secondTrafficOrgId")) {
				NodeList attrs = prop.getChildNodes();
				int aSize = attrs.getLength();
				for(int j=0; j<aSize; j++) {
					Node attr = attrs.item(j);
					if (!attr.getNodeName().contains("field")) {
						continue;
					}
					String orgId = StringUtils.trimToNull(attr.getTextContent());
					obj.setSecondOrgId(orgId);
				}
			}
		}
	}
	
	/**
	 * 加载配置文件中table属性的内容
	 * @param table
	 * @param areaConf
	 */
	private static void loadResultConfTableAttr(Node table, FeatureTableDef obj) {
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
	private static void loadResultConfConnectAttr(Node connection, FeatureTableDef obj) {
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
