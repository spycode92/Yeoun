package com.yeoun.process.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

// 생산 현황 추적 차트 응답 DTO
@Getter
@Builder
public class ProductionTrendResponseDTO {
	
	private String range;          // day | week | month
    private List<String> labels;   // x축 라벨
    private List<Long> planned;    // 계획 건수
    private List<Long> completed;  // 완료 건수

}
