package com.nukil.parse.util.bean;

public enum StatusCode {
    OK(1), 
    ERROR_PARAM(-1),
    ERROR_ANALYZE(-2),
    ERROR_OTHER(-3);

    private final int value;

    StatusCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static StatusCode findByValue(int value) {
        switch (value) {
            case 1:
                return OK;
            case -1:
                return ERROR_PARAM;
            case -2:
                return ERROR_ANALYZE;
            case -3:
                return ERROR_OTHER;
            default:
                return null;
        }
    }
}
