package netposa.fakecar.feature.rpc;
/**
 * 向数据库输出结果的类定义
 * @author hongs.yang
 *
 */
public class ResultRecord {
	
	private final KeyRecord key;
	private final ValueRecord firstValue;
	private final ValueRecord secondValue;
	
	public ResultRecord(ValueRecord firstValue) {
		this.key = null;
		this.firstValue = firstValue;
		this.secondValue = null;
	}
	
	public ResultRecord(KeyRecord key, 
			ValueRecord firstValue, ValueRecord secondValue) {
		this.key = key;
		this.firstValue = firstValue;
		this.secondValue = secondValue;
	}


	public KeyRecord getKey() {
		return key;
	}
	public ValueRecord getFirstValue() {
		return firstValue;
	}
	public ValueRecord getSecondValue() {
		return secondValue;
	}
	
	

}
