package com.yeoun.qc.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// QC 결과 조회 화면 목록용
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QcResultListDTO {
	
	// QC 결과 ID
	private Long qcResultId;
	
	// 작업지시번호
	private String orderId;
	
	// 제품코드
	private String prdId;
	
	// 제품명
	private String prdName;
	
	// 검사일자
	private LocalDate inspectionDate;
	
	// 전체판정
	private String overallResult;
	
	// 불합격사유
	private String failReason;
	
}
