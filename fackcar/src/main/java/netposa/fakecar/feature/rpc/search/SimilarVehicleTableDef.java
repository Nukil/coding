package netposa.fakecar.feature.rpc.search;
/**
 * 用于与similar_vehicle_table_conf.xml配置文件内容进行对应的java类
 * @author dl
 *
 */
public class SimilarVehicleTableDef {
	
	private String tableName;
	private String driverClass;
	private String url;
	private String user;
	private String password;
	
	private String firstTrafficId;
	private String firstSimilarTrafficId;
	private String secondTrafficId;
	private String secondSimilarTrafficId;
	
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public String getDriverClass() {
		return driverClass;
	}
	public void setDriverClass(String driverClass) {
		this.driverClass = driverClass;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getFirstTrafficId() {
		return firstTrafficId;
	}
	public void setFirstTrafficId(String firstTrafficId) {
		this.firstTrafficId = firstTrafficId;
	}
	public String getFirstSimilarTrafficId() {
		return firstSimilarTrafficId;
	}
	public void setFirstSimilarTrafficId(String firstSimilarTrafficId) {
		this.firstSimilarTrafficId = firstSimilarTrafficId;
	}
	public String getSecondTrafficId() {
		return secondTrafficId;
	}
	public void setSecondTrafficId(String secondTrafficId) {
		this.secondTrafficId = secondTrafficId;
	}
	public String getSecondSimilarTrafficId() {
		return secondSimilarTrafficId;
	}
	public void setSecondSimilarTrafficId(String secondSimilarTrafficId) {
		this.secondSimilarTrafficId = secondSimilarTrafficId;
	}
	
	@Override
	public String toString() {
		return "FeatureTableDef [tableName=" + tableName + ", driverClass="
				+ driverClass + ", url=" + url + ", user=" + user
				+ ", password=" + password + 
				", firstTrafficId=" + firstTrafficId + 
				", firstSimilarTrafficId=" + firstSimilarTrafficId + 
				", secondTrafficId=" + secondTrafficId + 
				", secondSimilarTrafficId=" + secondSimilarTrafficId + 
				"]";
	}
	
	
	
	
	
	

}
