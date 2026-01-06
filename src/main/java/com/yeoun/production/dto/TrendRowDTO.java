package com.yeoun.production.dto;

import lombok.Getter;
import lombok.Setter;

//생산 현황 추적 차트 (Mapper 집계 결과 1행)
@Getter
@Setter
public class TrendRowDTO {
	
	private String label;	// label: 그룹 키 (YYYY-MM-DD / IYYY-IW / YYYY-MM)
    private Long cnt;		// cnt: 건수

}
