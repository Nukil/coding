/**
 * Autogenerated by Thrift Compiler (0.10.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.netposa.poseidon.library.rpc.inrpc;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.10.0)", date = "2018-03-05")
public class ImgInfo implements org.apache.thrift.TBase<ImgInfo, ImgInfo._Fields>, java.io.Serializable, Cloneable, Comparable<ImgInfo> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("ImgInfo");

  private static final org.apache.thrift.protocol.TField IMG_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("imgId", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField FEATURE_FIELD_DESC = new org.apache.thrift.protocol.TField("feature", org.apache.thrift.protocol.TType.LIST, (short)2);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new ImgInfoStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new ImgInfoTupleSchemeFactory();

  public java.lang.String imgId; // required
  public java.util.List<java.lang.Double> feature; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    IMG_ID((short)1, "imgId"),
    FEATURE((short)2, "feature");

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
        case 1: // IMG_ID
          return IMG_ID;
        case 2: // FEATURE
          return FEATURE;
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
    tmpMap.put(_Fields.IMG_ID, new org.apache.thrift.meta_data.FieldMetaData("imgId", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.FEATURE, new org.apache.thrift.meta_data.FieldMetaData("feature", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.DOUBLE))));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(ImgInfo.class, metaDataMap);
  }

  public ImgInfo() {
  }

  public ImgInfo(
    java.lang.String imgId,
    java.util.List<java.lang.Double> feature)
  {
    this();
    this.imgId = imgId;
    this.feature = feature;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public ImgInfo(ImgInfo other) {
    if (other.isSetImgId()) {
      this.imgId = other.imgId;
    }
    if (other.isSetFeature()) {
      java.util.List<java.lang.Double> __this__feature = new java.util.ArrayList<java.lang.Double>(other.feature);
      this.feature = __this__feature;
    }
  }

  public ImgInfo deepCopy() {
    return new ImgInfo(this);
  }

  @Override
  public void clear() {
    this.imgId = null;
    this.feature = null;
  }

  public java.lang.String getImgId() {
    return this.imgId;
  }

  public ImgInfo setImgId(java.lang.String imgId) {
    this.imgId = imgId;
    return this;
  }

  public void unsetImgId() {
    this.imgId = null;
  }

  /** Returns true if field imgId is set (has been assigned a value) and false otherwise */
  public boolean isSetImgId() {
    return this.imgId != null;
  }

  public void setImgIdIsSet(boolean value) {
    if (!value) {
      this.imgId = null;
    }
  }

  public int getFeatureSize() {
    return (this.feature == null) ? 0 : this.feature.size();
  }

  public java.util.Iterator<java.lang.Double> getFeatureIterator() {
    return (this.feature == null) ? null : this.feature.iterator();
  }

  public void addToFeature(double elem) {
    if (this.feature == null) {
      this.feature = new java.util.ArrayList<java.lang.Double>();
    }
    this.feature.add(elem);
  }

  public java.util.List<java.lang.Double> getFeature() {
    return this.feature;
  }

  public ImgInfo setFeature(java.util.List<java.lang.Double> feature) {
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

  public void setFieldValue(_Fields field, java.lang.Object value) {
    switch (field) {
    case IMG_ID:
      if (value == null) {
        unsetImgId();
      } else {
        setImgId((java.lang.String)value);
      }
      break;

    case FEATURE:
      if (value == null) {
        unsetFeature();
      } else {
        setFeature((java.util.List<java.lang.Double>)value);
      }
      break;

    }
  }

  public java.lang.Object getFieldValue(_Fields field) {
    switch (field) {
    case IMG_ID:
      return getImgId();

    case FEATURE:
      return getFeature();

    }
    throw new java.lang.IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new java.lang.IllegalArgumentException();
    }

    switch (field) {
    case IMG_ID:
      return isSetImgId();
    case FEATURE:
      return isSetFeature();
    }
    throw new java.lang.IllegalStateException();
  }

  @Override
  public boolean equals(java.lang.Object that) {
    if (that == null)
      return false;
    if (that instanceof ImgInfo)
      return this.equals((ImgInfo)that);
    return false;
  }

  public boolean equals(ImgInfo that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_imgId = true && this.isSetImgId();
    boolean that_present_imgId = true && that.isSetImgId();
    if (this_present_imgId || that_present_imgId) {
      if (!(this_present_imgId && that_present_imgId))
        return false;
      if (!this.imgId.equals(that.imgId))
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

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + ((isSetImgId()) ? 131071 : 524287);
    if (isSetImgId())
      hashCode = hashCode * 8191 + imgId.hashCode();

    hashCode = hashCode * 8191 + ((isSetFeature()) ? 131071 : 524287);
    if (isSetFeature())
      hashCode = hashCode * 8191 + feature.hashCode();

    return hashCode;
  }

  @Override
  public int compareTo(ImgInfo other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = java.lang.Boolean.valueOf(isSetImgId()).compareTo(other.isSetImgId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetImgId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.imgId, other.imgId);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = java.lang.Boolean.valueOf(isSetFeature()).compareTo(other.isSetFeature());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetFeature()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.feature, other.feature);
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
    java.lang.StringBuilder sb = new java.lang.StringBuilder("ImgInfo(");
    boolean first = true;

    sb.append("imgId:");
    if (this.imgId == null) {
      sb.append("null");
    } else {
      sb.append(this.imgId);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("feature:");
    if (this.feature == null) {
      sb.append("null");
    } else {
      sb.append(this.feature);
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

  private static class ImgInfoStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public ImgInfoStandardScheme getScheme() {
      return new ImgInfoStandardScheme();
    }
  }

  private static class ImgInfoStandardScheme extends org.apache.thrift.scheme.StandardScheme<ImgInfo> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, ImgInfo struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // IMG_ID
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.imgId = iprot.readString();
              struct.setImgIdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // FEATURE
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list0 = iprot.readListBegin();
                struct.feature = new java.util.ArrayList<java.lang.Double>(_list0.size);
                double _elem1;
                for (int _i2 = 0; _i2 < _list0.size; ++_i2)
                {
                  _elem1 = iprot.readDouble();
                  struct.feature.add(_elem1);
                }
                iprot.readListEnd();
              }
              struct.setFeatureIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, ImgInfo struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.imgId != null) {
        oprot.writeFieldBegin(IMG_ID_FIELD_DESC);
        oprot.writeString(struct.imgId);
        oprot.writeFieldEnd();
      }
      if (struct.feature != null) {
        oprot.writeFieldBegin(FEATURE_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.DOUBLE, struct.feature.size()));
          for (double _iter3 : struct.feature)
          {
            oprot.writeDouble(_iter3);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class ImgInfoTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public ImgInfoTupleScheme getScheme() {
      return new ImgInfoTupleScheme();
    }
  }

  private static class ImgInfoTupleScheme extends org.apache.thrift.scheme.TupleScheme<ImgInfo> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, ImgInfo struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet optionals = new java.util.BitSet();
      if (struct.isSetImgId()) {
        optionals.set(0);
      }
      if (struct.isSetFeature()) {
        optionals.set(1);
      }
      oprot.writeBitSet(optionals, 2);
      if (struct.isSetImgId()) {
        oprot.writeString(struct.imgId);
      }
      if (struct.isSetFeature()) {
        {
          oprot.writeI32(struct.feature.size());
          for (double _iter4 : struct.feature)
          {
            oprot.writeDouble(_iter4);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, ImgInfo struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        struct.imgId = iprot.readString();
        struct.setImgIdIsSet(true);
      }
      if (incoming.get(1)) {
        {
          org.apache.thrift.protocol.TList _list5 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.DOUBLE, iprot.readI32());
          struct.feature = new java.util.ArrayList<java.lang.Double>(_list5.size);
          double _elem6;
          for (int _i7 = 0; _i7 < _list5.size; ++_i7)
          {
            _elem6 = iprot.readDouble();
            struct.feature.add(_elem6);
          }
        }
        struct.setFeatureIsSet(true);
      }
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}

