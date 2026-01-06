package com.yeoun.lot.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LotMaterialDetailDTO {
	
	// 원자재 기본 정보
	private String matId;					// 자재ID
	private String matName;					// 자재명
	private String matType;					// 자재유형
	private String matUnit;					// 단위
	
	// LOT 정보
	private String lotNo;					// LOT 번호
	private String lotType;					// LOT 유형 (RAW, WIP 등)
	private String lotStatus;				// LOT 상태
	private LocalDateTime lotCreatedDate;	// LOT 생성일

	// 투입 (완제품 기준)
	private Integer usedQty;				// 사용 수량
	
	// 입고 정보
	private String inboundId;				// 입고 ID
	private Long inboundAmount;				// 입고수량
	private LocalDateTime manufactureDate;	// 제조일
	private LocalDateTime expirationDate; 	// 유통기한
	private String inboundLocationId;		// 입고/보관 위치
	
	// 재고/잔량 정보
	private Long ivAmount;					// 현재 재고 수량
	private String ivStatus;				// 재고상태 : (정상 NORMAL / 임박 DISPOSAL_WAIT / 유통기한 만료 EXPIRED)
	private String inventoryLocationId;		// 현재 재고 위치
	private LocalDateTime ibDate;			// 입고일
	
	// 거래처 정보
	private String clientId;				// 거래처 ID
	private String clientName;				// 거래처명
	private String businessNo;				// 사업자번호
	private String managerTel;				// 연락처
	
	
	
}
