package com.yeoun.process.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.yeoun.equipment.entity.ProdLine;
import com.yeoun.equipment.repository.ProdLineRepository;
import com.yeoun.order.repository.WorkOrderRepository;
import com.yeoun.process.dto.ImmediateActionRowDTO;
import com.yeoun.process.dto.LineStayRowDTO;
import com.yeoun.process.dto.ProductionDashboardKpiDTO;
import com.yeoun.process.dto.StayCellDTO;
import com.yeoun.process.entity.WorkOrderProcess;
import com.yeoun.process.mapper.ProductionTrendMapper;
import com.yeoun.process.repository.WorkOrderProcessRepository;
import com.yeoun.qc.repository.QcResultRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProcessDashboardService {
	
	private final QcResultRepository qcResultRepository;
	private final WorkOrderRepository workOrderRepository;
    private final WorkOrderProcessRepository workOrderProcessRepository;
    private final ProdLineRepository prodLineRepository;
    private final ProductionTrendMapper productionTrendMapper;

    // 상단 KPI 카드
    public ProductionDashboardKpiDTO getKpis() {

        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        LocalDate tomorrow = today.plusDays(1);
        
        // 큰 숫자
        // 오늘 작업지시
        long todayOrders = workOrderRepository.countTodayOrders(start, end);
        // 진행 중 작업지시 수
        long inProgressOrders = workOrderRepository.countByStatus("IN_PROGRESS");
        // 지연 작업지시 수 (예정완료시간 초과)
        long delayedOrders = workOrderRepository.countByStatusAndPlanEndDateBefore("IN_PROGRESS", now);
        // QC 대기 단계 수
        long qcPendingSteps = workOrderProcessRepository.countQcPendingOnly("PRC-QC");
        // QC 불합격 수
        long qcFailSteps = 
        		qcResultRepository.countByOverallResultAndInspectionDateGreaterThanEqualAndInspectionDateLessThan(
        				"FAIL", today, tomorrow
        	    );
        
        // 작은 숫자
        // 오늘 완료된 작업지시
        long completedToday = workOrderRepository.countByStatusAndActEndDateBetween("COMPLETED", start, end);
        // 오늘 기준 대기(진행중)
        long waitingToday = workOrderRepository.countByStatus("IN_PROGRESS");
        return ProductionDashboardKpiDTO.builder()
                .todayOrders(todayOrders)
                .inProgressOrders(inProgressOrders)
                .delayedOrders(delayedOrders)
                .qcPendingOrders(qcPendingSteps) 
                .qcFailSteps(qcFailSteps) 
                .completedDelta(completedToday)
                .waitingDelta(waitingToday)
                .build();
    }
    
    // =====================================================================================
    // 라인 6단계명 고정
    private static final Map<Integer, String> STEP_NAME_MAP = Map.of(
        1, "블렌딩",
        2, "여과",
        3, "충진",
        4, "캡·펌프",
        5, "품질검사",
        6, "포장"
    );

    // 조회는 READY까지 포함(표시/시작대기 계산용)
    private static final List<String> HEATMAP_STATUSES = List.of("IN_PROGRESS", "QC_PENDING", "READY", "DONE");
    
    public List<LineStayRowDTO> getLineStayHeatmap() {

        LocalDateTime now = LocalDateTime.now();

        // 1) 사용중(Y) 라인만 정렬 조회
        List<ProdLine> lines = prodLineRepository.findByUseYnOrderByLineIdAsc("Y");

        // 2) 히트맵 구성에 필요한 WOP 조회
        // - orderStatuses: 작업지시 상태 필터 (현재 IN_PROGRESS만 대상)
        List<String> orderStatuses = List.of("IN_PROGRESS");

        // - HEATMAP_STATUSES: 공정 상태 필터 (IN_PROGRESS/QC_PENDING/READY/DONE 등)
        List<WorkOrderProcess> wops =
            workOrderProcessRepository.findForHeatmap(HEATMAP_STATUSES, orderStatuses);

        // 3) (라인ID, stepSeq) 기준으로 그룹핑
        record Key(String lineId, Integer stepSeq) {}

        Map<Key, List<WorkOrderProcess>> grouped =
            wops.stream().collect(Collectors.groupingBy(
                w -> new Key(
                    w.getWorkOrder().getLine().getLineId(),
                    w.getStepSeq()
                )
            ));

        // 4) 라인 × 6단계 고정 생성
        List<LineStayRowDTO> rows = new ArrayList<>();

        for (ProdLine line : lines) {

            String lineId = line.getLineId();
            List<StayCellDTO> steps = new ArrayList<>();
            
            // 이 라인에서 가장 오래 체류 중인 공정 (대표 작업지시 추출용)
            WorkOrderProcess lineMaxWop = null;
            long lineMaxStay = -1;

            // 라인 단위 진행중/QC대기 합
            long lineActiveTotal = 0;

            for (int step = 1; step <= 6; step++) {

                // ------------------------------------------------------------
                // A) (라인, 단계) 에 해당하는 공정진행 목록
                // ------------------------------------------------------------
                List<WorkOrderProcess> list =
                    grouped.getOrDefault(new Key(lineId, step), List.of());
                
                WorkOrderProcess oneWop = list.stream()
                	    .sorted((a, b) -> Integer.compare(rank(b.getStatus()), rank(a.getStatus())))
                	    .findFirst()
                	    .orElse(null);
                
                LocalDateTime start = null;
                LocalDateTime end   = null;

                // 1) 대표 wop(oneWop) 없으면 그냥 null 유지
                if (oneWop != null) {

                    String st = oneWop.getStatus();

                    if ("IN_PROGRESS".equals(st)) {
                        // 진행중: startTime 있어야 의미 있음
                        start = oneWop.getStartTime();
                        end   = null; // 진행중이니 end는 null 유지
                    }

                    else if ("QC_PENDING".equals(st)) {
                        // QC 대기: QC startTime은 "없다"가 정책이니 null
                        // QC대기 시간 보여주고 싶으면 prev DONE endTime을 start처럼 사용
                        List<WorkOrderProcess> prevList =
                            grouped.getOrDefault(new Key(lineId, step - 1), List.of());

                        WorkOrderProcess prev = prevList.stream()
                            .filter(p -> p.getWorkOrder() != null && oneWop.getWorkOrder() != null)
                            .filter(p -> Objects.equals(p.getWorkOrder().getOrderId(), oneWop.getWorkOrder().getOrderId()))
                            .filter(p -> "DONE".equals(p.getStatus()) && p.getEndTime() != null)
                            .max(Comparator.comparing(WorkOrderProcess::getEndTime))
                            .orElse(null);

                        start = (prev != null) ? prev.getEndTime() : null;
                        end = null;
                    }

                    else if ("DONE".equals(st)) {
                        // 완료: 시작~끝 둘 다 있는 경우만 세팅(없으면 null 유지)
                        start = oneWop.getStartTime();
                        end   = oneWop.getEndTime();
                    }

                    else {
                        // READY 포함: 시간은 굳이 안 보여준다
                        start = null;
                        end   = null;
                    }
                }


            	// 진행중이 없어도(READY/DONE만 있어도) 라인 대표 작업지시 잡기
            	if (lineMaxWop == null && oneWop != null) {
            	    lineMaxWop = oneWop;
            	}


                // 진행중 카운트: IN_PROGRESS/QC_PENDING만 (실제로 흐름이 돈다고 보는 상태)
                long inProgressCnt = list.stream()
                	    .filter(w -> "IN_PROGRESS".equals(w.getStatus()))
                	    .count();

            	long qcPendingCnt = list.stream()
            	    .filter(w -> "QC_PENDING".equals(w.getStatus()))
            	    .count();
            	

                // READY 카운트(표시용)
                long readyCnt = list.stream()
                    .filter(w -> "READY".equals(w.getStatus()))
                    .count();
                boolean hasReady = readyCnt > 0;

                // "시작대기(startable)" = READY가 있고, 이전 단계에 DONE이 존재하면
                boolean prevDoneExists = false;
                if (step > 1) {
                    List<WorkOrderProcess> prevList =
                        grouped.getOrDefault(new Key(lineId, step - 1), List.of());

                    prevDoneExists = prevList.stream()
                        .anyMatch(w -> "DONE".equals(w.getStatus()));
                }

                long startableCnt = (readyCnt > 0 && prevDoneExists) ? readyCnt : 0;
                
                // 빈 라인 숨기기
                lineActiveTotal += (inProgressCnt + qcPendingCnt + startableCnt);
                
                // ------------------------------------------------------------
                // B) 체류시간 계산 대상만 추림
                //  - IN_PROGRESS: startTime 기준
                //  - QC_PENDING : endTime 기준 (너희 시스템에서 QC 대기 시간이 중요)
                // ------------------------------------------------------------
                List<WorkOrderProcess> activeList = list.stream()
                    .filter(w -> "IN_PROGRESS".equals(w.getStatus()) || "QC_PENDING".equals(w.getStatus()))
                    .toList();

                // ------------------------------------------------------------
                // C) stayMax(실제 체류시간 최대) + 그 대상(maxWop) 찾기
                // ------------------------------------------------------------
                WorkOrderProcess maxWop = null;
                long stayMax = 0;

                for (WorkOrderProcess w : activeList) {
                    long stayMin;

                    if ("IN_PROGRESS".equals(w.getStatus())) {
                        if (w.getStartTime() == null) continue;
                        stayMin = Duration.between(w.getStartTime(), now).toMinutes();
                    } else { // QC_PENDING
                    	// QC 대기시간 = "이전 공정 종료시간" ~ now
                        // 같은 라인/이 step(=QC)에서, 같은 작업지시의 이전 단계(step-1) DONE(endTime) 조회

                        Integer curStep = w.getStepSeq();
                        if (curStep == null || curStep <= 1) continue;

                        // 이전 단계(step-1) 목록
                        List<WorkOrderProcess> prevList =
                            grouped.getOrDefault(new Key(lineId, curStep - 1), List.of());

                        // "같은 작업지시"의 이전 단계 1건 찾기
                        WorkOrderProcess prev = prevList.stream()
                            .filter(p -> p.getWorkOrder() != null && w.getWorkOrder() != null)
                            .filter(p -> Objects.equals(p.getWorkOrder().getOrderId(), w.getWorkOrder().getOrderId()))
                            .filter(p -> "DONE".equals(p.getStatus()))
                            .filter(p -> p.getEndTime() != null)
                            .max(Comparator.comparing(WorkOrderProcess::getEndTime)) // 가장 최근 종료
                            .orElse(null);

                        if (prev == null || prev.getEndTime() == null) continue;

                        stayMin = Duration.between(prev.getEndTime(), now).toMinutes();
                    }

                    if (stayMin > stayMax) {
                        stayMax = stayMin;
                        maxWop = w;
                    }
                }
                
                // 이 step의 maxWop가 라인 전체 기준으로도 가장 오래면 갱신
                if (maxWop != null && stayMax > lineMaxStay) {
                	lineMaxStay = stayMax;
                	lineMaxWop = maxWop;
                }

                // ------------------------------------------------------------
                // D) expectedMin(기준 예상시간) 계산
                //  - maxWop가 있어야 의미 있음
                // ------------------------------------------------------------
                long expectedMin = 0;
                double ratio = 0.0; // 실제/기준

                if (maxWop != null) {
                    double totalEU = ProcessTimeCalculator.calcTotalEU(maxWop.getWorkOrder());
                    expectedMin = ProcessTimeCalculator.calcExpectedMinutes(
                        maxWop.getProcess().getProcessId(),
                        totalEU
                    );

                    if (expectedMin > 0) {
                        ratio = (double) stayMax / (double) expectedMin;
                    }
                }

                // ------------------------------------------------------------
                // E) 3단계 판정
                //  - OK    : 기준 이하
                //  - WARN  : 기준 초과 ~ 20% 초과 이하
                //  - DELAY : 20% 초과
                // ------------------------------------------------------------
                String level = "OK";

                if (expectedMin > 0 && stayMax > 0) {
                    if (ratio <= 1.0)      level = "OK";
                    else if (ratio <= 1.2) level = "WARN";
                    else                   level = "DELAY";
                }
                
                // ------------------------------------------------------------
                // F) 셀 DTO 생성 + steps에 추가
                // ------------------------------------------------------------
                StayCellDTO cell = StayCellDTO.builder()
                    .lineId(lineId)
                    .lineName(line.getLineName())
                    .stepSeq(step)
                    .stepName(STEP_NAME_MAP.get(step))
                    .stayMin(stayMax)
                    .inProgressCnt(inProgressCnt)
                    .qcPendingCnt(qcPendingCnt)
                    .readyCnt(readyCnt)
                    .hasReady(hasReady)
                    .startableCnt(startableCnt)
                    .level(level)
                    .startTime(start)
                    .endTime(end)
                    .build();

                steps.add(cell);
            }
            
            // “진행중/QC대기 0건인 라인”은 화면에서 제외
            if (lineActiveTotal == 0) continue;
            
            String activeOrderId = null;

            if (lineMaxWop != null && lineMaxWop.getWorkOrder() != null) {
                activeOrderId = lineMaxWop.getWorkOrder().getOrderId();
            }

            // 라인 1개 row 완성
            rows.add(LineStayRowDTO.builder()
                .lineId(lineId)
                .lineName(line.getLineName())
                .activeOrderId(activeOrderId)
                .steps(steps)
                .build());
        }

        return rows;
    }
    
    private int rank(String status) {
        if (status == null) return 0;
        return switch (status) {
            case "IN_PROGRESS" -> 4;
            case "QC_PENDING"  -> 3;
            case "DONE"        -> 2;
            case "READY"       -> 1;
            default            -> 0;
        };
    }
    

    // ===================================================
    // 4. 즉시 조치 리스트
    public List<ImmediateActionRowDTO> getImmediateActions(int limit) {
        // limit 방어 (0/음수 방지)
        int safeLimit = (limit <= 0) ? 10 : limit;
        return productionTrendMapper.selectImmediateActions(safeLimit);
    }


}
