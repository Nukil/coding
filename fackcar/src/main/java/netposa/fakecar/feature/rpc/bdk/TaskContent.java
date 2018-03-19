package netposa.fakecar.feature.rpc.bdk;

public class TaskContent {

	// 车牌号
	private String plateNumber;
	// 车牌颜色
	private String plateColor;
	// 车身颜色
	private String vehicleColor;
	// 车辆类型
	private String vehicleType;
	// 车辆品牌
	private String vehicleLogo;
	// 车辆子品牌
	private String vehicleSubLogo;
	// 车辆年款
	private String vehicleStyle;
	// 过车记录编号
	private String trafficId;

	private String oldPlate;

	private String vehicleFeatureBuffer;
	// 车牌置信度
	private Double plateConfidence;
	// 车辆品牌置信度
	private Double brandConfidence;

	public String getPlateNumber() {
		return plateNumber;
	}

	public void setPlateNumber(String plateNumber) {
		this.plateNumber = plateNumber;
	}

	public String getPlateColor() {
		return plateColor;
	}

	public void setPlateColor(String plateColor) {
		this.plateColor = plateColor;
	}

	public String getVehicleColor() {
		return vehicleColor;
	}

	public void setVehicleColor(String vehicleColor) {
		this.vehicleColor = vehicleColor;
	}

	public String getVehicleType() {
		return vehicleType;
	}

	public void setVehicleType(String vehicleType) {
		this.vehicleType = vehicleType;
	}

	public String getVehicleLogo() {
		return vehicleLogo;
	}

	public void setVehicleLogo(String vehicleLogo) {
		this.vehicleLogo = vehicleLogo;
	}

	public String getVehicleSubLogo() {
		return vehicleSubLogo;
	}

	public void setVehicleSubLogo(String vehicleSubLogo) {
		this.vehicleSubLogo = vehicleSubLogo;
	}

	public String getVehicleStyle() {
		return vehicleStyle;
	}

	public void setVehicleStyle(String vehicleStyle) {
		this.vehicleStyle = vehicleStyle;
	}

	public String getTrafficId() {
		return trafficId;
	}

	public void setTrafficId(String trafficId) {
		this.trafficId = trafficId;
	}

	public String getOldPlate() {
		return oldPlate;
	}

	public void setOldPlate(String oldPlate) {
		this.oldPlate = oldPlate;
	}

	public String getVehicleFeatureBuffer() {
		return vehicleFeatureBuffer;
	}

	public void setVehicleFeatureBuffer(String vehicleFeatureBuffer) {
		this.vehicleFeatureBuffer = vehicleFeatureBuffer;
	}

	public Double getPlateConfidence() {
		return plateConfidence;
	}

	public void setPlateConfidence(Double plateConfidence) {
		this.plateConfidence = plateConfidence;
	}

	public Double getBrandConfidence() {
		return brandConfidence;
	}

	public void setBrandConfidence(Double brandConfidence) {
		this.brandConfidence = brandConfidence;
	}

	@Override
	public String toString() {
		return "TaskContent [plateNumber=" + plateNumber + ", plateColor="
				+ plateColor + ", vehicleColor=" + vehicleColor
				+ ", vehicleType=" + vehicleType + ", vehicleLogo="
				+ vehicleLogo + ", vehicleSubLogo=" + vehicleSubLogo
				+ ", vehicleStyle=" + vehicleStyle + ", trafficId=" + trafficId
				+ "]";
	}

}
