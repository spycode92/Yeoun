package com.yeoun.process.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderProcessDTO {
	
	// 작업지시번호
	private String orderId;
	
	// 제품코드
	private String prdId;
	
	// 제품명
	private String prdName;
	
	// 라인 정보
	private String lineId;     
    private String lineName;
	
	// 계획수량
	private Integer planQty;
	
	// 양품수량 (= 최종 공정 기준 양품수량)
	private Integer goodQty;
	
	// 불량수량
	private Integer defectQty;
	
	// 진행률 (공정 단계 기반 == (완료 단계 수 / 전체 단계 수) * 100)
	private Integer progressRate;
	
	// 현재공정
	private String currentProcess;
	
	// 상태
	private String status;
	
	// 경과시간 (첫 공정이 시작된 시간 ~ 지금까지 걸린 총 시간)
	private String elapsedTime;

}
