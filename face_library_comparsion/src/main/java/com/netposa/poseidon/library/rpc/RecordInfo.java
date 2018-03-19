/**
 * Autogenerated by Thrift Compiler (0.10.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.netposa.poseidon.library.rpc;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.10.0)", date = "2017-09-26")
public class RecordInfo implements org.apache.thrift.TBase<RecordInfo, RecordInfo._Fields>, java.io.Serializable, Cloneable, Comparable<RecordInfo> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("RecordInfo");

  private static final org.apache.thrift.protocol.TField ID_FIELD_DESC = new org.apache.thrift.protocol.TField("id", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField IMG_SCORE_FIELD_DESC = new org.apache.thrift.protocol.TField("imgScore", org.apache.thrift.protocol.TType.LIST, (short)2);
  private static final org.apache.thrift.protocol.TField EXT_FIELD_DESC = new org.apache.thrift.protocol.TField("ext", org.apache.thrift.protocol.TType.STRING, (short)3);
  private static final org.apache.thrift.protocol.TField HIGHEST_SCORE_FIELD_DESC = new org.apache.thrift.protocol.TField("highestScore", org.apache.thrift.protocol.TType.DOUBLE, (short)4);
  private static final org.apache.thrift.protocol.TField HIGHEST_SCORE_IMG_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("highestScoreImgId", org.apache.thrift.protocol.TType.STRING, (short)5);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new RecordInfoStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new RecordInfoTupleSchemeFactory();

  public java.lang.String id; // required
  public java.util.List<ImgScore> imgScore; // required
  public java.lang.String ext; // required
  public double highestScore; // required
  public java.lang.String highestScoreImgId; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    ID((short)1, "id"),
    IMG_SCORE((short)2, "imgScore"),
    EXT((short)3, "ext"),
    HIGHEST_SCORE((short)4, "highestScore"),
    HIGHEST_SCORE_IMG_ID((short)5, "highestScoreImgId");

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
        case 1: // ID
          return ID;
        case 2: // IMG_SCORE
          return IMG_SCORE;
        case 3: // EXT
          return EXT;
        case 4: // HIGHEST_SCORE
          return HIGHEST_SCORE;
        case 5: // HIGHEST_SCORE_IMG_ID
          return HIGHEST_SCORE_IMG_ID;
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
  private static final int __HIGHESTSCORE_ISSET_ID = 0;
  private byte __isset_bitfield = 0;
  public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.ID, new org.apache.thrift.meta_data.FieldMetaData("id", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.IMG_SCORE, new org.apache.thrift.meta_data.FieldMetaData("imgScore", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, ImgScore.class))));
    tmpMap.put(_Fields.EXT, new org.apache.thrift.meta_data.FieldMetaData("ext", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.HIGHEST_SCORE, new org.apache.thrift.meta_data.FieldMetaData("highestScore", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.DOUBLE)));
    tmpMap.put(_Fields.HIGHEST_SCORE_IMG_ID, new org.apache.thrift.meta_data.FieldMetaData("highestScoreImgId", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(RecordInfo.class, metaDataMap);
  }

  public RecordInfo() {
  }

  public RecordInfo(
    java.lang.String id,
    java.util.List<ImgScore> imgScore,
    java.lang.String ext,
    double highestScore,
    java.lang.String highestScoreImgId)
  {
    this();
    this.id = id;
    this.imgScore = imgScore;
    this.ext = ext;
    this.highestScore = highestScore;
    setHighestScoreIsSet(true);
    this.highestScoreImgId = highestScoreImgId;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public RecordInfo(RecordInfo other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetId()) {
      this.id = other.id;
    }
    if (other.isSetImgScore()) {
      java.util.List<ImgScore> __this__imgScore = new java.util.ArrayList<ImgScore>(other.imgScore.size());
      for (ImgScore other_element : other.imgScore) {
        __this__imgScore.add(new ImgScore(other_element));
      }
      this.imgScore = __this__imgScore;
    }
    if (other.isSetExt()) {
      this.ext = other.ext;
    }
    this.highestScore = other.highestScore;
    if (other.isSetHighestScoreImgId()) {
      this.highestScoreImgId = other.highestScoreImgId;
    }
  }

  public RecordInfo deepCopy() {
    return new RecordInfo(this);
  }

  @Override
  public void clear() {
    this.id = null;
    this.imgScore = null;
    this.ext = null;
    setHighestScoreIsSet(false);
    this.highestScore = 0.0;
    this.highestScoreImgId = null;
  }

  public java.lang.String getId() {
    return this.id;
  }

  public RecordInfo setId(java.lang.String id) {
    this.id = id;
    return this;
  }

  public void unsetId() {
    this.id = null;
  }

  /** Returns true if field id is set (has been assigned a value) and false otherwise */
  public boolean isSetId() {
    return this.id != null;
  }

  public void setIdIsSet(boolean value) {
    if (!value) {
      this.id = null;
    }
  }

  public int getImgScoreSize() {
    return (this.imgScore == null) ? 0 : this.imgScore.size();
  }

  public java.util.Iterator<ImgScore> getImgScoreIterator() {
    return (this.imgScore == null) ? null : this.imgScore.iterator();
  }

  public void addToImgScore(ImgScore elem) {
    if (this.imgScore == null) {
      this.imgScore = new java.util.ArrayList<ImgScore>();
    }
    this.imgScore.add(elem);
  }

  public java.util.List<ImgScore> getImgScore() {
    return this.imgScore;
  }

  public RecordInfo setImgScore(java.util.List<ImgScore> imgScore) {
    this.imgScore = imgScore;
    return this;
  }

  public void unsetImgScore() {
    this.imgScore = null;
  }

  /** Returns true if field imgScore is set (has been assigned a value) and false otherwise */
  public boolean isSetImgScore() {
    return this.imgScore != null;
  }

  public void setImgScoreIsSet(boolean value) {
    if (!value) {
      this.imgScore = null;
    }
  }

  public java.lang.String getExt() {
    return this.ext;
  }

  public RecordInfo setExt(java.lang.String ext) {
    this.ext = ext;
    return this;
  }

  public void unsetExt() {
    this.ext = null;
  }

  /** Returns true if field ext is set (has been assigned a value) and false otherwise */
  public boolean isSetExt() {
    return this.ext != null;
  }

  public void setExtIsSet(boolean value) {
    if (!value) {
      this.ext = null;
    }
  }

  public double getHighestScore() {
    return this.highestScore;
  }

  public RecordInfo setHighestScore(double highestScore) {
    this.highestScore = highestScore;
    setHighestScoreIsSet(true);
    return this;
  }

  public void unsetHighestScore() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __HIGHESTSCORE_ISSET_ID);
  }

  /** Returns true if field highestScore is set (has been assigned a value) and false otherwise */
  public boolean isSetHighestScore() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __HIGHESTSCORE_ISSET_ID);
  }

  public void setHighestScoreIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __HIGHESTSCORE_ISSET_ID, value);
  }

  public java.lang.String getHighestScoreImgId() {
    return this.highestScoreImgId;
  }

  public RecordInfo setHighestScoreImgId(java.lang.String highestScoreImgId) {
    this.highestScoreImgId = highestScoreImgId;
    return this;
  }

  public void unsetHighestScoreImgId() {
    this.highestScoreImgId = null;
  }

  /** Returns true if field highestScoreImgId is set (has been assigned a value) and false otherwise */
  public boolean isSetHighestScoreImgId() {
    return this.highestScoreImgId != null;
  }

  public void setHighestScoreImgIdIsSet(boolean value) {
    if (!value) {
      this.highestScoreImgId = null;
    }
  }

  public void setFieldValue(_Fields field, java.lang.Object value) {
    switch (field) {
    case ID:
      if (value == null) {
        unsetId();
      } else {
        setId((java.lang.String)value);
      }
      break;

    case IMG_SCORE:
      if (value == null) {
        unsetImgScore();
      } else {
        setImgScore((java.util.List<ImgScore>)value);
      }
      break;

    case EXT:
      if (value == null) {
        unsetExt();
      } else {
        setExt((java.lang.String)value);
      }
      break;

    case HIGHEST_SCORE:
      if (value == null) {
        unsetHighestScore();
      } else {
        setHighestScore((java.lang.Double)value);
      }
      break;

    case HIGHEST_SCORE_IMG_ID:
      if (value == null) {
        unsetHighestScoreImgId();
      } else {
        setHighestScoreImgId((java.lang.String)value);
      }
      break;

    }
  }

  public java.lang.Object getFieldValue(_Fields field) {
    switch (field) {
    case ID:
      return getId();

    case IMG_SCORE:
      return getImgScore();

    case EXT:
      return getExt();

    case HIGHEST_SCORE:
      return getHighestScore();

    case HIGHEST_SCORE_IMG_ID:
      return getHighestScoreImgId();

    }
    throw new java.lang.IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new java.lang.IllegalArgumentException();
    }

    switch (field) {
    case ID:
      return isSetId();
    case IMG_SCORE:
      return isSetImgScore();
    case EXT:
      return isSetExt();
    case HIGHEST_SCORE:
      return isSetHighestScore();
    case HIGHEST_SCORE_IMG_ID:
      return isSetHighestScoreImgId();
    }
    throw new java.lang.IllegalStateException();
  }

  @Override
  public boolean equals(java.lang.Object that) {
    if (that == null)
      return false;
    if (that instanceof RecordInfo)
      return this.equals((RecordInfo)that);
    return false;
  }

  public boolean equals(RecordInfo that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_id = true && this.isSetId();
    boolean that_present_id = true && that.isSetId();
    if (this_present_id || that_present_id) {
      if (!(this_present_id && that_present_id))
        return false;
      if (!this.id.equals(that.id))
        return false;
    }

    boolean this_present_imgScore = true && this.isSetImgScore();
    boolean that_present_imgScore = true && that.isSetImgScore();
    if (this_present_imgScore || that_present_imgScore) {
      if (!(this_present_imgScore && that_present_imgScore))
        return false;
      if (!this.imgScore.equals(that.imgScore))
        return false;
    }

    boolean this_present_ext = true && this.isSetExt();
    boolean that_present_ext = true && that.isSetExt();
    if (this_present_ext || that_present_ext) {
      if (!(this_present_ext && that_present_ext))
        return false;
      if (!this.ext.equals(that.ext))
        return false;
    }

    boolean this_present_highestScore = true;
    boolean that_present_highestScore = true;
    if (this_present_highestScore || that_present_highestScore) {
      if (!(this_present_highestScore && that_present_highestScore))
        return false;
      if (this.highestScore != that.highestScore)
        return false;
    }

    boolean this_present_highestScoreImgId = true && this.isSetHighestScoreImgId();
    boolean that_present_highestScoreImgId = true && that.isSetHighestScoreImgId();
    if (this_present_highestScoreImgId || that_present_highestScoreImgId) {
      if (!(this_present_highestScoreImgId && that_present_highestScoreImgId))
        return false;
      if (!this.highestScoreImgId.equals(that.highestScoreImgId))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + ((isSetId()) ? 131071 : 524287);
    if (isSetId())
      hashCode = hashCode * 8191 + id.hashCode();

    hashCode = hashCode * 8191 + ((isSetImgScore()) ? 131071 : 524287);
    if (isSetImgScore())
      hashCode = hashCode * 8191 + imgScore.hashCode();

    hashCode = hashCode * 8191 + ((isSetExt()) ? 131071 : 524287);
    if (isSetExt())
      hashCode = hashCode * 8191 + ext.hashCode();

    hashCode = hashCode * 8191 + org.apache.thrift.TBaseHelper.hashCode(highestScore);

    hashCode = hashCode * 8191 + ((isSetHighestScoreImgId()) ? 131071 : 524287);
    if (isSetHighestScoreImgId())
      hashCode = hashCode * 8191 + highestScoreImgId.hashCode();

    return hashCode;
  }

  @Override
  public int compareTo(RecordInfo other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;
    lastComparison = java.lang.Boolean.valueOf(isSetHighestScore()).compareTo(other.isSetHighestScore());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetHighestScore()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.highestScore,other.highestScore);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    
    lastComparison = java.lang.Boolean.valueOf(isSetId()).compareTo(other.isSetId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.id, other.id);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = java.lang.Boolean.valueOf(isSetImgScore()).compareTo(other.isSetImgScore());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetImgScore()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.imgScore, other.imgScore);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = java.lang.Boolean.valueOf(isSetExt()).compareTo(other.isSetExt());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetExt()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.ext, other.ext);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    
    lastComparison = java.lang.Boolean.valueOf(isSetHighestScoreImgId()).compareTo(other.isSetHighestScoreImgId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetHighestScoreImgId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.highestScoreImgId, other.highestScoreImgId);
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
    java.lang.StringBuilder sb = new java.lang.StringBuilder("RecordInfo(");
    boolean first = true;

    sb.append("id:");
    if (this.id == null) {
      sb.append("null");
    } else {
      sb.append(this.id);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("imgScore:");
    if (this.imgScore == null) {
      sb.append("null");
    } else {
      sb.append(this.imgScore);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("ext:");
    if (this.ext == null) {
      sb.append("null");
    } else {
      sb.append(this.ext);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("highestScore:");
    sb.append(this.highestScore);
    first = false;
    if (!first) sb.append(", ");
    sb.append("highestScoreImgId:");
    if (this.highestScoreImgId == null) {
      sb.append("null");
    } else {
      sb.append(this.highestScoreImgId);
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
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class RecordInfoStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public RecordInfoStandardScheme getScheme() {
      return new RecordInfoStandardScheme();
    }
  }

  private static class RecordInfoStandardScheme extends org.apache.thrift.scheme.StandardScheme<RecordInfo> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, RecordInfo struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // ID
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.id = iprot.readString();
              struct.setIdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // IMG_SCORE
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list24 = iprot.readListBegin();
                struct.imgScore = new java.util.ArrayList<ImgScore>(_list24.size);
                ImgScore _elem25;
                for (int _i26 = 0; _i26 < _list24.size; ++_i26)
                {
                  _elem25 = new ImgScore();
                  _elem25.read(iprot);
                  struct.imgScore.add(_elem25);
                }
                iprot.readListEnd();
              }
              struct.setImgScoreIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // EXT
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.ext = iprot.readString();
              struct.setExtIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // HIGHEST_SCORE
            if (schemeField.type == org.apache.thrift.protocol.TType.DOUBLE) {
              struct.highestScore = iprot.readDouble();
              struct.setHighestScoreIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 5: // HIGHEST_SCORE_IMG_ID
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.highestScoreImgId = iprot.readString();
              struct.setHighestScoreImgIdIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, RecordInfo struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.id != null) {
        oprot.writeFieldBegin(ID_FIELD_DESC);
        oprot.writeString(struct.id);
        oprot.writeFieldEnd();
      }
      if (struct.imgScore != null) {
        oprot.writeFieldBegin(IMG_SCORE_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.imgScore.size()));
          for (ImgScore _iter27 : struct.imgScore)
          {
            _iter27.write(oprot);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      if (struct.ext != null) {
        oprot.writeFieldBegin(EXT_FIELD_DESC);
        oprot.writeString(struct.ext);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldBegin(HIGHEST_SCORE_FIELD_DESC);
      oprot.writeDouble(struct.highestScore);
      oprot.writeFieldEnd();
      if (struct.highestScoreImgId != null) {
        oprot.writeFieldBegin(HIGHEST_SCORE_IMG_ID_FIELD_DESC);
        oprot.writeString(struct.highestScoreImgId);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class RecordInfoTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public RecordInfoTupleScheme getScheme() {
      return new RecordInfoTupleScheme();
    }
  }

  private static class RecordInfoTupleScheme extends org.apache.thrift.scheme.TupleScheme<RecordInfo> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, RecordInfo struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet optionals = new java.util.BitSet();
      if (struct.isSetId()) {
        optionals.set(0);
      }
      if (struct.isSetImgScore()) {
        optionals.set(1);
      }
      if (struct.isSetExt()) {
        optionals.set(2);
      }
      if (struct.isSetHighestScore()) {
        optionals.set(3);
      }
      if (struct.isSetHighestScoreImgId()) {
        optionals.set(4);
      }
      oprot.writeBitSet(optionals, 5);
      if (struct.isSetId()) {
        oprot.writeString(struct.id);
      }
      if (struct.isSetImgScore()) {
        {
          oprot.writeI32(struct.imgScore.size());
          for (ImgScore _iter28 : struct.imgScore)
          {
            _iter28.write(oprot);
          }
        }
      }
      if (struct.isSetExt()) {
        oprot.writeString(struct.ext);
      }
      if (struct.isSetHighestScore()) {
        oprot.writeDouble(struct.highestScore);
      }
      if (struct.isSetHighestScoreImgId()) {
        oprot.writeString(struct.highestScoreImgId);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, RecordInfo struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet incoming = iprot.readBitSet(5);
      if (incoming.get(0)) {
        struct.id = iprot.readString();
        struct.setIdIsSet(true);
      }
      if (incoming.get(1)) {
        {
          org.apache.thrift.protocol.TList _list29 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
          struct.imgScore = new java.util.ArrayList<ImgScore>(_list29.size);
          ImgScore _elem30;
          for (int _i31 = 0; _i31 < _list29.size; ++_i31)
          {
            _elem30 = new ImgScore();
            _elem30.read(iprot);
            struct.imgScore.add(_elem30);
          }
        }
        struct.setImgScoreIsSet(true);
      }
      if (incoming.get(2)) {
        struct.ext = iprot.readString();
        struct.setExtIsSet(true);
      }
      if (incoming.get(3)) {
        struct.highestScore = iprot.readDouble();
        struct.setHighestScoreIsSet(true);
      }
      if (incoming.get(4)) {
        struct.highestScoreImgId = iprot.readString();
        struct.setHighestScoreImgIdIsSet(true);
      }
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}

