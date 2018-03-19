/**
 * Autogenerated by Thrift Compiler (0.10.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.netposa.poseidon.face.rpc.outrpc;


import java.util.Map;
import java.util.HashMap;
import org.apache.thrift.TEnum;

public enum StatusCode implements org.apache.thrift.TEnum {
  OK(1),
  ERROR_PARAM(2),
  ERROR_NOT_EXIST_LIBRARY(3),
  ERROR_OTHER(10);

  private final int value;

  private StatusCode(int value) {
    this.value = value;
  }

  /**
   * Get the integer value of this enum value, as defined in the Thrift IDL.
   */
  public int getValue() {
    return value;
  }

  /**
   * Find a the enum type by its integer value, as defined in the Thrift IDL.
   * @return null if the value is not found.
   */
  public static StatusCode findByValue(int value) { 
    switch (value) {
      case 1:
        return OK;
      case 2:
        return ERROR_PARAM;
      case 3:
        return ERROR_NOT_EXIST_LIBRARY;
      case 10:
        return ERROR_OTHER;
      default:
        return null;
    }
  }
}
