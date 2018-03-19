/**
 * Autogenerated by Thrift Compiler (0.9.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package netposa.fakecar.feature.rpc.search;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.thrift.EncodingUtils;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;
import org.apache.thrift.scheme.TupleScheme;

@SuppressWarnings({"serial","rawtypes","unchecked"})
public class InputRecord implements org.apache.thrift.TBase<InputRecord, InputRecord._Fields>, java.io.Serializable, Cloneable {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("InputRecord");

  private static final org.apache.thrift.protocol.TField START_TIME_FIELD_DESC = new org.apache.thrift.protocol.TField("startTime", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField END_TIME_FIELD_DESC = new org.apache.thrift.protocol.TField("endTime", org.apache.thrift.protocol.TType.STRING, (short)2);
  private static final org.apache.thrift.protocol.TField CLPP_FIELD_DESC = new org.apache.thrift.protocol.TField("clpp", org.apache.thrift.protocol.TType.STRING, (short)3);
  private static final org.apache.thrift.protocol.TField COUNT_FIELD_DESC = new org.apache.thrift.protocol.TField("count", org.apache.thrift.protocol.TType.I32, (short)4);
  private static final org.apache.thrift.protocol.TField FEATURE_FIELD_DESC = new org.apache.thrift.protocol.TField("feature", org.apache.thrift.protocol.TType.STRING, (short)5);
  private static final org.apache.thrift.protocol.TField DISTENCE_FIELD_DESC = new org.apache.thrift.protocol.TField("distence", org.apache.thrift.protocol.TType.I32, (short)6);
  private static final org.apache.thrift.protocol.TField CSYS_FIELD_DESC = new org.apache.thrift.protocol.TField("csys", org.apache.thrift.protocol.TType.I32, (short)7);

private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new InputRecordStandardSchemeFactory());
    schemes.put(TupleScheme.class, new InputRecordTupleSchemeFactory());
  }

  public String startTime; // required
  public String endTime; // required
  public String clpp; // required
  public int count; // required
  public ByteBuffer feature; // required
  public int distence; // required
  public int csys; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    START_TIME((short)1, "startTime"),
    END_TIME((short)2, "endTime"),
    CLPP((short)3, "clpp"),
    COUNT((short)4, "count"),
    FEATURE((short)5, "feature"),
    DISTENCE((short)6, "distence"),
    CSYS((short)7, "csys");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // START_TIME
          return START_TIME;
        case 2: // END_TIME
          return END_TIME;
        case 3: // CLPP
          return CLPP;
        case 4: // COUNT
          return COUNT;
        case 5: // FEATURE
          return FEATURE;
        case 6: // DISTENCE
          return DISTENCE;
        case 7: // CSYS
          return CSYS;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __COUNT_ISSET_ID = 0;
  private static final int __DISTENCE_ISSET_ID = 1;
  private static final int __CSYS_ISSET_ID = 2;
  private byte __isset_bitfield = 0;
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.START_TIME, new org.apache.thrift.meta_data.FieldMetaData("startTime", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.END_TIME, new org.apache.thrift.meta_data.FieldMetaData("endTime", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.CLPP, new org.apache.thrift.meta_data.FieldMetaData("clpp", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.COUNT, new org.apache.thrift.meta_data.FieldMetaData("count", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.FEATURE, new org.apache.thrift.meta_data.FieldMetaData("feature", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    tmpMap.put(_Fields.DISTENCE, new org.apache.thrift.meta_data.FieldMetaData("distence", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.CSYS, new org.apache.thrift.meta_data.FieldMetaData("csys", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(InputRecord.class, metaDataMap);
  }

  public InputRecord() {
  }

  public InputRecord(
    String startTime,
    String endTime,
    String clpp,
    int count,
    ByteBuffer feature,
    int distence,
    int csys)
  {
    this();
    this.startTime = startTime;
    this.endTime = endTime;
    this.clpp = clpp;
    this.count = count;
    setCountIsSet(true);
    this.feature = feature;
    this.distence = distence;
    setDistenceIsSet(true);
    this.csys = csys;
    setCsysIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public InputRecord(InputRecord other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetStartTime()) {
      this.startTime = other.startTime;
    }
    if (other.isSetEndTime()) {
      this.endTime = other.endTime;
    }
    if (other.isSetClpp()) {
      this.clpp = other.clpp;
    }
    this.count = other.count;
    if (other.isSetFeature()) {
      this.feature = org.apache.thrift.TBaseHelper.copyBinary(other.feature);
;
    }
    this.distence = other.distence;
    this.csys = other.csys;
  }

  public InputRecord deepCopy() {
    return new InputRecord(this);
  }

  @Override
  public void clear() {
    this.startTime = null;
    this.endTime = null;
    this.clpp = null;
    setCountIsSet(false);
    this.count = 0;
    this.feature = null;
    setDistenceIsSet(false);
    this.distence = 0;
    setCsysIsSet(false);
    this.csys = 0;
  }

  public String getStartTime() {
    return this.startTime;
  }

  public InputRecord setStartTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  public void unsetStartTime() {
    this.startTime = null;
  }

  /** Returns true if field startTime is set (has been assigned a value) and false otherwise */
  public boolean isSetStartTime() {
    return this.startTime != null;
  }

  public void setStartTimeIsSet(boolean value) {
    if (!value) {
      this.startTime = null;
    }
  }

  public String getEndTime() {
    return this.endTime;
  }

  public InputRecord setEndTime(String endTime) {
    this.endTime = endTime;
    return this;
  }

  public void unsetEndTime() {
    this.endTime = null;
  }

  /** Returns true if field endTime is set (has been assigned a value) and false otherwise */
  public boolean isSetEndTime() {
    return this.endTime != null;
  }

  public void setEndTimeIsSet(boolean value) {
    if (!value) {
      this.endTime = null;
    }
  }

  public String getClpp() {
    return this.clpp;
  }

  public InputRecord setClpp(String clpp) {
    this.clpp = clpp;
    return this;
  }

  public void unsetClpp() {
    this.clpp = null;
  }

  /** Returns true if field clpp is set (has been assigned a value) and false otherwise */
  public boolean isSetClpp() {
    return this.clpp != null;
  }

  public void setClppIsSet(boolean value) {
    if (!value) {
      this.clpp = null;
    }
  }

  public int getCount() {
    return this.count;
  }

  public InputRecord setCount(int count) {
    this.count = count;
    setCountIsSet(true);
    return this;
  }

  public void unsetCount() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __COUNT_ISSET_ID);
  }

  /** Returns true if field count is set (has been assigned a value) and false otherwise */
  public boolean isSetCount() {
    return EncodingUtils.testBit(__isset_bitfield, __COUNT_ISSET_ID);
  }

  public void setCountIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __COUNT_ISSET_ID, value);
  }

  public byte[] getFeature() {
    setFeature(org.apache.thrift.TBaseHelper.rightSize(feature));
    return feature == null ? null : feature.array();
  }

  public ByteBuffer bufferForFeature() {
    return feature;
  }

  public InputRecord setFeature(byte[] feature) {
    setFeature(feature == null ? (ByteBuffer)null : ByteBuffer.wrap(feature));
    return this;
  }

  public InputRecord setFeature(ByteBuffer feature) {
    this.feature = feature;
    return this;
  }

  public void unsetFeature() {
    this.feature = null;
  }

  /** Returns true if field feature is set (has been assigned a value) and false otherwise */
  public boolean isSetFeature() {
    return this.feature != null;
  }

  public void setFeatureIsSet(boolean value) {
    if (!value) {
      this.feature = null;
    }
  }

  public int getDistence() {
    return this.distence;
  }

  public InputRecord setDistence(int distence) {
    this.distence = distence;
    setDistenceIsSet(true);
    return this;
  }

  public void unsetDistence() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __DISTENCE_ISSET_ID);
  }

  /** Returns true if field distence is set (has been assigned a value) and false otherwise */
  public boolean isSetDistence() {
    return EncodingUtils.testBit(__isset_bitfield, __DISTENCE_ISSET_ID);
  }

  public void setDistenceIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __DISTENCE_ISSET_ID, value);
  }

  public int getCsys() {
    return this.csys;
  }

  public InputRecord setCsys(int csys) {
    this.csys = csys;
    setCsysIsSet(true);
    return this;
  }

  public void unsetCsys() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __CSYS_ISSET_ID);
  }

  /** Returns true if field csys is set (has been assigned a value) and false otherwise */
  public boolean isSetCsys() {
    return EncodingUtils.testBit(__isset_bitfield, __CSYS_ISSET_ID);
  }

  public void setCsysIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __CSYS_ISSET_ID, value);
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case START_TIME:
      if (value == null) {
        unsetStartTime();
      } else {
        setStartTime((String)value);
      }
      break;

    case END_TIME:
      if (value == null) {
        unsetEndTime();
      } else {
        setEndTime((String)value);
      }
      break;

    case CLPP:
      if (value == null) {
        unsetClpp();
      } else {
        setClpp((String)value);
      }
      break;

    case COUNT:
      if (value == null) {
        unsetCount();
      } else {
        setCount((Integer)value);
      }
      break;

    case FEATURE:
      if (value == null) {
        unsetFeature();
      } else {
        setFeature((ByteBuffer)value);
      }
      break;

    case DISTENCE:
      if (value == null) {
        unsetDistence();
      } else {
        setDistence((Integer)value);
      }
      break;

    case CSYS:
      if (value == null) {
        unsetCsys();
      } else {
        setCsys((Integer)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case START_TIME:
      return getStartTime();

    case END_TIME:
      return getEndTime();

    case CLPP:
      return getClpp();

    case COUNT:
      return Integer.valueOf(getCount());

    case FEATURE:
      return getFeature();

    case DISTENCE:
      return Integer.valueOf(getDistence());

    case CSYS:
      return Integer.valueOf(getCsys());

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case START_TIME:
      return isSetStartTime();
    case END_TIME:
      return isSetEndTime();
    case CLPP:
      return isSetClpp();
    case COUNT:
      return isSetCount();
    case FEATURE:
      return isSetFeature();
    case DISTENCE:
      return isSetDistence();
    case CSYS:
      return isSetCsys();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof InputRecord)
      return this.equals((InputRecord)that);
    return false;
  }

  public boolean equals(InputRecord that) {
    if (that == null)
      return false;

    boolean this_present_startTime = true && this.isSetStartTime();
    boolean that_present_startTime = true && that.isSetStartTime();
    if (this_present_startTime || that_present_startTime) {
      if (!(this_present_startTime && that_present_startTime))
        return false;
      if (!this.startTime.equals(that.startTime))
        return false;
    }

    boolean this_present_endTime = true && this.isSetEndTime();
    boolean that_present_endTime = true && that.isSetEndTime();
    if (this_present_endTime || that_present_endTime) {
      if (!(this_present_endTime && that_present_endTime))
        return false;
      if (!this.endTime.equals(that.endTime))
        return false;
    }

    boolean this_present_clpp = true && this.isSetClpp();
    boolean that_present_clpp = true && that.isSetClpp();
    if (this_present_clpp || that_present_clpp) {
      if (!(this_present_clpp && that_present_clpp))
        return false;
      if (!this.clpp.equals(that.clpp))
        return false;
    }

    boolean this_present_count = true;
    boolean that_present_count = true;
    if (this_present_count || that_present_count) {
      if (!(this_present_count && that_present_count))
        return false;
      if (this.count != that.count)
        return false;
    }

    boolean this_present_feature = true && this.isSetFeature();
    boolean that_present_feature = true && that.isSetFeature();
    if (this_present_feature || that_present_feature) {
      if (!(this_present_feature && that_present_feature))
        return false;
      if (!this.feature.equals(that.feature))
        return false;
    }

    boolean this_present_distence = true;
    boolean that_present_distence = true;
    if (this_present_distence || that_present_distence) {
      if (!(this_present_distence && that_present_distence))
        return false;
      if (this.distence != that.distence)
        return false;
    }

    boolean this_present_csys = true;
    boolean that_present_csys = true;
    if (this_present_csys || that_present_csys) {
      if (!(this_present_csys && that_present_csys))
        return false;
      if (this.csys != that.csys)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  public int compareTo(InputRecord other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;
    InputRecord typedOther = (InputRecord)other;

    lastComparison = Boolean.valueOf(isSetStartTime()).compareTo(typedOther.isSetStartTime());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetStartTime()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.startTime, typedOther.startTime);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetEndTime()).compareTo(typedOther.isSetEndTime());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetEndTime()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.endTime, typedOther.endTime);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetClpp()).compareTo(typedOther.isSetClpp());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetClpp()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.clpp, typedOther.clpp);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetCount()).compareTo(typedOther.isSetCount());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetCount()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.count, typedOther.count);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetFeature()).compareTo(typedOther.isSetFeature());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetFeature()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.feature, typedOther.feature);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetDistence()).compareTo(typedOther.isSetDistence());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetDistence()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.distence, typedOther.distence);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetCsys()).compareTo(typedOther.isSetCsys());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetCsys()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.csys, typedOther.csys);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("InputRecord(");
    boolean first = true;

    sb.append("startTime:");
    if (this.startTime == null) {
      sb.append("null");
    } else {
      sb.append(this.startTime);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("endTime:");
    if (this.endTime == null) {
      sb.append("null");
    } else {
      sb.append(this.endTime);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("clpp:");
    if (this.clpp == null) {
      sb.append("null");
    } else {
      sb.append(this.clpp);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("count:");
    sb.append(this.count);
    first = false;
    if (!first) sb.append(", ");
    sb.append("feature:");
    if (this.feature == null) {
      sb.append("null");
    } else {
      org.apache.thrift.TBaseHelper.toString(this.feature, sb);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("distence:");
    sb.append(this.distence);
    first = false;
    if (!first) sb.append(", ");
    sb.append("csys:");
    sb.append(this.csys);
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class InputRecordStandardSchemeFactory implements SchemeFactory {
    public InputRecordStandardScheme getScheme() {
      return new InputRecordStandardScheme();
    }
  }

  private static class InputRecordStandardScheme extends StandardScheme<InputRecord> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, InputRecord struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // START_TIME
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.startTime = iprot.readString();
              struct.setStartTimeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // END_TIME
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.endTime = iprot.readString();
              struct.setEndTimeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // CLPP
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.clpp = iprot.readString();
              struct.setClppIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // COUNT
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.count = iprot.readI32();
              struct.setCountIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 5: // FEATURE
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.feature = iprot.readBinary();
              struct.setFeatureIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 6: // DISTENCE
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.distence = iprot.readI32();
              struct.setDistenceIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 7: // CSYS
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.csys = iprot.readI32();
              struct.setCsysIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, InputRecord struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.startTime != null) {
        oprot.writeFieldBegin(START_TIME_FIELD_DESC);
        oprot.writeString(struct.startTime);
        oprot.writeFieldEnd();
      }
      if (struct.endTime != null) {
        oprot.writeFieldBegin(END_TIME_FIELD_DESC);
        oprot.writeString(struct.endTime);
        oprot.writeFieldEnd();
      }
      if (struct.clpp != null) {
        oprot.writeFieldBegin(CLPP_FIELD_DESC);
        oprot.writeString(struct.clpp);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldBegin(COUNT_FIELD_DESC);
      oprot.writeI32(struct.count);
      oprot.writeFieldEnd();
      if (struct.feature != null) {
        oprot.writeFieldBegin(FEATURE_FIELD_DESC);
        oprot.writeBinary(struct.feature);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldBegin(DISTENCE_FIELD_DESC);
      oprot.writeI32(struct.distence);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(CSYS_FIELD_DESC);
      oprot.writeI32(struct.csys);
      oprot.writeFieldEnd();
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class InputRecordTupleSchemeFactory implements SchemeFactory {
    public InputRecordTupleScheme getScheme() {
      return new InputRecordTupleScheme();
    }
  }

  private static class InputRecordTupleScheme extends TupleScheme<InputRecord> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, InputRecord struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetStartTime()) {
        optionals.set(0);
      }
      if (struct.isSetEndTime()) {
        optionals.set(1);
      }
      if (struct.isSetClpp()) {
        optionals.set(2);
      }
      if (struct.isSetCount()) {
        optionals.set(3);
      }
      if (struct.isSetFeature()) {
        optionals.set(4);
      }
      if (struct.isSetDistence()) {
        optionals.set(5);
      }
      if (struct.isSetCsys()) {
        optionals.set(6);
      }
      oprot.writeBitSet(optionals, 7);
      if (struct.isSetStartTime()) {
        oprot.writeString(struct.startTime);
      }
      if (struct.isSetEndTime()) {
        oprot.writeString(struct.endTime);
      }
      if (struct.isSetClpp()) {
        oprot.writeString(struct.clpp);
      }
      if (struct.isSetCount()) {
        oprot.writeI32(struct.count);
      }
      if (struct.isSetFeature()) {
        oprot.writeBinary(struct.feature);
      }
      if (struct.isSetDistence()) {
        oprot.writeI32(struct.distence);
      }
      if (struct.isSetCsys()) {
        oprot.writeI32(struct.csys);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, InputRecord struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(7);
      if (incoming.get(0)) {
        struct.startTime = iprot.readString();
        struct.setStartTimeIsSet(true);
      }
      if (incoming.get(1)) {
        struct.endTime = iprot.readString();
        struct.setEndTimeIsSet(true);
      }
      if (incoming.get(2)) {
        struct.clpp = iprot.readString();
        struct.setClppIsSet(true);
      }
      if (incoming.get(3)) {
        struct.count = iprot.readI32();
        struct.setCountIsSet(true);
      }
      if (incoming.get(4)) {
        struct.feature = iprot.readBinary();
        struct.setFeatureIsSet(true);
      }
      if (incoming.get(5)) {
        struct.distence = iprot.readI32();
        struct.setDistenceIsSet(true);
      }
      if (incoming.get(6)) {
        struct.csys = iprot.readI32();
        struct.setCsysIsSet(true);
      }
    }
  }

}
