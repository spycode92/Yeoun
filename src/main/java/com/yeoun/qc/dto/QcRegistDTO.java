package com.yeoun.qc.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// QC 등록 화면 목록용
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QcRegistDTO {
	
	private Long qcResultId;
	
	// 작업지시번호
	private String orderId;
	
	// 제품코드
	private String prdId;
	
	// 제품명
	private String prdName;
	
	// 계획수량
	private Integer planQty;
	
	// QC 상태
	private String overallResult;
	
	// 검사일
	private LocalDate inspectionDate;

	// LOT 번호
	private String lotNo;
	
	// QC 대기 시작 시각 (QC_RESULT 생성 시각)
	private LocalDateTime qcCreatedAt;

	// 라인명
	private String lineName; 

}
