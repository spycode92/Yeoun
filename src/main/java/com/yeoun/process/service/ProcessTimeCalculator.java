package com.yeoun.process.service;

import static com.yeoun.process.constant.ProcessTimeStandard.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.yeoun.process.util.EuConverter;
import com.yeoun.process.util.ProductSpecParser;
import com.yeoun.process.entity.WorkOrderProcess;
import com.yeoun.masterData.entity.ProductMst;
import com.yeoun.masterData.entity.RouteStep;
import com.yeoun.order.entity.WorkOrder;

/**
 * =====================================================
 * 공정 예상시간 / 지연 판정 계산기
 * -----------------------------------------------------
 * 역할:
 * 1) 작업지시 기준 총 작업량(EU) 계산
 * 2) 공정별 예상 소요시간 계산
 * 3) 실제 경과시간 대비 지연 여부 판단
 * =====================================================
 */
public class ProcessTimeCalculator {
	
	private ProcessTimeCalculator() {}

    // 작업지시 기준 총 EU 계산
	// - 제품 1개당 EU를 구한 뒤 계획수량(planQty)과 곱해서 "총 작업량" 계산
	public static double calcTotalEU(WorkOrder wo) {

		// 작업지시가 없거나 계획수량이 없으면 계산 불가
	    if (wo == null || wo.getPlanQty() == null) return 0;

	    ProductMst p = wo.getProduct();
	    if (p == null) return wo.getPlanQty();

	    // 제품 스펙 문자열: 예) "용량 30ml - 과일향, 꽃향"
	    String spec = p.getPrdSpec();      
	    // 제품 형태(액체/고체) 판단에 쓰는 값: 예) "LIQUID" / "SOLID"
	    String itemName = p.getItemName(); 

	    // spec에서 숫자만 뽑아서 용량(size)을 추출: 30, 50, 100
	    int size = ProductSpecParser.extractSize(spec);
	    // LIQUID / SOLID 결정:
        // 1) itemName을 우선 신뢰, 2) 없으면 spec에서 ml/g로 판단
	    String form = ProductSpecParser.extractForm(spec, itemName);

	    // 용량/형태를 EU로 환산 (예: LIQUID 50ml = 1EU, 30ml = 0.6EU)
	    double euPerUnit = EuConverter.toEU(form, size);

	    // 총 EU = 계획수량 * 1개당 EU
	    return wo.getPlanQty() * euPerUnit;
	}


    // 공정별 예상 소요시간 계산
	// - 배치 공정(고정 분): 그냥 정해진 분 반환
	//  - 단위 공정(EU당 초): totalEU * (초/EU) -> 초를 분으로 변환
    public static long calcExpectedMinutes(String processId, double totalEU) {

        // 배치 공정
    	// - PRC-BLD(블렌딩)=30분, PRC-FLT(여과)=15분, PRC-QC=20분
        Integer batchMin = BATCH_MINUTES.get(processId);
        if (batchMin != null) {
            return batchMin;
        }

        // 단위 공정
        // - PRC-FIL(충전)=8초/EU, PRC-CAP=6초/EU, PRC-LBL=10초/EU
        Integer secPerEU = UNIT_SECONDS_PER_EU.get(processId);
        
        // 단위 공정이면:
        // - totalEU * secPerEU = 총 소요시간(초)
        // - /60 해서 분으로 변환
        // - ceil 올림 처리: 8.1분이면 9분으로 (예상시간은 보수적으로)
        if (secPerEU != null) {
            return (long) Math.ceil((totalEU * secPerEU) / 60.0);
        }
        return 0;
    }

    // 공정 지연 여부 판정
    // - expectedMin(예상시간) vs elapsedMin(실경과시간)을 비교
    public static boolean isDelayed(WorkOrderProcess wop, double totalEU) {
        if (wop == null || wop.getProcess() == null) return false;

        String status = wop.getStatus();
        if (status == null) return false;

        long expectedMin = calcExpectedMinutes(wop.getProcess().getProcessId(), totalEU);
        if (expectedMin <= 0) return false;

        // READY는 지연 판단 안 함
        if ("READY".equals(status)) return false;

        // QC_PENDING은 지연 판단 안 함
        if ("QC_PENDING".equals(status)) return false;

        // IN_PROGRESS: startTime ~ now
        if ("IN_PROGRESS".equals(status)) {
            if (wop.getStartTime() == null) return false;
            long elapsedMin = Duration.between(wop.getStartTime(), LocalDateTime.now()).toMinutes();
            return elapsedMin > expectedMin;
        }

        // DONE: startTime ~ endTime
        if ("DONE".equals(status)) {
            if (wop.getStartTime() == null || wop.getEndTime() == null) return false;
            long actualMin = Duration.between(wop.getStartTime(), wop.getEndTime()).toMinutes();
            return actualMin > expectedMin;
        }

        return false;
    }
    
    // 작업지시 1건의 "전체 공정 예상시간(분)" 계산
    // - routeSteps 순서대로 공정별 예상시간을 합산
    public static long calcExpectedMinutesForWorkOrderFromRoute(WorkOrder wo, List<RouteStep> routeSteps) {

        if (wo == null || routeSteps == null || routeSteps.isEmpty()) return 0;

        // 1) 작업지시 기준 총 EU 계산 (planQty * euPerUnit)
        double totalEU = calcTotalEU(wo);

        // 2) 라우트 공정(step)들을 돌면서 공정별 예상시간(분) 합산
        long totalMin = 0;

        for (RouteStep step : routeSteps) {
            if (step == null || step.getProcess() == null) continue;

            String processId = step.getProcess().getProcessId();
            if (processId == null) continue;

            // 공정 1개 예상시간(분)
            long expectedMin = calcExpectedMinutes(processId, totalEU);
            totalMin += expectedMin;
        }

        return totalMin;
    }
    
    
}
