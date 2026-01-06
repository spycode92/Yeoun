package com.yeoun.production.enums;

public enum BomStatus {
	READY,      // 전체 생산 가능   
    PARTIAL,    // 부분 생산 가능 -상태값...(발주중, 생산중)
    LACK,       // 생산 불가
    WAIT        // 계산 전 (초기 상태)
    
}
