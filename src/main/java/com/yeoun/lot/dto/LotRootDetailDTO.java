package com.yeoun.lot.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// LOT ROOT 상세 (오른쪽 패널 개요) 
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LotRootDetailDTO {
	
	// LOT 기본 정보
	private String lotNo;					// LOT 코드
	private String productCode;				// 제품코드
	private String productName;				// 제품명
	private String productType;				// 제품유형 (완제품 등)
	
	// 생산 상태, 지시 정보
	private String lotStatusLabel; 			// "진행중", "생산완료" 		
	private String workOrderId;				// 작업지시번호
	
	// 수량 정보
	private Integer planQty;				// 생산지시수량 (계획 수량)
	private Integer goodQty;				// 현재 양품 수량
	private Integer defectQty;				// 현재 불량 수량
	
	// 일정 정보
	private LocalDate startDate;			// 시작일자 (실제 시각)
	private LocalDate expectedEndDate;		// 예상 종료일자
	
	// 라우트/공정 요약
	private String routeId;        			// 라우트 ID
	private String routeSteps;				// 라우트 단계
	
	// 출하 정보 (LOT_HISTORY로 계산)
	private String shippedYn;				// 출하 여부 (Y/N)
	private LocalDate shippedAt;			// 출하 일시
	private Integer shippedQty;				// 출하 수량

}
