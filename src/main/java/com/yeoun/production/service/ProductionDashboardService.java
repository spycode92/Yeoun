package com.yeoun.production.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeoun.order.repository.WorkOrderRepository;
import com.yeoun.process.dto.WorkOrderProcessDTO;
import com.yeoun.process.service.WorkOrderProcessService;
import com.yeoun.production.dto.ProductionDashboardDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductionDashboardService {
	
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderProcessService workOrderProcessService; 

    @Transactional(readOnly = true)
    public ProductionDashboardDTO getDashboardData() {

        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday   = startOfToday.plusDays(1);

        // -------------------------------
        // 1) KPI 카드 데이터
        // -------------------------------
        long todayOrders = workOrderRepository.countCreatedBetween(startOfToday, endOfToday);
        long inProgress  = workOrderRepository.countByStatus("IN_PROGRESS");
        long completed   = workOrderRepository.countByStatus("DONE");
        long delayed     = workOrderRepository.countDelayedOrders(LocalDateTime.now());

        // -------------------------------
        // 2) 공정 현황 테이블 데이터
        // -------------------------------
        List<WorkOrderProcessDTO> processList =
                workOrderProcessService.getWorkOrderListForStatus();

        // -------------------------------
        // 3) 전체 Good, Defect 수량 합산
        // -------------------------------
        long goodTotal = processList.stream()
                .mapToLong(dto -> dto.getGoodQty() == null ? 0L : dto.getGoodQty())
                .sum();

        long defectTotal = processList.stream()
                .mapToLong(dto -> dto.getDefectQty() == null ? 0L : dto.getDefectQty())
                .sum();

        // -------------------------------
        // 4) 라인별 생산량 차트 
        // -------------------------------
        Map<String, Long> lineQtyMap = processList.stream()
                .collect(Collectors.groupingBy(
                        dto -> dto.getLineName() == null ? "미지정" : dto.getLineName(),
                        Collectors.summingLong(dto -> dto.getGoodQty() == null ? 0L : dto.getGoodQty())
                ));

        // 순서 예쁘게 정렬 (라인ID/이름 기준)
        List<String> lineLabels = lineQtyMap.keySet().stream()
                .sorted()
                .toList();

        List<Long> lineSeries = lineLabels.stream()
                .map(lineQtyMap::get)
                .toList();

        // -------------------------------
        // 5) 최종 DTO 조립
        // -------------------------------
        return ProductionDashboardDTO.builder()
                .todayOrders(todayOrders)
                .inProgress(inProgress)
                .completed(completed)
                .delayed(delayed)
                .lineLabels(lineLabels)
                .lineSeries(lineSeries)
                .goodQtyTotal(goodTotal)
                .defectQtyTotal(defectTotal)   
                .processList(processList)
                .build();
    }

}
