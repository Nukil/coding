package com.netposa.poseidon.library.bean;

public enum ConnectionStatusCode {
    OK(1),
    DISCONNECTED(-1);

    private final int value;

    private ConnectionStatusCode(int value) {
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
    public static ConnectionStatusCode findByValue(int value) {
        switch (value) {
            case 1:
                return OK;
            case -1:
                return DISCONNECTED;
            default:
                return null;
        }
    }
}
