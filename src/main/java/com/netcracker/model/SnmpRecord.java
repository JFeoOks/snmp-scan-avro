package com.netcracker.model;

public class SnmpRecord {

    private String iod;
    private String value;

    public SnmpRecord(String iod, String value) {
        this.iod = iod;
        this.value = value;
    }

    public String getIod() {
        return iod;
    }

    public void setIod(String iod) {
        this.iod = iod;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
