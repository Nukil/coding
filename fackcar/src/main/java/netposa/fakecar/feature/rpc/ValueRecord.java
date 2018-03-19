package netposa.fakecar.feature.rpc;

import java.io.Serializable;

public class ValueRecord implements Serializable {

  /**
	 * 
	 */
  private static final long serialVersionUID = 1234036535742581670L;

  private byte[] vehicle_color = null;
  private byte[] vehicle_type = null;
  private byte[] vehicle_logo = null;
  private byte[] vehicle_child_logo = null;
  private byte[] vehicle_style = null;
  private float[] vehicle_feature = null;
  private byte[] vehicle_feature_buffer = null;
  private byte[] record_id = null;
  private byte[] licence_plate_color;
  private byte[] old_licence_plate = null;
  private byte[] kkbh = null;
  private byte[] xzqh = null;
  private byte headrear = -1;

  private long initTime = System.currentTimeMillis();

  public ValueRecord() {
  }

  public byte[] getVehicle_color() {
    return vehicle_color;
  }

  public void setVehicle_color(byte[] vehicle_color) {
    this.vehicle_color = vehicle_color;
  }

  public byte[] getVehicle_type() {
    return vehicle_type;
  }

  public void setVehicle_type(byte[] vehicle_type) {
    this.vehicle_type = vehicle_type;
  }

  public byte[] getVehicle_logo() {
    return vehicle_logo;
  }

  public void setVehicle_logo(byte[] vehicle_logo) {
    this.vehicle_logo = vehicle_logo;
  }

  public byte[] getVehicle_child_logo() {
    return vehicle_child_logo;
  }

  public void setVehicle_child_logo(byte[] vehicle_child_logo) {
    this.vehicle_child_logo = vehicle_child_logo;
  }

  public byte[] getVehicle_style() {
    return vehicle_style;
  }

  public void setVehicle_style(byte[] vehicle_style) {
    this.vehicle_style = vehicle_style;
  }

  public float[] getVehicle_feature() {
    return vehicle_feature;
  }

  public void setVehicle_feature(float[] vehicle_feature) {
    this.vehicle_feature = vehicle_feature;
  }

  public byte[] getRecord_id() {
    return record_id;
  }

  public void setRecord_id(byte[] record_id) {
    this.record_id = record_id;
  }

  public byte[] getLicence_plate_color() {
    return licence_plate_color;
  }

  public void setLicence_plate_color(byte[] licence_plate_color) {
    this.licence_plate_color = licence_plate_color;
  }

  public byte[] getVehicle_feature_buffer() {
    return vehicle_feature_buffer;
  }

  public void setVehicle_feature_buffer(byte[] vehicle_feature_buffer) {
    this.vehicle_feature_buffer = vehicle_feature_buffer;
  }

  public byte[] getOld_licence_plate() {
    return old_licence_plate;
  }

  public void setOld_licence_plate(byte[] old_licence_plate) {
    this.old_licence_plate = old_licence_plate;
  }

  public long getInitTime() {
    return initTime;
  }

  public byte[] getKkbh() {
	return kkbh;
  }

  public void setKkbh(byte[] kkbh) {
	this.kkbh = kkbh;
  }

  public byte[] getXzqh() {
	return xzqh;
  }

  public void setXzqh(byte[] xzqh) {
	this.xzqh = xzqh;
  }

  public void setHeadrear(byte headrear) { this.headrear = headrear; }

  public byte getHeadrear() { return headrear; }

}
