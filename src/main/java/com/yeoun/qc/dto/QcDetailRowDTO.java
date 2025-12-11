package com.yeoun.qc.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// QC 상세 모달 목록용
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QcDetailRowDTO {
	
	private String qcResultDtlId;	// QC 결과 상세ID
	private String qcItemId;		// QC 항목ID
	private String itemName;		// 항목명
	private String unit;			// 단위
	private String measureValue;	// 측정값
	private String result;			// 판정
	private String remark;			// 비고
	
	// 기준값 (설명형 텍스트) - 색상, 향, 특성 등
	private String stdText;
	
	// 수치형 기준값 (허용 하한/상한)
	private BigDecimal minValue;
	private BigDecimal maxValue;
	
	// 화면에 보여줄 최종 기준값
	// stdText가 없는 경우가 있기에 수치형 기준값으로 설정
	private String displayStd;
	
	
}
