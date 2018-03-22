package netposa.firstincity.analysis.lib.db;

import java.sql.SQLException;

import scala.Serializable;


public class OpaqInputKey implements Serializable {

    private static final long serialVersionUID = 2902625683721697980L;

    private String plateNum;
    private String plateNumColor;

    public OpaqInputKey() {
    }

    public OpaqInputKey(String plateNum, String plateNumColor) {
        this.plateNum = plateNum;
        this.plateNumColor = plateNumColor;
    }

    public boolean readFields(String plateNum, String plateColor) throws SQLException {
        this.plateNum = plateNum;
        this.plateNumColor = plateColor;
        return true;
    }

    @Override
    public String toString() {
        return "OpaqInputKey [plateNum=" + plateNum + ", plateNumColor="
                + plateNumColor + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((plateNum == null) ? 0 : plateNum.hashCode());
        result = prime * result
                + ((plateNumColor == null) ? 0 : plateNumColor.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OpaqInputKey other = (OpaqInputKey) obj;
        if (plateNum == null) {
            if (other.plateNum != null)
                return false;
        } else if (!plateNum.equals(other.plateNum))
            return false;
        if (plateNumColor == null) {
            if (other.plateNumColor != null)
                return false;
        } else if (!plateNumColor.equals(other.plateNumColor))
            return false;
        return true;
    }

    public String getPlateNum() {
        return plateNum;
    }

    public void setPlateNum(String plateNum) {
        this.plateNum = plateNum;
    }

    public String getPlateNumColor() {
        return plateNumColor;
    }

    public void setPlateNumColor(String plateNumColor) {
        this.plateNumColor = plateNumColor;
    }

    public byte[] getPlateAndColor() {
        byte[] hphm = this.plateNum.getBytes();
        byte[] hpys = this.plateNumColor.getBytes();
        byte[] hphm_hpys = new byte[hphm.length + hpys.length];
        System.arraycopy(hphm, 0, hphm_hpys, 0, hphm.length);
        System.arraycopy(hpys, 0, hphm_hpys, hphm.length, hpys.length);
        return hphm_hpys;
    }

}
