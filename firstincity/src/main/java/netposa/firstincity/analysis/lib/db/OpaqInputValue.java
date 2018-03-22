package netposa.firstincity.analysis.lib.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import scala.Serializable;

public class OpaqInputValue implements Serializable {

	private static final long serialVersionUID = 2902625683721697980L;
	
	private String jlbh;
	private long jgsj;
	private int clsd;
	private String xzqh;
	private String kkbh;
	private String cdbh;
	private String hphm;
	private String hpys;
	private String hpzl;
	private String csys;
	private String cllx;
	private String clpp;
	private String clzpp;
	private String clnk;

	private String searchDate;

	public OpaqInputValue(){}
	
	public boolean readFields(ResultSet rs, String searchDate) throws SQLException {
		
		jlbh = rs.getString("jlbh");
		jgsj = rs.getLong("jgsj");
		clsd = rs.getInt("clsd");
		xzqh = rs.getString("xzqh");
		kkbh = rs.getString("kkbh");
		cdbh = rs.getString("cdbh");
		hphm = rs.getString("hphm");
		hpys = rs.getString("hpys");
		hpzl = rs.getString("hpzl");
		csys = rs.getString("csys");
		cllx = rs.getString("cllx");
		clpp = rs.getString("clpp");
		clzpp = rs.getString("clzpp");
		clnk = rs.getString("clnk");
		
		this.searchDate = searchDate;
		
		return true;
	}

	@Override
	public String toString() {
		return "OpaqInputValue [jlbh=" + jlbh + ",jgsj=" + jgsj + ",clsd=" + clsd 
				+ ",xzqh=" + xzqh + ",kkbh=" + kkbh + ",cdbh=" + cdbh 
				+ ",hphm=" + hphm + ",hpys=" + hpys + ",hpzl=" + hpzl 
				+ ",csys=" + csys + ",cllx=" + cllx + ",clpp=" + clpp 
				+ ",clzpp=" + clzpp + ",clnk=" + clnk + ", searchDate=" + searchDate + "]";
	}

	public String getJlbh() {
		return jlbh;
	}

	public void setJlbh(String jlbh) {
		this.jlbh = jlbh;
	}

	public long getJgsj() {
		return jgsj;
	}

	public void setJgsj(long jgsj) {
		this.jgsj = jgsj;
	}

	public int getClsd() {
		return clsd;
	}

	public void setClsd(int clsd) {
		this.clsd = clsd;
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

	public String getCdbh() {
		return cdbh;
	}

	public void setCdbh(String cdbh) {
		this.cdbh = cdbh;
	}

	public String getHphm() {
		return hphm;
	}

	public void setHphm(String hphm) {
		this.hphm = hphm;
	}

	public String getHpys() {
		return hpys;
	}

	public void setHpys(String hpys) {
		this.hpys = hpys;
	}

	public String getHpzl() {
		return hpzl;
	}

	public void setHpzl(String hpzl) {
		this.hpzl = hpzl;
	}

	public String getCsys() {
		return csys;
	}

	public void setCsys(String csys) {
		this.csys = csys;
	}

	public String getCllx() {
		return cllx;
	}

	public void setCllx(String cllx) {
		this.cllx = cllx;
	}

	public String getClpp() {
		return clpp;
	}

	public void setClpp(String clpp) {
		this.clpp = clpp;
	}

	public String getClzpp() {
		return clzpp;
	}

	public void setClzpp(String clzpp) {
		this.clzpp = clzpp;
	}

	public String getClnk() {
		return clnk;
	}

	public void setClnk(String clnk) {
		this.clnk = clnk;
	}

	public String getSearchDate() {
		return searchDate;
	}

	public void setSearchDate(String searchDate) {
		this.searchDate = searchDate;
	}

}
