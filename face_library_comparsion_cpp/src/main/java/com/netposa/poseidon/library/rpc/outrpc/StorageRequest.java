/**
 * Autogenerated by Thrift Compiler (0.10.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.netposa.poseidon.library.rpc.outrpc;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.10.0)", date = "2018-03-05")
public class StorageRequest implements org.apache.thrift.TBase<StorageRequest, StorageRequest._Fields>, java.io.Serializable, Cloneable, Comparable<StorageRequest> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("StorageRequest");

  private static final org.apache.thrift.protocol.TField STORAGE_INFOS_FIELD_DESC = new org.apache.thrift.protocol.TField("storageInfos", org.apache.thrift.protocol.TType.LIST, (short)1);
  private static final org.apache.thrift.protocol.TField TYPE_FIELD_DESC = new org.apache.thrift.protocol.TField("type", org.apache.thrift.protocol.TType.I32, (short)2);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new StorageRequestStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new StorageRequestTupleSchemeFactory();

  public java.util.List<StorageInfo> storageInfos; // required
  /**
   * 
   * @see OperationType
   */
  public OperationType type; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    STORAGE_INFOS((short)1, "storageInfos"),
    /**
     * 
     * @see OperationType
     */
    TYPE((short)2, "type");

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
        case 1: // STORAGE_INFOS
          return STORAGE_INFOS;
        case 2: // TYPE
          return TYPE;
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
  public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.STORAGE_INFOS, new org.apache.thrift.meta_data.FieldMetaData("storageInfos", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, StorageInfo.class))));
    tmpMap.put(_Fields.TYPE, new org.apache.thrift.meta_data.FieldMetaData("type", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.EnumMetaData(org.apache.thrift.protocol.TType.ENUM, OperationType.class)));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(StorageRequest.class, metaDataMap);
  }

  public StorageRequest() {
  }

  public StorageRequest(
    java.util.List<StorageInfo> storageInfos,
    OperationType type)
  {
    this();
    this.storageInfos = storageInfos;
    this.type = type;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public StorageRequest(StorageRequest other) {
    if (other.isSetStorageInfos()) {
      java.util.List<StorageInfo> __this__storageInfos = new java.util.ArrayList<StorageInfo>(other.storageInfos.size());
      for (StorageInfo other_element : other.storageInfos) {
        __this__storageInfos.add(new StorageInfo(other_element));
      }
      this.storageInfos = __this__storageInfos;
    }
    if (other.isSetType()) {
      this.type = other.type;
    }
  }

  public StorageRequest deepCopy() {
    return new StorageRequest(this);
  }

  @Override
  public void clear() {
    this.storageInfos = null;
    this.type = null;
  }

  public int getStorageInfosSize() {
    return (this.storageInfos == null) ? 0 : this.storageInfos.size();
  }

  public java.util.Iterator<StorageInfo> getStorageInfosIterator() {
    return (this.storageInfos == null) ? null : this.storageInfos.iterator();
  }

  public void addToStorageInfos(StorageInfo elem) {
    if (this.storageInfos == null) {
      this.storageInfos = new java.util.ArrayList<StorageInfo>();
    }
    this.storageInfos.add(elem);
  }

  public java.util.List<StorageInfo> getStorageInfos() {
    return this.storageInfos;
  }

  public StorageRequest setStorageInfos(java.util.List<StorageInfo> storageInfos) {
    this.storageInfos = storageInfos;
    return this;
  }

  public void unsetStorageInfos() {
    this.storageInfos = null;
  }

  /** Returns true if field storageInfos is set (has been assigned a value) and false otherwise */
  public boolean isSetStorageInfos() {
    return this.storageInfos != null;
  }

  public void setStorageInfosIsSet(boolean value) {
    if (!value) {
      this.storageInfos = null;
    }
  }

  /**
   * 
   * @see OperationType
   */
  public OperationType getType() {
    return this.type;
  }

  /**
   * 
   * @see OperationType
   */
  public StorageRequest setType(OperationType type) {
    this.type = type;
    return this;
  }

  public void unsetType() {
    this.type = null;
  }

  /** Returns true if field type is set (has been assigned a value) and false otherwise */
  public boolean isSetType() {
    return this.type != null;
  }

  public void setTypeIsSet(boolean value) {
    if (!value) {
      this.type = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case STORAGE_INFOS:
      if (value == null) {
        unsetStorageInfos();
      } else {
        setStorageInfos((java.util.List<StorageInfo>)value);
      }
      break;

    case TYPE:
      if (value == null) {
        unsetType();
      } else {
        setType((OperationType)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case STORAGE_INFOS:
      return getStorageInfos();

    case TYPE:
      return getType();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case STORAGE_INFOS:
      return isSetStorageInfos();
    case TYPE:
      return isSetType();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof StorageRequest)
      return this.equals((StorageRequest)that);
    return false;
  }

  public boolean equals(StorageRequest that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_storageInfos = true && this.isSetStorageInfos();
    boolean that_present_storageInfos = true && that.isSetStorageInfos();
    if (this_present_storageInfos || that_present_storageInfos) {
      if (!(this_present_storageInfos && that_present_storageInfos))
        return false;
      if (!this.storageInfos.equals(that.storageInfos))
        return false;
    }

    boolean this_present_type = true && this.isSetType();
    boolean that_present_type = true && that.isSetType();
    if (this_present_type || that_present_type) {
      if (!(this_present_type && that_present_type))
        return false;
      if (!this.type.equals(that.type))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + ((isSetStorageInfos()) ? 131071 : 524287);
    if (isSetStorageInfos())
      hashCode = hashCode * 8191 + storageInfos.hashCode();

    hashCode = hashCode * 8191 + ((isSetType()) ? 131071 : 524287);
    if (isSetType())
      hashCode = hashCode * 8191 + type.getValue();

    return hashCode;
  }

  @Override
  public int compareTo(StorageRequest other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetStorageInfos()).compareTo(other.isSetStorageInfos());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetStorageInfos()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.storageInfos, other.storageInfos);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetType()).compareTo(other.isSetType());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetType()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.type, other.type);
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
    StringBuilder sb = new StringBuilder("StorageRequest(");
    boolean first = true;

    sb.append("storageInfos:");
    if (this.storageInfos == null) {
      sb.append("null");
    } else {
      sb.append(this.storageInfos);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("type:");
    if (this.type == null) {
      sb.append("null");
    } else {
      sb.append(this.type);
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

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class StorageRequestStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public StorageRequestStandardScheme getScheme() {
      return new StorageRequestStandardScheme();
    }
  }

  private static class StorageRequestStandardScheme extends org.apache.thrift.scheme.StandardScheme<StorageRequest> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, StorageRequest struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // STORAGE_INFOS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list8 = iprot.readListBegin();
                struct.storageInfos = new java.util.ArrayList<StorageInfo>(_list8.size);
                StorageInfo _elem9;
                for (int _i10 = 0; _i10 < _list8.size; ++_i10)
                {
                  _elem9 = new StorageInfo();
                  _elem9.read(iprot);
                  struct.storageInfos.add(_elem9);
                }
                iprot.readListEnd();
              }
              struct.setStorageInfosIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // TYPE
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.type = OperationType.findByValue(iprot.readI32());
              struct.setTypeIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, StorageRequest struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.storageInfos != null) {
        oprot.writeFieldBegin(STORAGE_INFOS_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.storageInfos.size()));
          for (StorageInfo _iter11 : struct.storageInfos)
          {
            _iter11.write(oprot);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      if (struct.type != null) {
        oprot.writeFieldBegin(TYPE_FIELD_DESC);
        oprot.writeI32(struct.type.getValue());
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class StorageRequestTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public StorageRequestTupleScheme getScheme() {
      return new StorageRequestTupleScheme();
    }
  }

  private static class StorageRequestTupleScheme extends org.apache.thrift.scheme.TupleScheme<StorageRequest> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, StorageRequest struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet optionals = new java.util.BitSet();
      if (struct.isSetStorageInfos()) {
        optionals.set(0);
      }
      if (struct.isSetType()) {
        optionals.set(1);
      }
      oprot.writeBitSet(optionals, 2);
      if (struct.isSetStorageInfos()) {
        {
          oprot.writeI32(struct.storageInfos.size());
          for (StorageInfo _iter12 : struct.storageInfos)
          {
            _iter12.write(oprot);
          }
        }
      }
      if (struct.isSetType()) {
        oprot.writeI32(struct.type.getValue());
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, StorageRequest struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        {
          org.apache.thrift.protocol.TList _list13 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
          struct.storageInfos = new java.util.ArrayList<StorageInfo>(_list13.size);
          StorageInfo _elem14;
          for (int _i15 = 0; _i15 < _list13.size; ++_i15)
          {
            _elem14 = new StorageInfo();
            _elem14.read(iprot);
            struct.storageInfos.add(_elem14);
          }
        }
        struct.setStorageInfosIsSet(true);
      }
      if (incoming.get(1)) {
        struct.type = OperationType.findByValue(iprot.readI32());
        struct.setTypeIsSet(true);
      }
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}

