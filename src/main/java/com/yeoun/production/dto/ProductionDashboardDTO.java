package com.yeoun.production.dto;

import java.util.List;

import com.yeoun.process.dto.WorkOrderProcessDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductionDashboardDTO {
	
	// KPI 카드
    private long todayOrders;   // 오늘 작업지시 수
    private long inProgress;    // 진행중
    private long completed;     // 완료
    private long delayed;       // 지연 공정 수

    // 라인별 생산량 차트
    private List<String> lineLabels;  // ["라인1", "라인2", ...]
    private List<Long> lineSeries;    // [100, 200, 150, ...]

    // 불량 비율 차트 (양품 vs 불량)
    private long goodQtyTotal;        // 전체 양품 수량
    private long defectQtyTotal;      // 전체 불량 수량

    // 공정 현황 테이블 
    private List<WorkOrderProcessDTO> processList;

}
