package netposa.fakecar.feature.rpc.util;
/**
 * 用于与feature_table_conf.xml配置文件内容进行对应的java类
 * @author hongs.yang
 *
 */
public class FeatureTableDef {
	
	private String tableName;
	private String driverClass;
	private String url;
	private String user;
	private String password;
	
	private String licencePlate;
	private String firstLicencePlateColor;
	private String secondLicencePlateColor;
	private String firstVehicleColor;
	private String secondVehicleColor;
	private String firstVehicleType;
	private String secondVehicleType;
	private String firstVehicleLogo;
	private String secondVehicleLogo;
	private String firstVehicleChildLogo;
	private String secondVehicleChildLogo;
	private String firstVehicleStyle;
	private String secondVehicleStyle;
	private String firstTrafficId;
	private String secondTrafficId;
	private String type;
	private String storeTime;
	private String firstMonitorId;
	private String firstOrgId;
	private String secondMonitorId;
	private String secondOrgId;
	
	
	
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
	public String getLicencePlate() {
		return licencePlate;
	}
	public void setLicencePlate(String licencePlate) {
		this.licencePlate = licencePlate;
	}
	public String getFirstLicencePlateColor() {
		return firstLicencePlateColor;
	}
	public void setFirstLicencePlateColor(String firstLicencePlateColor) {
		this.firstLicencePlateColor = firstLicencePlateColor;
	}
	public String getSecondLicencePlateColor() {
		return secondLicencePlateColor;
	}
	public void setSecondLicencePlateColor(String secondLicencePlateColor) {
		this.secondLicencePlateColor = secondLicencePlateColor;
	}
	public String getFirstVehicleColor() {
		return firstVehicleColor;
	}
	public void setFirstVehicleColor(String firstVehicleColor) {
		this.firstVehicleColor = firstVehicleColor;
	}
	public String getSecondVehicleColor() {
		return secondVehicleColor;
	}
	public void setSecondVehicleColor(String secondVehicleColor) {
		this.secondVehicleColor = secondVehicleColor;
	}
	public String getFirstVehicleType() {
		return firstVehicleType;
	}
	public void setFirstVehicleType(String firstVehicleType) {
		this.firstVehicleType = firstVehicleType;
	}
	public String getSecondVehicleType() {
		return secondVehicleType;
	}
	public void setSecondVehicleType(String secondVehicleType) {
		this.secondVehicleType = secondVehicleType;
	}
	public String getFirstVehicleLogo() {
		return firstVehicleLogo;
	}
	public void setFirstVehicleLogo(String firstVehicleLogo) {
		this.firstVehicleLogo = firstVehicleLogo;
	}
	public String getSecondVehicleLogo() {
		return secondVehicleLogo;
	}
	public void setSecondVehicleLogo(String secondVehicleLogo) {
		this.secondVehicleLogo = secondVehicleLogo;
	}
	public String getFirstVehicleChildLogo() {
		return firstVehicleChildLogo;
	}
	public void setFirstVehicleChildLogo(String firstVehicleChildLogo) {
		this.firstVehicleChildLogo = firstVehicleChildLogo;
	}
	public String getSecondVehicleChildLogo() {
		return secondVehicleChildLogo;
	}
	public void setSecondVehicleChildLogo(String secondVehicleChildLogo) {
		this.secondVehicleChildLogo = secondVehicleChildLogo;
	}
	public String getFirstVehicleStyle() {
		return firstVehicleStyle;
	}
	public void setFirstVehicleStyle(String firstVehicleStyle) {
		this.firstVehicleStyle = firstVehicleStyle;
	}
	public String getSecondVehicleStyle() {
		return secondVehicleStyle;
	}
	public void setSecondVehicleStyle(String secondVehicleStyle) {
		this.secondVehicleStyle = secondVehicleStyle;
	}
	public String getFirstTrafficId() {
		return firstTrafficId;
	}
	public void setFirstTrafficId(String firstTrafficId) {
		this.firstTrafficId = firstTrafficId;
	}
	public String getSecondTrafficId() {
		return secondTrafficId;
	}
	public void setSecondTrafficId(String secondTrafficId) {
		this.secondTrafficId = secondTrafficId;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getStoreTime() {
		return storeTime;
	}
	public void setStoreTime(String storeTime) {
		this.storeTime = storeTime;
	}
	
	
	
	public String getFirstMonitorId() {
		return firstMonitorId;
	}
	public void setFirstMonitorId(String firstMonitorId) {
		this.firstMonitorId = firstMonitorId;
	}
	public String getFirstOrgId() {
		return firstOrgId;
	}
	public void setFirstOrgId(String firstOrgId) {
		this.firstOrgId = firstOrgId;
	}
	public String getSecondMonitorId() {
		return secondMonitorId;
	}
	public void setSecondMonitorId(String secondMonitorId) {
		this.secondMonitorId = secondMonitorId;
	}
	public String getSecondOrgId() {
		return secondOrgId;
	}
	public void setSecondOrgId(String secondOrgId) {
		this.secondOrgId = secondOrgId;
	}
	@Override
	public String toString() {
		return "FeatureTableDef [tableName=" + tableName + ", driverClass="
				+ driverClass + ", url=" + url + ", user=" + user
				+ ", password=" + password + ", licencePlate=" + licencePlate
				+ ", firstLicencePlateColor=" + firstLicencePlateColor
				+ ", secondLicencePlateColor=" + secondLicencePlateColor
				+ ", firstVehicleColor=" + firstVehicleColor
				+ ", secondVehicleColor=" + secondVehicleColor
				+ ", firstVehicleType=" + firstVehicleType
				+ ", secondVehicleType=" + secondVehicleType
				+ ", firstVehicleLogo=" + firstVehicleLogo
				+ ", secondVehicleLogo=" + secondVehicleLogo
				+ ", firstVehicleChildLogo=" + firstVehicleChildLogo
				+ ", secondVehicleChildLogo=" + secondVehicleChildLogo
				+ ", firstVehicleStyle=" + firstVehicleStyle
				+ ", secondVehicleStyle=" + secondVehicleStyle
				+ ", firstTrafficId=" + firstTrafficId + ", secondTrafficId="
				+ secondTrafficId + ", type=" + type + ", storeTime="
				+ storeTime + ", firstMonitorId=" + firstMonitorId + ", firstOrgId=" + firstOrgId
				+ ", secondMonitor=" + secondMonitorId + ", secondOrgId=" + secondOrgId + "]";
	}
	
	
	
	
	
	

}
