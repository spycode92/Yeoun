package com.yeoun.process.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionDashboardKpiDTO {
	
	private long todayOrders;		// 오늘 작업지시 (생성)
	private long inProgressOrders;	// 진행 중 작업지시 수
	private long delayedOrders;		// 지연 작업지시 수 (예정완료시간 초과)
	private long qcPendingOrders;	// QC 대기
	private long qcFailSteps;		// QC 불합격
	
	private long completedDelta; 	// ↑ 처리 +N
	private long waitingDelta;   	// ↓ 대기 N
	

}
