package com.yeoun.common.e_num;

public enum AlarmDestination {
	MAIN("/alarm/main"),
	NOTICE("/alarm/notice"),
	EMP("/alarm/emp"),
	LEAVE("/alarm/leave"),
	ATTANDANCE("/alarm/attandance"),
	APPROVAL("/alarm/approval"),
	PAY("/alarm/pay"),
	MASTERDATA("/alarm/masterData"),
    INVENTORY("/alarm/inventory"),
    SALES("/alarm/sales"),
    PRODUCTION("/alarm/production"),
    ORDER("/alarm/order"),
    PROCESS("/alarm/process"),
    LOT("/alarm/lot"),
    QC("/alarm/qc"),
    ALARM("/alarm/alarm");

    private final String destination;

    AlarmDestination(String destination) {
        this.destination = destination;
    }

    public String getDestination() {
        return destination;
    }
}
