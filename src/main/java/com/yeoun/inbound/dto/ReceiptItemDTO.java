package com.yeoun.inbound.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ReceiptItemDTO {
	private Long inboundItemId; 
	private String itemName; // 제품 / 원재료 이름
	private String itemId;
	private String itemType; // 입고 타입 
	private String lotNo; // LOT 번호
	private Long inboundAmount; // 검수 후 확정 수량
	private Long requestAmount; // 발주 수량 / 생산 수량
	private Long disposeAmount; // 불량 수량
	private LocalDateTime manufactureDate; // 제조일
	private LocalDateTime expirationDate;  // 유통기한
	private String locationId; // 창고위치
	private Long unitPrice; // 단가
	private Long supplyAmount; // 공급가액
	private Long totalPrice; // 합계
}
