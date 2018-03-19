/**
 * Autogenerated by Thrift Compiler (0.10.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.netposa.poseidon.face.rpc;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.10.0)", date = "2017-09-13")
public class AccompanyAyalyzeResult implements org.apache.thrift.TBase<AccompanyAyalyzeResult, AccompanyAyalyzeResult._Fields>, java.io.Serializable, Cloneable, Comparable<AccompanyAyalyzeResult> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("AccompanyAyalyzeResult");

  private static final org.apache.thrift.protocol.TField LOG_NUMS_FIELD_DESC = new org.apache.thrift.protocol.TField("logNums", org.apache.thrift.protocol.TType.LIST, (short)1);
  private static final org.apache.thrift.protocol.TField ACCOMPANY_COUNT_FIELD_DESC = new org.apache.thrift.protocol.TField("accompanyCount", org.apache.thrift.protocol.TType.I32, (short)2);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new AccompanyAyalyzeResultStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new AccompanyAyalyzeResultTupleSchemeFactory();

  public java.util.List<String> logNums; // required
  public int accompanyCount; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    LOG_NUMS((short)1, "logNums"),
    ACCOMPANY_COUNT((short)2, "accompanyCount");

    private static final java.util.Map<String, _Fields> byName = new java.util.HashMap<String, _Fields>();

    static {
      for (_Fields field : java.util.EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // LOG_NUMS
          return LOG_NUMS;
        case 2: // ACCOMPANY_COUNT
          return ACCOMPANY_COUNT;
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
  private static final int __ACCOMPANYCOUNT_ISSET_ID = 0;
  private byte __isset_bitfield = 0;
  public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.LOG_NUMS, new org.apache.thrift.meta_data.FieldMetaData("logNums", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    tmpMap.put(_Fields.ACCOMPANY_COUNT, new org.apache.thrift.meta_data.FieldMetaData("accompanyCount", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(AccompanyAyalyzeResult.class, metaDataMap);
  }

  public AccompanyAyalyzeResult() {
  }

  public AccompanyAyalyzeResult(
    java.util.List<String> logNums,
    int accompanyCount)
  {
    this();
    this.logNums = logNums;
    this.accompanyCount = accompanyCount;
    setAccompanyCountIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public AccompanyAyalyzeResult(AccompanyAyalyzeResult other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetLogNums()) {
      java.util.List<String> __this__logNums = new java.util.ArrayList<String>(other.logNums);
      this.logNums = __this__logNums;
    }
    this.accompanyCount = other.accompanyCount;
  }

  public AccompanyAyalyzeResult deepCopy() {
    return new AccompanyAyalyzeResult(this);
  }

  @Override
  public void clear() {
    this.logNums = null;
    setAccompanyCountIsSet(false);
    this.accompanyCount = 0;
  }

  public int getLogNumsSize() {
    return (this.logNums == null) ? 0 : this.logNums.size();
  }

  public java.util.Iterator<String> getLogNumsIterator() {
    return (this.logNums == null) ? null : this.logNums.iterator();
  }

  public void addToLogNums(String elem) {
    if (this.logNums == null) {
      this.logNums = new java.util.ArrayList<String>();
    }
    this.logNums.add(elem);
  }

  public java.util.List<String> getLogNums() {
    return this.logNums;
  }

  public AccompanyAyalyzeResult setLogNums(java.util.List<String> logNums) {
    this.logNums = logNums;
    return this;
  }

  public void unsetLogNums() {
    this.logNums = null;
  }

  /** Returns true if field logNums is set (has been assigned a value) and false otherwise */
  public boolean isSetLogNums() {
    return this.logNums != null;
  }

  public void setLogNumsIsSet(boolean value) {
    if (!value) {
      this.logNums = null;
    }
  }

  public int getAccompanyCount() {
    return this.accompanyCount;
  }

  public AccompanyAyalyzeResult setAccompanyCount(int accompanyCount) {
    this.accompanyCount = accompanyCount;
    setAccompanyCountIsSet(true);
    return this;
  }

  public void unsetAccompanyCount() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __ACCOMPANYCOUNT_ISSET_ID);
  }

  /** Returns true if field accompanyCount is set (has been assigned a value) and false otherwise */
  public boolean isSetAccompanyCount() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __ACCOMPANYCOUNT_ISSET_ID);
  }

  public void setAccompanyCountIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __ACCOMPANYCOUNT_ISSET_ID, value);
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case LOG_NUMS:
      if (value == null) {
        unsetLogNums();
      } else {
        setLogNums((java.util.List<String>)value);
      }
      break;

    case ACCOMPANY_COUNT:
      if (value == null) {
        unsetAccompanyCount();
      } else {
        setAccompanyCount((Integer)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case LOG_NUMS:
      return getLogNums();

    case ACCOMPANY_COUNT:
      return getAccompanyCount();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case LOG_NUMS:
      return isSetLogNums();
    case ACCOMPANY_COUNT:
      return isSetAccompanyCount();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof AccompanyAyalyzeResult)
      return this.equals((AccompanyAyalyzeResult)that);
    return false;
  }

  public boolean equals(AccompanyAyalyzeResult that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_logNums = true && this.isSetLogNums();
    boolean that_present_logNums = true && that.isSetLogNums();
    if (this_present_logNums || that_present_logNums) {
      if (!(this_present_logNums && that_present_logNums))
        return false;
      if (!this.logNums.equals(that.logNums))
        return false;
    }

    boolean this_present_accompanyCount = true;
    boolean that_present_accompanyCount = true;
    if (this_present_accompanyCount || that_present_accompanyCount) {
      if (!(this_present_accompanyCount && that_present_accompanyCount))
        return false;
      if (this.accompanyCount != that.accompanyCount)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + ((isSetLogNums()) ? 131071 : 524287);
    if (isSetLogNums())
      hashCode = hashCode * 8191 + logNums.hashCode();

    hashCode = hashCode * 8191 + accompanyCount;

    return hashCode;
  }

  @Override
  public int compareTo(AccompanyAyalyzeResult other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetLogNums()).compareTo(other.isSetLogNums());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetLogNums()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.logNums, other.logNums);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetAccompanyCount()).compareTo(other.isSetAccompanyCount());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetAccompanyCount()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.accompanyCount, other.accompanyCount);
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
    scheme(iprot).read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    scheme(oprot).write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("AccompanyAyalyzeResult(");
    boolean first = true;

    sb.append("logNums:");
    if (this.logNums == null) {
      sb.append("null");
    } else {
      sb.append(this.logNums);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("accompanyCount:");
    sb.append(this.accompanyCount);
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

  private static class AccompanyAyalyzeResultStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public AccompanyAyalyzeResultStandardScheme getScheme() {
      return new AccompanyAyalyzeResultStandardScheme();
    }
  }

  private static class AccompanyAyalyzeResultStandardScheme extends org.apache.thrift.scheme.StandardScheme<AccompanyAyalyzeResult> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, AccompanyAyalyzeResult struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // LOG_NUMS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list32 = iprot.readListBegin();
                struct.logNums = new java.util.ArrayList<String>(_list32.size);
                String _elem33;
                for (int _i34 = 0; _i34 < _list32.size; ++_i34)
                {
                  _elem33 = iprot.readString();
                  struct.logNums.add(_elem33);
                }
                iprot.readListEnd();
              }
              struct.setLogNumsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // ACCOMPANY_COUNT
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.accompanyCount = iprot.readI32();
              struct.setAccompanyCountIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, AccompanyAyalyzeResult struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.logNums != null) {
        oprot.writeFieldBegin(LOG_NUMS_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.logNums.size()));
          for (String _iter35 : struct.logNums)
          {
            oprot.writeString(_iter35);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldBegin(ACCOMPANY_COUNT_FIELD_DESC);
      oprot.writeI32(struct.accompanyCount);
      oprot.writeFieldEnd();
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class AccompanyAyalyzeResultTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public AccompanyAyalyzeResultTupleScheme getScheme() {
      return new AccompanyAyalyzeResultTupleScheme();
    }
  }

  private static class AccompanyAyalyzeResultTupleScheme extends org.apache.thrift.scheme.TupleScheme<AccompanyAyalyzeResult> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, AccompanyAyalyzeResult struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet optionals = new java.util.BitSet();
      if (struct.isSetLogNums()) {
        optionals.set(0);
      }
      if (struct.isSetAccompanyCount()) {
        optionals.set(1);
      }
      oprot.writeBitSet(optionals, 2);
      if (struct.isSetLogNums()) {
        {
          oprot.writeI32(struct.logNums.size());
          for (String _iter36 : struct.logNums)
          {
            oprot.writeString(_iter36);
          }
        }
      }
      if (struct.isSetAccompanyCount()) {
        oprot.writeI32(struct.accompanyCount);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, AccompanyAyalyzeResult struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        {
          org.apache.thrift.protocol.TList _list37 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.logNums = new java.util.ArrayList<String>(_list37.size);
          String _elem38;
          for (int _i39 = 0; _i39 < _list37.size; ++_i39)
          {
            _elem38 = iprot.readString();
            struct.logNums.add(_elem38);
          }
        }
        struct.setLogNumsIsSet(true);
      }
      if (incoming.get(1)) {
        struct.accompanyCount = iprot.readI32();
        struct.setAccompanyCountIsSet(true);
      }
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}

