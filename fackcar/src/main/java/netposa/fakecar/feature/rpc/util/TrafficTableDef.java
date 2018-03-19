package netposa.fakecar.feature.rpc.util;
/**
 * 用于与traffic_table_conf.xml配置文件内容进行对应的java类
 * @author hongs.yang
 *
 */
public class TrafficTableDef {
	
	private String tableName;
	private String driverClass;
	private String url;
	private String user;
	private String password;
	
	private String jlbh;
	private String xzqh;
	private String kkbh;
	
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
	public String getJlbh() {
		return jlbh;
	}
	public void setJlbh(String jlbh) {
		this.jlbh = jlbh;
	}
	public String getXzqh() {
		return xzqh;
	}
	public void setXzqh(String xzqh) {
		this.xzqh = xzqh;
	}
	public String getKkbh() {
		return kkbh;
	}
	public void setKkbh(String kkbh) {
		this.kkbh = kkbh;
	}
	@Override
	public String toString() {
		return "FeatureTableDef [tableName=" + tableName + ", driverClass="
				+ driverClass + ", url=" + url + ", user=" + user
				+ ", password=" + password + ", jlbh=" + jlbh + ", xzqh=" + xzqh + ", kkbh=" + kkbh + "]";
	}
}
