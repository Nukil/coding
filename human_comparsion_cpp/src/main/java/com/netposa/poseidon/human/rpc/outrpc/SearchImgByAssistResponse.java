/**
 * Autogenerated by Thrift Compiler (0.10.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.netposa.poseidon.human.rpc.outrpc;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.10.0)", date = "2018-01-25")
public class SearchImgByAssistResponse implements org.apache.thrift.TBase<SearchImgByAssistResponse, SearchImgByAssistResponse._Fields>, java.io.Serializable, Cloneable, Comparable<SearchImgByAssistResponse> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("SearchImgByAssistResponse");

  private static final org.apache.thrift.protocol.TField R_CODE_FIELD_DESC = new org.apache.thrift.protocol.TField("rCode", org.apache.thrift.protocol.TType.I32, (short)1);
  private static final org.apache.thrift.protocol.TField MESSAGE_FIELD_DESC = new org.apache.thrift.protocol.TField("message", org.apache.thrift.protocol.TType.STRING, (short)2);
  private static final org.apache.thrift.protocol.TField RESULT_FIELD_DESC = new org.apache.thrift.protocol.TField("result", org.apache.thrift.protocol.TType.LIST, (short)3);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new SearchImgByAssistResponseStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new SearchImgByAssistResponseTupleSchemeFactory();

  /**
   * 
   * @see StatusCode
   */
  public StatusCode rCode; // required
  public java.lang.String message; // required
  public java.util.List<SearchImgResult> result; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    /**
     * 
     * @see StatusCode
     */
    R_CODE((short)1, "rCode"),
    MESSAGE((short)2, "message"),
    RESULT((short)3, "result");

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
        case 1: // R_CODE
          return R_CODE;
        case 2: // MESSAGE
          return MESSAGE;
        case 3: // RESULT
          return RESULT;
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
    tmpMap.put(_Fields.R_CODE, new org.apache.thrift.meta_data.FieldMetaData("rCode", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.EnumMetaData(org.apache.thrift.protocol.TType.ENUM, StatusCode.class)));
    tmpMap.put(_Fields.MESSAGE, new org.apache.thrift.meta_data.FieldMetaData("message", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.RESULT, new org.apache.thrift.meta_data.FieldMetaData("result", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, SearchImgResult.class))));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(SearchImgByAssistResponse.class, metaDataMap);
  }

  public SearchImgByAssistResponse() {
  }

  public SearchImgByAssistResponse(
    StatusCode rCode,
    java.lang.String message,
    java.util.List<SearchImgResult> result)
  {
    this();
    this.rCode = rCode;
    this.message = message;
    this.result = result;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public SearchImgByAssistResponse(SearchImgByAssistResponse other) {
    if (other.isSetRCode()) {
      this.rCode = other.rCode;
    }
    if (other.isSetMessage()) {
      this.message = other.message;
    }
    if (other.isSetResult()) {
      java.util.List<SearchImgResult> __this__result = new java.util.ArrayList<SearchImgResult>(other.result.size());
      for (SearchImgResult other_element : other.result) {
        __this__result.add(new SearchImgResult(other_element));
      }
      this.result = __this__result;
    }
  }

  public SearchImgByAssistResponse deepCopy() {
    return new SearchImgByAssistResponse(this);
  }

  @Override
  public void clear() {
    this.rCode = null;
    this.message = null;
    this.result = null;
  }

  /**
   * 
   * @see StatusCode
   */
  public StatusCode getRCode() {
    return this.rCode;
  }

  /**
   * 
   * @see StatusCode
   */
  public SearchImgByAssistResponse setRCode(StatusCode rCode) {
    this.rCode = rCode;
    return this;
  }

  public void unsetRCode() {
    this.rCode = null;
  }

  /** Returns true if field rCode is set (has been assigned a value) and false otherwise */
  public boolean isSetRCode() {
    return this.rCode != null;
  }

  public void setRCodeIsSet(boolean value) {
    if (!value) {
      this.rCode = null;
    }
  }

  public java.lang.String getMessage() {
    return this.message;
  }

  public SearchImgByAssistResponse setMessage(java.lang.String message) {
    this.message = message;
    return this;
  }

  public void unsetMessage() {
    this.message = null;
  }

  /** Returns true if field message is set (has been assigned a value) and false otherwise */
  public boolean isSetMessage() {
    return this.message != null;
  }

  public void setMessageIsSet(boolean value) {
    if (!value) {
      this.message = null;
    }
  }

  public int getResultSize() {
    return (this.result == null) ? 0 : this.result.size();
  }

  public java.util.Iterator<SearchImgResult> getResultIterator() {
    return (this.result == null) ? null : this.result.iterator();
  }

  public void addToResult(SearchImgResult elem) {
    if (this.result == null) {
      this.result = new java.util.ArrayList<SearchImgResult>();
    }
    this.result.add(elem);
  }

  public java.util.List<SearchImgResult> getResult() {
    return this.result;
  }

  public SearchImgByAssistResponse setResult(java.util.List<SearchImgResult> result) {
    this.result = result;
    return this;
  }

  public void unsetResult() {
    this.result = null;
  }

  /** Returns true if field result is set (has been assigned a value) and false otherwise */
  public boolean isSetResult() {
    return this.result != null;
  }

  public void setResultIsSet(boolean value) {
    if (!value) {
      this.result = null;
    }
  }

  public void setFieldValue(_Fields field, java.lang.Object value) {
    switch (field) {
    case R_CODE:
      if (value == null) {
        unsetRCode();
      } else {
        setRCode((StatusCode)value);
      }
      break;

    case MESSAGE:
      if (value == null) {
        unsetMessage();
      } else {
        setMessage((java.lang.String)value);
      }
      break;

    case RESULT:
      if (value == null) {
        unsetResult();
      } else {
        setResult((java.util.List<SearchImgResult>)value);
      }
      break;

    }
  }

  public java.lang.Object getFieldValue(_Fields field) {
    switch (field) {
    case R_CODE:
      return getRCode();

    case MESSAGE:
      return getMessage();

    case RESULT:
      return getResult();

    }
    throw new java.lang.IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new java.lang.IllegalArgumentException();
    }

    switch (field) {
    case R_CODE:
      return isSetRCode();
    case MESSAGE:
      return isSetMessage();
    case RESULT:
      return isSetResult();
    }
    throw new java.lang.IllegalStateException();
  }

  @Override
  public boolean equals(java.lang.Object that) {
    if (that == null)
      return false;
    if (that instanceof SearchImgByAssistResponse)
      return this.equals((SearchImgByAssistResponse)that);
    return false;
  }

  public boolean equals(SearchImgByAssistResponse that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_rCode = true && this.isSetRCode();
    boolean that_present_rCode = true && that.isSetRCode();
    if (this_present_rCode || that_present_rCode) {
      if (!(this_present_rCode && that_present_rCode))
        return false;
      if (!this.rCode.equals(that.rCode))
        return false;
    }

    boolean this_present_message = true && this.isSetMessage();
    boolean that_present_message = true && that.isSetMessage();
    if (this_present_message || that_present_message) {
      if (!(this_present_message && that_present_message))
        return false;
      if (!this.message.equals(that.message))
        return false;
    }

    boolean this_present_result = true && this.isSetResult();
    boolean that_present_result = true && that.isSetResult();
    if (this_present_result || that_present_result) {
      if (!(this_present_result && that_present_result))
        return false;
      if (!this.result.equals(that.result))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + ((isSetRCode()) ? 131071 : 524287);
    if (isSetRCode())
      hashCode = hashCode * 8191 + rCode.getValue();

    hashCode = hashCode * 8191 + ((isSetMessage()) ? 131071 : 524287);
    if (isSetMessage())
      hashCode = hashCode * 8191 + message.hashCode();

    hashCode = hashCode * 8191 + ((isSetResult()) ? 131071 : 524287);
    if (isSetResult())
      hashCode = hashCode * 8191 + result.hashCode();

    return hashCode;
  }

  @Override
  public int compareTo(SearchImgByAssistResponse other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = java.lang.Boolean.valueOf(isSetRCode()).compareTo(other.isSetRCode());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetRCode()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.rCode, other.rCode);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = java.lang.Boolean.valueOf(isSetMessage()).compareTo(other.isSetMessage());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMessage()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.message, other.message);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = java.lang.Boolean.valueOf(isSetResult()).compareTo(other.isSetResult());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetResult()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.result, other.result);
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
    java.lang.StringBuilder sb = new java.lang.StringBuilder("SearchImgByAssistResponse(");
    boolean first = true;

    sb.append("rCode:");
    if (this.rCode == null) {
      sb.append("null");
    } else {
      sb.append(this.rCode);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("message:");
    if (this.message == null) {
      sb.append("null");
    } else {
      sb.append(this.message);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("result:");
    if (this.result == null) {
      sb.append("null");
    } else {
      sb.append(this.result);
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

  private static class SearchImgByAssistResponseStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public SearchImgByAssistResponseStandardScheme getScheme() {
      return new SearchImgByAssistResponseStandardScheme();
    }
  }

  private static class SearchImgByAssistResponseStandardScheme extends org.apache.thrift.scheme.StandardScheme<SearchImgByAssistResponse> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, SearchImgByAssistResponse struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // R_CODE
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.rCode = com.netposa.poseidon.human.rpc.outrpc.StatusCode.findByValue(iprot.readI32());
              struct.setRCodeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // MESSAGE
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.message = iprot.readString();
              struct.setMessageIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // RESULT
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list24 = iprot.readListBegin();
                struct.result = new java.util.ArrayList<SearchImgResult>(_list24.size);
                SearchImgResult _elem25;
                for (int _i26 = 0; _i26 < _list24.size; ++_i26)
                {
                  _elem25 = new SearchImgResult();
                  _elem25.read(iprot);
                  struct.result.add(_elem25);
                }
                iprot.readListEnd();
              }
              struct.setResultIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, SearchImgByAssistResponse struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.rCode != null) {
        oprot.writeFieldBegin(R_CODE_FIELD_DESC);
        oprot.writeI32(struct.rCode.getValue());
        oprot.writeFieldEnd();
      }
      if (struct.message != null) {
        oprot.writeFieldBegin(MESSAGE_FIELD_DESC);
        oprot.writeString(struct.message);
        oprot.writeFieldEnd();
      }
      if (struct.result != null) {
        oprot.writeFieldBegin(RESULT_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.result.size()));
          for (SearchImgResult _iter27 : struct.result)
          {
            _iter27.write(oprot);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class SearchImgByAssistResponseTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public SearchImgByAssistResponseTupleScheme getScheme() {
      return new SearchImgByAssistResponseTupleScheme();
    }
  }

  private static class SearchImgByAssistResponseTupleScheme extends org.apache.thrift.scheme.TupleScheme<SearchImgByAssistResponse> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, SearchImgByAssistResponse struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet optionals = new java.util.BitSet();
      if (struct.isSetRCode()) {
        optionals.set(0);
      }
      if (struct.isSetMessage()) {
        optionals.set(1);
      }
      if (struct.isSetResult()) {
        optionals.set(2);
      }
      oprot.writeBitSet(optionals, 3);
      if (struct.isSetRCode()) {
        oprot.writeI32(struct.rCode.getValue());
      }
      if (struct.isSetMessage()) {
        oprot.writeString(struct.message);
      }
      if (struct.isSetResult()) {
        {
          oprot.writeI32(struct.result.size());
          for (SearchImgResult _iter28 : struct.result)
          {
            _iter28.write(oprot);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, SearchImgByAssistResponse struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet incoming = iprot.readBitSet(3);
      if (incoming.get(0)) {
        struct.rCode = com.netposa.poseidon.human.rpc.outrpc.StatusCode.findByValue(iprot.readI32());
        struct.setRCodeIsSet(true);
      }
      if (incoming.get(1)) {
        struct.message = iprot.readString();
        struct.setMessageIsSet(true);
      }
      if (incoming.get(2)) {
        {
          org.apache.thrift.protocol.TList _list29 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
          struct.result = new java.util.ArrayList<SearchImgResult>(_list29.size);
          SearchImgResult _elem30;
          for (int _i31 = 0; _i31 < _list29.size; ++_i31)
          {
            _elem30 = new SearchImgResult();
            _elem30.read(iprot);
            struct.result.add(_elem30);
          }
        }
        struct.setResultIsSet(true);
      }
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}

