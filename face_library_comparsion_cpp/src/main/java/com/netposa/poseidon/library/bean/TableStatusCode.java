package com.netposa.poseidon.library.bean;

public enum TableStatusCode {
    IN_USING(1),
    WAITTING_DELETED(2),
    WAITTING_CREATED(3);

    private final int value;

    private TableStatusCode(int value) {
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
    public static TableStatusCode findByValue(int value) {
        switch (value) {
            case 1:
                return IN_USING;
            case 2:
                return WAITTING_DELETED;
            case 3:
                return WAITTING_CREATED;
            default:
                return null;
        }
    }
}
