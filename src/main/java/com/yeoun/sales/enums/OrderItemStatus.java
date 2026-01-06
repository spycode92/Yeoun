package com.yeoun.sales.enums;

public enum OrderItemStatus {
	REQUEST("요청"),
    CONFIRMED("수주확정"),
    PLANNED("생산계획"),
    SHIPPED("출하완료"),
	CANCEL("취소");
	
	  private final String label;

	    OrderItemStatus(String label) {
	        this.label = label;
	    }

	    public String getLabel() {
	        return label;
	    }
}

