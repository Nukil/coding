/**
 * Autogenerated by Thrift Compiler (0.10.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.netposa.poseidon.library.rpc;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.10.0)", date = "2017-09-26")
public class FeatureVerifyRecord implements org.apache.thrift.TBase<FeatureVerifyRecord, FeatureVerifyRecord._Fields>, java.io.Serializable, Cloneable, Comparable<FeatureVerifyRecord> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("FeatureVerifyRecord");

  private static final org.apache.thrift.protocol.TField FEATURE1_FIELD_DESC = new org.apache.thrift.protocol.TField("feature1", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField FEATURE2_FIELD_DESC = new org.apache.thrift.protocol.TField("feature2", org.apache.thrift.protocol.TType.STRING, (short)2);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new FeatureVerifyRecordStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new FeatureVerifyRecordTupleSchemeFactory();

  public java.nio.ByteBuffer feature1; // required
  public java.nio.ByteBuffer feature2; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    FEATURE1((short)1, "feature1"),
    FEATURE2((short)2, "feature2");

    private static final java.util.Map<java.lang.String, _Fields> byName = new java.util.HashMap<java.lang.String, _Fields>();

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
        case 1: // FEATURE1
          return FEATURE1;
        case 2: // FEATURE2
          return FEATURE2;
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
      if (fields == null) throw new java.lang.IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(java.lang.String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final java.lang.String _fieldName;

    _Fields(short thriftId, java.lang.String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public java.lang.String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.FEATURE1, new org.apache.thrift.meta_data.FieldMetaData("feature1", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    tmpMap.put(_Fields.FEATURE2, new org.apache.thrift.meta_data.FieldMetaData("feature2", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(FeatureVerifyRecord.class, metaDataMap);
  }

  public FeatureVerifyRecord() {
  }

  public FeatureVerifyRecord(
    java.nio.ByteBuffer feature1,
    java.nio.ByteBuffer feature2)
  {
    this();
    this.feature1 = org.apache.thrift.TBaseHelper.copyBinary(feature1);
    this.feature2 = org.apache.thrift.TBaseHelper.copyBinary(feature2);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public FeatureVerifyRecord(FeatureVerifyRecord other) {
    if (other.isSetFeature1()) {
      this.feature1 = org.apache.thrift.TBaseHelper.copyBinary(other.feature1);
    }
    if (other.isSetFeature2()) {
      this.feature2 = org.apache.thrift.TBaseHelper.copyBinary(other.feature2);
    }
  }

  public FeatureVerifyRecord deepCopy() {
    return new FeatureVerifyRecord(this);
  }

  @Override
  public void clear() {
    this.feature1 = null;
    this.feature2 = null;
  }

  public byte[] getFeature1() {
    setFeature1(org.apache.thrift.TBaseHelper.rightSize(feature1));
    return feature1 == null ? null : feature1.array();
  }

  public java.nio.ByteBuffer bufferForFeature1() {
    return org.apache.thrift.TBaseHelper.copyBinary(feature1);
  }

  public FeatureVerifyRecord setFeature1(byte[] feature1) {
    this.feature1 = feature1 == null ? (java.nio.ByteBuffer)null : java.nio.ByteBuffer.wrap(feature1.clone());
    return this;
  }

  public FeatureVerifyRecord setFeature1(java.nio.ByteBuffer feature1) {
    this.feature1 = org.apache.thrift.TBaseHelper.copyBinary(feature1);
    return this;
  }

  public void unsetFeature1() {
    this.feature1 = null;
  }

  /** Returns true if field feature1 is set (has been assigned a value) and false otherwise */
  public boolean isSetFeature1() {
    return this.feature1 != null;
  }

  public void setFeature1IsSet(boolean value) {
    if (!value) {
      this.feature1 = null;
    }
  }

  public byte[] getFeature2() {
    setFeature2(org.apache.thrift.TBaseHelper.rightSize(feature2));
    return feature2 == null ? null : feature2.array();
  }

  public java.nio.ByteBuffer bufferForFeature2() {
    return org.apache.thrift.TBaseHelper.copyBinary(feature2);
  }

  public FeatureVerifyRecord setFeature2(byte[] feature2) {
    this.feature2 = feature2 == null ? (java.nio.ByteBuffer)null : java.nio.ByteBuffer.wrap(feature2.clone());
    return this;
  }

  public FeatureVerifyRecord setFeature2(java.nio.ByteBuffer feature2) {
    this.feature2 = org.apache.thrift.TBaseHelper.copyBinary(feature2);
    return this;
  }

  public void unsetFeature2() {
    this.feature2 = null;
  }

  /** Returns true if field feature2 is set (has been assigned a value) and false otherwise */
  public boolean isSetFeature2() {
    return this.feature2 != null;
  }

  public void setFeature2IsSet(boolean value) {
    if (!value) {
      this.feature2 = null;
    }
  }

  public void setFieldValue(_Fields field, java.lang.Object value) {
    switch (field) {
    case FEATURE1:
      if (value == null) {
        unsetFeature1();
      } else {
        if (value instanceof byte[]) {
          setFeature1((byte[])value);
        } else {
          setFeature1((java.nio.ByteBuffer)value);
        }
      }
      break;

    case FEATURE2:
      if (value == null) {
        unsetFeature2();
      } else {
        if (value instanceof byte[]) {
          setFeature2((byte[])value);
        } else {
          setFeature2((java.nio.ByteBuffer)value);
        }
      }
      break;

    }
  }

  public java.lang.Object getFieldValue(_Fields field) {
    switch (field) {
    case FEATURE1:
      return getFeature1();

    case FEATURE2:
      return getFeature2();

    }
    throw new java.lang.IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new java.lang.IllegalArgumentException();
    }

    switch (field) {
    case FEATURE1:
      return isSetFeature1();
    case FEATURE2:
      return isSetFeature2();
    }
    throw new java.lang.IllegalStateException();
  }

  @Override
  public boolean equals(java.lang.Object that) {
    if (that == null)
      return false;
    if (that instanceof FeatureVerifyRecord)
      return this.equals((FeatureVerifyRecord)that);
    return false;
  }

  public boolean equals(FeatureVerifyRecord that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_feature1 = true && this.isSetFeature1();
    boolean that_present_feature1 = true && that.isSetFeature1();
    if (this_present_feature1 || that_present_feature1) {
      if (!(this_present_feature1 && that_present_feature1))
        return false;
      if (!this.feature1.equals(that.feature1))
        return false;
    }

    boolean this_present_feature2 = true && this.isSetFeature2();
    boolean that_present_feature2 = true && that.isSetFeature2();
    if (this_present_feature2 || that_present_feature2) {
      if (!(this_present_feature2 && that_present_feature2))
        return false;
      if (!this.feature2.equals(that.feature2))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + ((isSetFeature1()) ? 131071 : 524287);
    if (isSetFeature1())
      hashCode = hashCode * 8191 + feature1.hashCode();

    hashCode = hashCode * 8191 + ((isSetFeature2()) ? 131071 : 524287);
    if (isSetFeature2())
      hashCode = hashCode * 8191 + feature2.hashCode();

    return hashCode;
  }

  @Override
  public int compareTo(FeatureVerifyRecord other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = java.lang.Boolean.valueOf(isSetFeature1()).compareTo(other.isSetFeature1());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetFeature1()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.feature1, other.feature1);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = java.lang.Boolean.valueOf(isSetFeature2()).compareTo(other.isSetFeature2());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetFeature2()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.feature2, other.feature2);
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
  public java.lang.String toString() {
    java.lang.StringBuilder sb = new java.lang.StringBuilder("FeatureVerifyRecord(");
    boolean first = true;

    sb.append("feature1:");
    if (this.feature1 == null) {
      sb.append("null");
    } else {
      org.apache.thrift.TBaseHelper.toString(this.feature1, sb);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("feature2:");
    if (this.feature2 == null) {
      sb.append("null");
    } else {
      org.apache.thrift.TBaseHelper.toString(this.feature2, sb);
    }
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

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
    try {
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class FeatureVerifyRecordStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public FeatureVerifyRecordStandardScheme getScheme() {
      return new FeatureVerifyRecordStandardScheme();
    }
  }

  private static class FeatureVerifyRecordStandardScheme extends org.apache.thrift.scheme.StandardScheme<FeatureVerifyRecord> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, FeatureVerifyRecord struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // FEATURE1
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.feature1 = iprot.readBinary();
              struct.setFeature1IsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // FEATURE2
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.feature2 = iprot.readBinary();
              struct.setFeature2IsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, FeatureVerifyRecord struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.feature1 != null) {
        oprot.writeFieldBegin(FEATURE1_FIELD_DESC);
        oprot.writeBinary(struct.feature1);
        oprot.writeFieldEnd();
      }
      if (struct.feature2 != null) {
        oprot.writeFieldBegin(FEATURE2_FIELD_DESC);
        oprot.writeBinary(struct.feature2);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class FeatureVerifyRecordTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public FeatureVerifyRecordTupleScheme getScheme() {
      return new FeatureVerifyRecordTupleScheme();
    }
  }

  private static class FeatureVerifyRecordTupleScheme extends org.apache.thrift.scheme.TupleScheme<FeatureVerifyRecord> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, FeatureVerifyRecord struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet optionals = new java.util.BitSet();
      if (struct.isSetFeature1()) {
        optionals.set(0);
      }
      if (struct.isSetFeature2()) {
        optionals.set(1);
      }
      oprot.writeBitSet(optionals, 2);
      if (struct.isSetFeature1()) {
        oprot.writeBinary(struct.feature1);
      }
      if (struct.isSetFeature2()) {
        oprot.writeBinary(struct.feature2);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, FeatureVerifyRecord struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        struct.feature1 = iprot.readBinary();
        struct.setFeature1IsSet(true);
      }
      if (incoming.get(1)) {
        struct.feature2 = iprot.readBinary();
        struct.setFeature2IsSet(true);
      }
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}

