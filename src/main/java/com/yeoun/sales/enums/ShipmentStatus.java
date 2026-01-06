package com.yeoun.sales.enums;

public enum ShipmentStatus {
    WAITING,     // 예약대기 (수주 등록 직후)
    RESERVED,   // 예약중 (재고 충분해서 예약됨)
    PENDING,    // 출고 등록
    LACK,        // 재고 부족
    SHIPPED,      // 출하완료
    CANCEL
}

