package com.yeoun.process.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeoun.common.e_num.AlarmDestination;
import com.yeoun.common.service.AlarmService;
import com.yeoun.inbound.service.InboundService;
import com.yeoun.lot.dto.LotHistoryDTO;
import com.yeoun.lot.dto.LotMasterDTO;
import com.yeoun.lot.entity.LotMaster;
import com.yeoun.lot.entity.LotRelationship;
import com.yeoun.lot.repository.LotMasterRepository;
import com.yeoun.lot.repository.LotRelationshipRepository;
import com.yeoun.lot.service.LotTraceService;
import com.yeoun.masterData.entity.RouteHeader;
import com.yeoun.masterData.entity.RouteStep;
import com.yeoun.masterData.repository.RouteHeaderRepository;
import com.yeoun.masterData.repository.RouteStepRepository;
import com.yeoun.order.dto.MaterialAvailabilityDTO;
import com.yeoun.order.entity.WorkOrder;
import com.yeoun.order.mapper.OrderMapper;
import com.yeoun.order.repository.WorkOrderRepository;
import com.yeoun.outbound.entity.OutboundItem;
import com.yeoun.outbound.repository.OutboundItemRepository;
import com.yeoun.process.dto.WorkOrderProcessDTO;
import com.yeoun.process.dto.WorkOrderProcessDetailDTO;
import com.yeoun.process.dto.WorkOrderProcessStepDTO;
import com.yeoun.process.entity.WorkOrderProcess;
import com.yeoun.process.repository.WorkOrderProcessRepository;
import com.yeoun.production.entity.ProductionPlan;
import com.yeoun.production.entity.ProductionPlanItem;
import com.yeoun.production.enums.ProductionStatus;
import com.yeoun.production.repository.ProductionPlanItemRepository;
import com.yeoun.production.repository.ProductionPlanRepository;
import com.yeoun.qc.entity.QcResult;
import com.yeoun.qc.repository.QcResultRepository;
import com.yeoun.qc.service.QcResultService;

import lombok.RequiredArgsConstructor;

/**
 * 작업지시 기준 공정 진행 현황 Service
 */
@Service
@RequiredArgsConstructor
public class WorkOrderProcessService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderProcessRepository workOrderProcessRepository;
    private final RouteHeaderRepository routeHeaderRepository;
    private final RouteStepRepository routeStepRepository;
    private final QcResultRepository qcResultRepository;
    private final QcResultService qcResultService;
    private final InboundService inboundService;
    private final AlarmService alarmService;
    private final OrderMapper orderMapper;
    
    // LOT 관련
    private final LotTraceService lotTraceService;
    private final LotMasterRepository lotMasterRepository;
    private final LotRelationshipRepository lotRelationshipRepository;
    
    // 출고(자재) 관련 - LOT_RELATIONSHIP 만들 때 사용
    private final OutboundItemRepository outboundItemRepository;
    
    // 생산계획 관련 - 공정 종료 시 상태값 변경
    private final ProductionPlanRepository productionPlanRepository;
    private final ProductionPlanItemRepository productionPlanItemRepository;
    
    // =========================================================================
    // 검색 조건 없는 공정현황 목록 (안 쓰면 삭제)
    @Transactional(readOnly = true)
    public List<WorkOrderProcessDTO> getWorkOrderListForStatus() {
        return getWorkOrderListForStatus(null, null, null, null);
    }
    
	// =========================================================================
	// 1. 공정현황 메인 목록 (검색/필터/정렬 포함)
	// =========================================================================
	@Transactional(readOnly = true)
	public List<WorkOrderProcessDTO> getWorkOrderListForStatus(LocalDate workDate, String processId, String status, String keyword) {
		
	    // 1) 공정현황 대상 작업지시만 조회
	    List<String> statuses = List.of("RELEASED", "IN_PROGRESS");
	    List<WorkOrder> workOrders =
	            workOrderRepository.findByStatusInAndOutboundYn(statuses, "Y");
	
	     // 대상 자체가 없으면 빈 리스트 반환
	     if (workOrders.isEmpty()) {
	         return List.of();
	     }
	
	    // 2) 작업일자(workDate) 필터
	    // 현재는 WorkOrder.createdDate를 작업지시일자로 사용 중
	    // (추후 actStartDate / planDate 등으로 기준 변경 시 이 부분만 수정하면 됨)
	    if (workDate != null) {
	        workOrders = workOrders.stream()
	                .filter(w ->
	                        w.getCreatedDate() != null &&
	                        w.getCreatedDate().toLocalDate().equals(workDate)
	                )
	                .collect(Collectors.toList());
	    }
	    if (workOrders.isEmpty()) return List.of();
	
	    // 3) 작업지시 상태(status) 필터
	    if (status != null && !status.isBlank()) {
	        workOrders = workOrders.stream()
	                .filter(w -> status.equals(w.getStatus()))
	                .collect(Collectors.toList());
	    }
	    if (workOrders.isEmpty()) return List.of();
	
	    // 4) 정렬: 상태 우선순위 + 작업지시번호
	    workOrders.sort(
	    	    Comparator.comparing((WorkOrder w) -> statusPriority(w.getStatus()))
	    	              .thenComparing(WorkOrder::getCreatedDate, Comparator.nullsLast(Comparator.naturalOrder()))
	    	              .thenComparing(WorkOrder::getOrderId)
	    	);

	    // 5) 남은 작업지시의 ID 목록 추출
	    // 이후 공정단계 / QC 결과를 IN 조회로 일괄 조회하기 위한 준비
	    List<String> orderIds = workOrders.stream()
	            .map(WorkOrder::getOrderId)
	            .toList();
	
	    // 6) 공정단계(WorkOrderProcess) 일괄 조회
	    // N+1 방지 포인트: 작업지시별로 조회하지 않고 IN + 정렬로 한 번에 조회
	    // 정렬 기준: orderId ASC, stepSeq ASC (화면 계산 시 순서가 중요)
	    List<WorkOrderProcess> allProcesses =
	            workOrderProcessRepository
	                    .findByWorkOrderOrderIdInOrderByWorkOrderOrderIdAscStepSeqAsc(orderIds);
	
	    // 작업지시번호별 공정 리스트로 묶어두면 DTO 변환 때 빠르게 매칭 가능
	    Map<String, List<WorkOrderProcess>> processMap = allProcesses.stream()
	            .collect(Collectors.groupingBy(p -> p.getWorkOrder().getOrderId()));
	
	    // 7) QC 결과(QcResult) 일괄 조회
	    // 작업지시 단위로 QC_RESULT가 존재하며, 목록에서 양품/불량을 보여주기 위함
	    List<QcResult> allQcResults = qcResultRepository.findByOrderIdIn(orderIds);
	
	    // orderId -> qcResult 맵 (중복 방어: 같은 orderId가 여러 건이면 첫 번째만 사용)
	    Map<String, QcResult> qcMap = allQcResults.stream()
	            .collect(Collectors.toMap(
	                    QcResult::getOrderId,
	                    qc -> qc,
	                    (q1, q2) -> q1
	            ));
	
	    // 8) DTO 변환: WorkOrder(헤더) + 공정단계 + QC 결과를 합쳐 요약 DTO 생성
	    List<WorkOrderProcessDTO> dtoList = workOrders.stream()
	            .map(w -> {
	                List<WorkOrderProcess> processes =
	                        processMap.getOrDefault(w.getOrderId(), List.of());
	                QcResult qcResult = qcMap.get(w.getOrderId());
	                return toProcessSummaryDto(w, processes, qcResult);
	            })
	            .collect(Collectors.toList());
	
	    // 9) 현재공정 필터(DTO단)
	    if (processId != null && !processId.isBlank()) {
	        dtoList = dtoList.stream()
	                .filter(dto -> processId.equals(dto.getCurrentProcess()))
	                .toList();
	    }
	
	    // 10) 키워드 검색(DTO단)
	    // 검색 대상: 작업지시번호 / 제품ID / 제품명
	    if (keyword != null && !keyword.isBlank()) {
	        String kw = keyword.toLowerCase();
	
	        dtoList = dtoList.stream()
	                .filter(dto ->
	                        (dto.getOrderId() != null && dto.getOrderId().toLowerCase().contains(kw)) ||
	                        (dto.getPrdId() != null   && dto.getPrdId().toLowerCase().contains(kw)) ||
	                        (dto.getPrdName() != null && dto.getPrdName().toLowerCase().contains(kw))
	                )
	                .toList();
	    }
	
	    return dtoList;
	}

    
	private int statusPriority(String status) {
	    if (status == null) return 99;

	    return switch (status) {
	        case "IN_PROGRESS" -> 1;   // 진행중
	        case "RELEASED"    -> 2;   // 대기
	        default            -> 50;  // 기타 상태
	    };
	}



    /**
     * 공정현황 목록용 요약 DTO 생성
     */
    private WorkOrderProcessDTO toProcessSummaryDto(WorkOrder workOrder,
    												List<WorkOrderProcess> processes,
    												QcResult qcResult) {

        // 양품수량: 작업지시당 QC_RESULT 1건 기준
        int goodQty = 0;
        if (qcResult != null && qcResult.getGoodQty() != null) { 
            goodQty = qcResult.getGoodQty();
        }
        
        // 2) 불량수량
        int defectQty = 0;
        
        if (qcResult != null && qcResult.getDefectQty() != null) {
            // 1순위: QC_RESULT.DEFECT_QTY
            defectQty = qcResult.getDefectQty();
        } else {
            // 2순위: 공정 마지막 단계(최종 공정)의 DEFECT_QTY 사용
            WorkOrderProcess lastStep = processes.stream()
                    .max(Comparator.comparing(
                            p -> p.getStepSeq() == null ? 0 : p.getStepSeq()
                    ))
                    .orElse(null);

            if (lastStep != null && lastStep.getDefectQty() != null) {
                defectQty = lastStep.getDefectQty();
            }
        }

        int progressRate = calculateProgressRateHybrid(processes);
        String currentProcess = resolveCurrentProcess(processes);
        LocalDateTime endAt = null;

	    // 완료/폐기면 actEndDate로 끊기
	    if ("COMPLETED".equals(workOrder.getStatus()) || "SCRAPPED".equals(workOrder.getStatus())) {
	        endAt = workOrder.getActEndDate();
	
	        // 혹시 actEndDate 없으면 마지막 공정 endTime으로 대체
	        if (endAt == null) {
	            endAt = processes.stream()
	                    .map(WorkOrderProcess::getEndTime)
	                    .filter(Objects::nonNull)
	                    .max(LocalDateTime::compareTo)
	                    .orElse(null);
	        }
	    }
	
	    String elapsedTime = calculateElapsedTime(processes, endAt);


        WorkOrderProcessDTO dto = new WorkOrderProcessDTO();
        dto.setOrderId(workOrder.getOrderId());
        dto.setPrdId(workOrder.getProduct().getPrdId());
        dto.setPrdName(workOrder.getProduct().getPrdName());
        dto.setPlanQty(workOrder.getPlanQty());
        dto.setStatus(workOrder.getStatus());
        if ("COMPLETED".equals(workOrder.getStatus()) || "SCRAPPED".equals(workOrder.getStatus())) {
        	dto.setDoneTime(workOrder.getActEndDate()); // 없으면 null
        }
        dto.setPlanStartDate(workOrder.getPlanStartDate());
        dto.setPlanEndDate(workOrder.getPlanEndDate());
        
        // 라인 정보
        if (workOrder.getLine() != null) {
            dto.setLineId(workOrder.getLine().getLineId());
            dto.setLineName(workOrder.getLine().getLineName());
        }

        dto.setGoodQty(goodQty);
        dto.setDefectQty(defectQty);
        dto.setProgressRate(progressRate);
        dto.setCurrentProcess(currentProcess);
        dto.setElapsedTime(elapsedTime);

        return dto;
    }

    /**
     * 진행률(완료율) 계산
     * - DONE만 완료로 인정
     * - IN_PROGRESS/QC_PENDING/READY는 미완료
     */
    private int calculateProgressRateHybrid(List<WorkOrderProcess> processes) {
        if (processes == null || processes.isEmpty()) return 0;

        int totalSteps = processes.size();

        long doneCount = processes.stream()
            .filter(p -> "DONE".equalsIgnoreCase(p.getStatus()))
            .count();

        boolean hasInProgress = processes.stream()
            .anyMatch(p -> "IN_PROGRESS".equalsIgnoreCase(p.getStatus())
                        || "QC_PENDING".equalsIgnoreCase(p.getStatus()));

        double progress = doneCount * 100.0 / totalSteps;

        if (hasInProgress && doneCount < totalSteps) {
            progress += (100.0 / totalSteps) * 0.5;
        }

        return (int) Math.round(Math.min(progress, 99));
    }

    /**
     * 현재 공정명 계산
     * - IN_PROGRESS 우선
     * - 없으면 READY 중 가장 앞(stepSeq) 단계
     * - 모두 DONE 또는 QC_PENDING이면 "완료"
     * - 그 외에는 "대기"
     */
    private String resolveCurrentProcess(List<WorkOrderProcess> processes) {
        if (processes.isEmpty()) {
            return "대기";
        }
        
        // 0) QC_PENDING(조치 필요) 우선
        WorkOrderProcess qcPending = processes.stream()
                .filter(p -> "QC_PENDING".equals(p.getStatus()))
                .findFirst()
                .orElse(null);
        
        if (qcPending != null) {
            return qcPending.getProcess().getProcessName(); // "QC 검사"
        }

        // IN_PROGRESS 공정 우선
        WorkOrderProcess inProgress = processes.stream()
                .filter(p -> "IN_PROGRESS".equals(p.getStatus()))
                .findFirst()
                .orElse(null);

        if (inProgress != null) {
            return inProgress.getProcess().getProcessName();
        }

        // 2) READY 중 가장 앞 단계
        WorkOrderProcess nextReady = processes.stream()
                .filter(p -> "READY".equals(p.getStatus()))
                .sorted((a, b) -> Integer.compare(
                        a.getStepSeq() == null ? 0 : a.getStepSeq(),
                        b.getStepSeq() == null ? 0 : b.getStepSeq()
                ))
                .findFirst()
                .orElse(null);

        if (nextReady != null) {
            return nextReady.getProcess().getProcessName();
        }

        // 공정이 있고, 모두 DONE 또는 QC_PENDING이면 "완료"
        boolean allDoneOrQcPending = processes.stream()
                .allMatch(p -> "DONE".equals(p.getStatus()) || "QC_PENDING".equals(p.getStatus()));

        if (allDoneOrQcPending) {
            return "완료";
        }

        return "대기";
    }

    /**
     * 경과시간 계산
     * - 진행중: firstStart ~ now
     * - 완료/폐기: firstStart ~ endAt(완료/폐기 시각)
     */
    private String calculateElapsedTime(List<WorkOrderProcess> processes, LocalDateTime endAtOrNull) {

        LocalDateTime firstStart = processes.stream()
                .map(WorkOrderProcess::getStartTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        if (firstStart == null) return "-";

        LocalDateTime endAt = (endAtOrNull != null) ? endAtOrNull : LocalDateTime.now();

        // 방어: endAt이 firstStart보다 빠르면 firstStart로 맞춤
        if (endAt.isBefore(firstStart)) endAt = firstStart;

        Duration d = Duration.between(firstStart, endAt);
        long hours = d.toHours();
        long minutes = d.toMinutesPart();

        return hours + "시간 " + minutes + "분";
    }


    // =========================================================================
    // 2. 공정현황 상세 모달
    // =========================================================================
    @Transactional(readOnly = true)
    public WorkOrderProcessDetailDTO getWorkOrderProcessDetail(String orderId) {

        // 1) 작업지시 기본정보
        WorkOrder workOrder = workOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 작업지시: " + orderId));

        // 2) 제품별 라우트 찾기
        String routeId = workOrder.getRouteId();
        
        RouteHeader routeHeader = routeHeaderRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 작업지시의 라우트가 없습니다. ROUTE_ID=" + routeId));

        // 3) 라우트 단계 목록 (마스터 기준)
        List<RouteStep> steps = routeStepRepository
                .findByRouteHeaderOrderByStepSeqAsc(routeHeader);

        // 4) 이 작업지시의 공정진행 데이터
        List<WorkOrderProcess> processes =
                workOrderProcessRepository.findByWorkOrderOrderIdOrderByStepSeqAsc(orderId);

        // RouteStep 기준으로 WorkOrderProcess 매핑
        Map<String, WorkOrderProcess> processMap = processes.stream()
                .filter(p -> p.getRouteStep() != null && p.getRouteStep().getRouteStepId() != null)
                .collect(Collectors.toMap(
                        p -> p.getRouteStep().getRouteStepId(),
                        p -> p,
                        (p1, p2) -> p1
                ));
        
        // 5) 상단 요약 DTO
        WorkOrderProcessDTO headerDto = new WorkOrderProcessDTO();
        headerDto.setOrderId(workOrder.getOrderId());
        headerDto.setPrdId(workOrder.getProduct().getPrdId());
        headerDto.setPrdName(workOrder.getProduct().getPrdName());
        headerDto.setPlanQty(workOrder.getPlanQty());
        headerDto.setStatus(workOrder.getStatus());
        headerDto.setLineId(workOrder.getLine().getLineId());
        headerDto.setLineName(workOrder.getLine().getLineName());
        
        // QC 결과 PASS 여부 (포장 공정 시작 조건)
        boolean isQcPassed = qcResultRepository.existsByOrderIdAndOverallResult(orderId, "PASS");

        // 6) 공정 단계 DTO 리스트 + 버튼 플래그 세팅
        List<WorkOrderProcessStepDTO> stepDTOs = buildStepDtos(steps, processMap, workOrder, isQcPassed);
        
        return new WorkOrderProcessDetailDTO(headerDto, stepDTOs);
    }

    /**
     * 공정 목록/상세 모달에서 사용하는 DTO 리스트를 반환
     * 공정 설계(RouteStep) + 공정 실행 상태(WorkOrderProcess)를 결합
     */
    private List<WorkOrderProcessStepDTO> buildStepDtos(List<RouteStep> steps,
											            Map<String, WorkOrderProcess> processMap,
											            WorkOrder workOrder,
											            boolean isQcPassed) {

        // 작업지시 계획수량 (1EA 기준값 및 3단계 이후 기준값 계산용)
        Integer planQty = workOrder.getPlanQty();
        
        // 작업지시 기준 총 작업량(EU) 계산
        // - PRD_SPEC에서 30ml/50ml/100ml/5g/10g 파싱
        // - ITEM_NAME(LIQUID/SOLID) 참고
        // - planQty(EA) × EU 환산값 = totalEU
        double totalEU = ProcessTimeCalculator.calcTotalEU(workOrder);

        // 1) RouteStep 기준으로 공정 단계 DTO를 하나씩 생성
        List<WorkOrderProcessStepDTO> stepDTOs = steps.stream()
                .map(step -> {

                    WorkOrderProcessStepDTO dto = new WorkOrderProcessStepDTO();

                    // ----------------------------
                    // (1) 기본 공정 정보 세팅
                    // ----------------------------
                    dto.setStepSeq(step.getStepSeq());                       // 공정 순번
                    dto.setProcessId(step.getProcess().getProcessId());      // 공정 코드 
                    dto.setProcessName(step.getProcess().getProcessName());  // 공정명
                    dto.setPlanQty(planQty);								 // 계획수량

                    // RouteStep -> WorkOrderProcess 매핑
                    WorkOrderProcess proc = processMap.get(step.getRouteStepId());

                    // ----------------------------
                    // (2) 실행 데이터 복사 (상태/시간/수량/메모)
                    // ----------------------------
                    if (proc != null) {
                        dto.setStatus(proc.getStatus());         // READY / IN_PROGRESS / DONE
                        dto.setStartTime(proc.getStartTime());   // 시작시간
                        dto.setEndTime(proc.getEndTime());       // 종료시간
                        dto.setGoodQty(proc.getGoodQty());       // 양품 수량
                        dto.setDefectQty(proc.getDefectQty());   // 불량 수량
                        dto.setMemo(proc.getMemo());             // 메모

                    } else {
                        // 아직 진행되지 않은 공정
                        dto.setStatus("READY");
                        dto.setStartTime(null);
                        dto.setEndTime(null);
                        dto.setGoodQty(null);
                        dto.setDefectQty(null);
                        dto.setMemo(null);
                    }

                    // ----------------------------
                    // (3) 기준값(standardQty) 계산
                    // ----------------------------
                    Double standardQty = null;
                    String processId = dto.getProcessId();

                    if ("PRC-BLD".equals(processId)) {
                        // 1단계 블렌딩: 기준 배합량(총량) 계산
                        standardQty = calculateBlendStandardQty(workOrder);

                    } else if ("PRC-FLT".equals(processId)) {
                        // 2단계 여과: 기준 배합량 유지 (현재는 손실 고려 X)
                        standardQty = calculateFilterStandardQty(workOrder);

                    } else {
                        // 3단계 이후 공정: 기준 완제품 수량(이론) = 계획 수량
                        //  - 이유: 이 단계에서는 배합량이 아니라 개수 기준으로 판단
                        if (planQty != null) {
                            standardQty = planQty.doubleValue();
                        }
                    }

                    dto.setStandardQty(standardQty);
                    
                    // ----------------------------
                    // (4) 예상 소요시간/지연 여부 계산
                    // ----------------------------
                    // 공정별 예상시간(분)
                    long expectedMin = ProcessTimeCalculator.calcExpectedMinutes(processId, totalEU);
                    dto.setExpectedMinutes(expectedMin);

                    // 지연 여부: 시작된 공정만 판단
                    // - proc/startTime 없으면 false
                    boolean delayed = ProcessTimeCalculator.isDelayed(proc, totalEU);

                    dto.setDelayed(delayed);

                    return dto;
                })
                .collect(Collectors.toList());
        
	     // standardQty를 "이전 공정 양품" 기준으로 재설정
	     for (int i = 1; i < stepDTOs.size(); i++) {
	
	         WorkOrderProcessStepDTO curr = stepDTOs.get(i);
	         WorkOrderProcessStepDTO prev = stepDTOs.get(i - 1);
	
	         // 여과(PRC-FLT)만: 1단계 배합량(ml)을 이어받아 표준값 표시
	         if ("PRC-FLT".equals(curr.getProcessId())) {

        	    String prevStatus = (prev.getStatus() == null) ? "" : prev.getStatus().trim().toUpperCase();
        	    Integer prevGood  = prev.getGoodQty();

        	    if ("DONE".equals(prevStatus) && prevGood != null) {
        	        curr.setStandardQty(prevGood.doubleValue());   // 1단계 양품으로!
        	    } else {
        	        curr.setStandardQty(prev.getStandardQty());    // fallback: 1단계 기준배합량
        	    }
        	    continue;
        	}
	     }

        // 2) 공정 시작/종료 버튼 활성화 여부 계산
        for (int i = 0; i < stepDTOs.size(); i++) {

            WorkOrderProcessStepDTO dto = stepDTOs.get(i);
            WorkOrderProcessStepDTO prevDto = (i > 0) ? stepDTOs.get(i - 1) : null;

            // 이전 단계가 DONE이어야 다음 단계 시작 가능
            boolean prevDone = (i == 0) || "DONE".equals(prevDto.getStatus());

            // 이전 단계가 QC_PENDING이면 이후 진행 불가
            boolean isPrevBlocking = (prevDto != null) && "QC_PENDING".equals(prevDto.getStatus());

            // 라벨링 단계(PRC-LBL)는 반드시 QC PASS 이후 시작 가능
            if ("PRC-LBL".equals(dto.getProcessId())) {
                dto.setCanStart("READY".equals(dto.getStatus()) && isQcPassed);

            } else {
                // 일반 공정 시작 조건
                dto.setCanStart("READY".equals(dto.getStatus()) && prevDone && !isPrevBlocking);
            }
            // 진행중(IN_PROGRESS) 상태일 때만 "종료" 버튼 활성화
            dto.setCanFinish("IN_PROGRESS".equals(dto.getStatus()));
        }

        return stepDTOs;
    }
    
    /**
     * 블렌딩 공정 기준 배합량 (이론값) 계산
     * - BOM_MST + 작업지시 계획수량(planQty) 기준
     * - 원자재(RAW)만 합산
     */
    private Double calculateBlendStandardQty(WorkOrder workOrder) {

        String prdId  = workOrder.getProduct().getPrdId();
        Integer planQty = workOrder.getPlanQty();

        // BOM + 필요수량 조회 (지금 handleLotOnFirstStepStart 에서 쓰는 쿼리 그대로 사용)
        List<MaterialAvailabilityDTO> materials =
                orderMapper.selectMaterials(prdId, planQty);

        double totalRequiredQty = materials.stream()
                .filter(m -> "RAW".equals(m.getMatType()))        // 원자재만
                .mapToDouble(MaterialAvailabilityDTO::getRequiredQty)
                .sum();

        return totalRequiredQty;
    }

    /**
     * 여과 공정 기준 배합량
     * - 일단 블렌딩 기준 배합량과 동일하게 사용 (이론상 손실 없다고 가정)
     * - 나중에 여과 손실률 반영하고 싶으면 여기서만 수정하면 됨
     */
    private Double calculateFilterStandardQty(WorkOrder workOrder) {
        return calculateBlendStandardQty(workOrder);
    }

    // =========================================================================
    // 3. 공정 단계 시작
    @Transactional
    public WorkOrderProcessStepDTO startStep(String orderId, Integer stepSeq) {

        WorkOrderProcess proc = workOrderProcessRepository
                .findByWorkOrderOrderIdAndStepSeq(orderId, stepSeq)
                .orElseThrow(() -> new IllegalArgumentException("해당 공정 단계가 존재하지 않습니다."));

        if (!"READY".equals(proc.getStatus())) {
            throw new IllegalStateException("대기 상태(READY)인 공정만 시작할 수 있습니다.");
        }

        WorkOrder workOrder = proc.getWorkOrder();
        
        // LOT 처리 공통 진입
        handleLotOnStepStart(workOrder, proc);
        
        // 공정 상태
        proc.setStatus("IN_PROGRESS");
        proc.setStartTime(LocalDateTime.now());

        // 작업지시 시작일/상태 변경
        // 최초 시작일자 기록 (이미 값 있으면 유지)
        if (workOrder.getActStartDate() == null) {
            workOrder.setActStartDate(LocalDateTime.now());
        }
        if ("RELEASED".equals(workOrder.getStatus())) {
        	workOrder.setStatus("IN_PROGRESS");
        }
        
        workOrderRepository.save(workOrder);
        WorkOrderProcess saved = workOrderProcessRepository.save(proc);
        return toStepDTO(saved);
    }
    
    /**
     * 공정 단계 시작 시 LOT 처리
     * - 1단계: WIP LOT 생성 + CREATE, PROC_START
     * - 2단계 이후: 기존 LOT 재사용 + PROC_START만
     * - 여과/충전 등: LOCATION 변경이 필요하면 여기서 같이 처리
     */
    private void handleLotOnStepStart(WorkOrder workOrder, WorkOrderProcess proc) {

        String orderId  = workOrder.getOrderId();
        String lineCode = workOrder.getLine().getLineId();
        Integer stepSeq = proc.getStepSeq();
        String processId = proc.getProcess().getProcessId();

        String lotNo;

        // ----------------- 1단계: 지금 만든 메서드 그대로 사용 -----------------
        if (stepSeq == 1) {
            handleLotOnFirstStepStart(workOrder, proc);
            return;   // 이 안에서 lotNo, history까지 다 처리했으므로 끝
        }

        // ----------------- 2단계 이후: 첫 공정의 LOT_NO 재사용 -----------------
        // (1단계 WOP에서 lotNo 가져오기)
        WorkOrderProcess firstProc =
                workOrderProcessRepository.findByWorkOrderOrderIdAndStepSeq(orderId, 1)
                        .orElseThrow(() -> new IllegalStateException("1단계 공정 정보 없음"));

        lotNo = firstProc.getLotNo();
        proc.setLotNo(lotNo); // 혹시 비어있다면 세팅

        // LOT_HISTORY : PROC_START 공통 등록
        LotHistoryDTO hist = new LotHistoryDTO();
        hist.setLotNo(lotNo);
        hist.setOrderId(orderId);
        hist.setProcessId(processId);
        hist.setEventType("PROC_START");    // LOT_EVENT_TYPE
        hist.setStatus("IN_PROCESS");       // LOT_STATUS
        hist.setLocationType("LINE");       // 필요하면 공정별로 변경
        hist.setLocationId(lineCode);
        hist.setQuantity(workOrder.getPlanQty());
        hist.setStartTime(LocalDateTime.now());
        hist.setWorkedId(workOrder.getCreatedEmp().getEmpId());

        lotTraceService.registLotHistory(hist);

        // 여과/충전 시작 시 LOC만 바뀌는 경우가 있으면 processId로 분기해서 처리
        // if ("PRC-FLT".equals(processId)) { ... }
    }


	/**
     * 블렌딩 1단계 시작 시
     * - LOT_MASTER : WIP LOT 생성
	 * - LOT_HISTORY : CREATE, PROC_START
	 * - LOT_RELATIONSHIP : 원자재 LOT 사용 관계 생성
     */
    private void handleLotOnFirstStepStart(WorkOrder workOrder, WorkOrderProcess proc) {

        String orderId  = workOrder.getOrderId();
        String lineCode = workOrder.getLine().getLineId();
        
        // ==========================
        // BOM 기반 원자재 필요량 계산
        // ==========================
        String prdId = workOrder.getProduct().getPrdId();
        Integer planQty = workOrder.getPlanQty();

        // Mapper에서 BOM + 재고까지 계산
        List<MaterialAvailabilityDTO> materials =
                orderMapper.selectMaterials(prdId, planQty);

        double totalRequiredQty = materials.stream()
        		.filter(m -> "RAW".equals(m.getMatType()))
        		.mapToDouble(MaterialAvailabilityDTO::getRequiredQty)
                .sum();

        // 필요 원자재 부피 메모
        String formatted = String.format("%,.0f", totalRequiredQty);
        String memo = "필요 원자재 부피 합계: " + formatted + "ml (단위: BOM 기준)";
        String originMemo = proc.getMemo();
        proc.setMemo((originMemo == null ? "" : originMemo + "\n") + memo);


        // -----------------------------
        // 1) LOT_MASTER : WIP LOT 생성
        // -----------------------------
        LotMasterDTO lotMasterDTO = LotMasterDTO.builder()
                .lotType("WIP")                         // 공정용 LOT
                .orderId(orderId)
                .prdId(workOrder.getProduct().getPrdId())
                .quantity(workOrder.getPlanQty())
                .currentStatus("IN_PROCESS")           // LOT_STATUS
                .currentLocType("LINE")                // LOCATION_TYPE
                .currentLocId(lineCode)
                .statusChangeDate(LocalDateTime.now())
                .build();

        // LOT_MASTER INSERT + LOT_NO 생성
        String lotNo = lotTraceService.registLotMaster(lotMasterDTO, lineCode);
        proc.setLotNo(lotNo);

        // -----------------------------
        // 2) LOT_HISTORY : CREATE
        // -----------------------------
        LotHistoryDTO createHist = new LotHistoryDTO();
        createHist.setLotNo(lotNo);
        createHist.setOrderId(orderId);
        createHist.setEventType("CREATE");            // LOT_EVENT_TYPE
        createHist.setStatus("NEW");                  // LOT_STATUS
        createHist.setLocationType("LINE");
        createHist.setLocationId(lineCode);
        createHist.setQuantity(workOrder.getPlanQty());
        createHist.setWorkedId(workOrder.getCreatedEmp().getEmpId());

        lotTraceService.registLotHistory(createHist);

        // -----------------------------
        // 3) LOT_HISTORY : PROC_START
        // -----------------------------
        LotHistoryDTO procStartHist = new LotHistoryDTO();
        procStartHist.setLotNo(lotNo);
        procStartHist.setOrderId(orderId);
        procStartHist.setProcessId(proc.getProcess().getProcessId()); // PRC-BLD
        procStartHist.setEventType("PROC_START");
        procStartHist.setStatus("IN_PROCESS");
        procStartHist.setLocationType("LINE");
        procStartHist.setLocationId(lineCode);
        procStartHist.setQuantity(workOrder.getPlanQty());
        procStartHist.setStartTime(LocalDateTime.now());
        procStartHist.setWorkedId(workOrder.getCreatedEmp().getEmpId());

        lotTraceService.registLotHistory(procStartHist);

        // -----------------------------
        // 4) LOT_RELATIONSHIP 생성
        // -----------------------------
        createLotRelationshipForOrder(orderId, lotNo);
    }

    /**
     * 해당 작업지시(orderId)로 출고된 원자재 LOT들을 조회하여
     * LOT_RELATIONSHIP(OUTPUT_LOT = 생산 LOT, INPUT_LOT = 원자재 LOT)을 생성
     */
    private void createLotRelationshipForOrder(String orderId, String outputLotNo) {

        // 1) 생산 LOT 조회
        LotMaster outputLot = lotMasterRepository.findByLotNo(outputLotNo)
                .orElseThrow(() -> new IllegalArgumentException("생산 LOT 없음: " + outputLotNo));

        // 2) 이 작업지시에 출고된 자재 전체 조회 (RAW, SUB, PKG 다 포함)
        List<String> materialTypes = List.of("RAW", "SUB", "PKG");

        List<OutboundItem> items =
                outboundItemRepository.findByOutbound_WorkOrderIdAndItemTypeIn(orderId, materialTypes);

        // 3) LOT별 사용 수량 합산 (같은 LOT이 여러 번 출고된 경우)
        Map<String, Long> usedQtyByLot = items.stream()
                .collect(Collectors.groupingBy(
                        OutboundItem::getLotNo,
                        Collectors.summingLong(OutboundItem::getOutboundAmount)
                ));

        // 4) LOT 관계 생성
        for (Map.Entry<String, Long> entry : usedQtyByLot.entrySet()) {

            String inputLotNo = entry.getKey();
            long usedQty = entry.getValue();

            LotMaster inputLot = lotMasterRepository.findByLotNo(inputLotNo)
                    .orElseThrow(() -> new IllegalArgumentException("원자재 LOT 없음: " + inputLotNo));

            LotRelationship rel = new LotRelationship();
            rel.setOutputLot(outputLot);                 // 완제품 LOT (부모)
            rel.setInputLot(inputLot);                   // 투입 LOT (자식)
            rel.setUsedQty((int) usedQty);               // 여러 번 출고된 건도 합산된 수량으로 저장

            lotRelationshipRepository.save(rel);
        }
    }


	/**
     * 공정 단계 종료 처리
     * - IN_PROGRESS → DONE or QC_PENDING
     * - 마지막 단계 완료 시 WORK_ORDER 상태 COMPLETED + ACT_END_DATE 설정
     */
    @Transactional
    public WorkOrderProcessStepDTO finishStep(String orderId, Integer stepSeq,
    										  Integer goodQty, Integer defectQty, String memo) {

    	// 작업지시 공정 단계 조회
        WorkOrderProcess proc = workOrderProcessRepository
                .findByWorkOrderOrderIdAndStepSeq(orderId, stepSeq)
                .orElseThrow(() -> new IllegalArgumentException("해당 공정 단계가 존재하지 않습니다."));

        // 진행 중인 공정만 종료 가능
        if (!"IN_PROGRESS".equals(proc.getStatus())) {
            throw new IllegalStateException("진행중(IN_PROGRESS) 상태인 공정만 종료할 수 있습니다.");
        }

        String processId = proc.getProcess().getProcessId();
        
        // 수량 정보 기록
        proc.setGoodQty(goodQty);
        proc.setDefectQty(defectQty);
        
        // 공정 상태 종료 처리
        proc.setStatus("DONE");
        proc.setEndTime(LocalDateTime.now());

        // 캡/펌프 공정인 경우에만 QC_RESULT PENDING 생성
        if ("PRC-CAP".equals(processId)) {
        	
        	// 1) QC_RESULT PENDING 생성
            qcResultService.createPendingQcResultForOrder(orderId);
            
            // 2) QC 공정 WOP 상태를 QC_PENDING으로 전환
            WorkOrderProcess qcProc = workOrderProcessRepository
                    .findByWorkOrderOrderIdAndProcessProcessId(orderId, "PRC-QC")
                    .orElseThrow(() -> new IllegalStateException("QC 공정 단계가 없습니다. orderId=" + orderId));
            
            // QC 공정이 READY일 때만 QC_PENDING으로 바꿔줌
            if ("READY".equals(qcProc.getStatus())) {
                qcProc.setStatus("QC_PENDING");
                workOrderProcessRepository.save(qcProc);
            }
            
            // 해당 공정 종료 시 QC 알림
            String message = "새로 등록된 QC 검사가 있습니다.";
            alarmService.sendAlarmMessage(AlarmDestination.QC, message);
        }

        // 마지막 공정 여부 판단
        WorkOrder workOrder = proc.getWorkOrder();
        boolean hasLaterStep =
                workOrderProcessRepository.existsByWorkOrderOrderIdAndStepSeqGreaterThan(orderId, stepSeq);

        // 마지막 공정이면 작업지시 종료 처리
        if (!hasLaterStep) {

            // 1) 작업지시 완료 처리
            workOrder.setStatus("COMPLETED");
            workOrder.setActEndDate(LocalDateTime.now());

            String planId = workOrder.getPlanId();
            
            // 2) 생산계획 완료 여부 판단
            if (planId != null) {

                // (1) 생산계획 조회 (목표수량/상태 변경 위해 필요)
                ProductionPlan plan = productionPlanRepository.findById(planId)
                        .orElseThrow(() -> new IllegalStateException("생산계획을 찾을 수 없습니다. planId=" + planId));

                // (2) 진행 중 작업지시 존재 여부 (CREATED/RELEASED가 남아 있으면 아직 작업 남음)
                boolean existsActiveWo = workOrderRepository.existsByPlanIdAndStatusIn(
                        planId, List.of("CREATED", "RELEASED")
                );

                // (3) 목표수량(계획수량) - DB: PRODUCTION_PLAN.PLAN_QTY
                int targetQty = plan.getPlanQty();

                // (4) 마지막 공정(PRC-LBL) DONE 양품수량 합 (계획 내 실제 완제품 생산량)
                int producedGoodQty =
                        workOrderProcessRepository.sumLastStepGoodQtyByPlanId(planId, "PRC-LBL");

                // (5) 계획 완료 조건:
                // - 진행 중 작업지시 없고
                // - 마지막 공정 양품 합이 목표수량 이상이면 DONE
                boolean isPlanDone = (!existsActiveWo) && (producedGoodQty >= targetQty);

                if (isPlanDone) {
                    plan.setStatus(ProductionStatus.DONE);

                    // 생산계획 아이템도 DONE 처리
                    List<ProductionPlanItem> items = productionPlanItemRepository.findByPlanId(planId);
                    for (ProductionPlanItem item : items) {
                        item.setStatus(ProductionStatus.DONE);
                    }

                    productionPlanRepository.save(plan);
                    productionPlanItemRepository.saveAll(items);

                } else {
                    // 목표 미달 or 진행중 WO 존재 -> 생산중 유지 (SCRAPPED 있어도 여기로 올 수 있음)
                    plan.setStatus(ProductionStatus.IN_PROGRESS);
                    productionPlanRepository.save(plan);
                }
            }


        }
        
        // LOT 종료 공통 처리
        handleLotOnStepEnd(workOrder, proc, hasLaterStep);
        
     	// 포장 공정 + 마지막 단계일 때만 완제품 입고대기 생성
        if (!hasLaterStep && "PRC-LBL".equals(processId)) {
            inboundService.saveProductInbound(proc.getWopId());
        }

        workOrderRepository.save(workOrder);
        WorkOrderProcess saved = workOrderProcessRepository.save(proc);
        return toStepDTO(saved);
    }
    
    /**
     * 공정 단계 종료 시 LOT 처리
     * - 모든 공정: PROC_END 한 건 기록
     * - 마지막 공정(포장 완료): LOT_MASTER.STATUS = PROD_DONE 로 변경
     */
    private void handleLotOnStepEnd(WorkOrder workOrder,
                                    WorkOrderProcess proc,
                                    boolean hasLaterStep) {

        String lotNo = proc.getLotNo();
        if (lotNo == null) {
            // 방어 코드: 이 경우는 원래 나오면 안 됨
            return;
        }

        String processId = proc.getProcess().getProcessId();

        // 1) LOT_HISTORY : PROC_END 기록
        LotHistoryDTO hist = new LotHistoryDTO();
        hist.setLotNo(lotNo);
        hist.setOrderId(workOrder.getOrderId());
        hist.setProcessId(processId);
        hist.setEventType("PROC_END");
        hist.setStatus(hasLaterStep ? "IN_PROCESS" : "PROD_DONE"); // 마지막이면 생산완료
        hist.setLocationType("LINE");
        hist.setLocationId(workOrder.getLine().getLineId());
        hist.setQuantity(workOrder.getPlanQty());
        hist.setEndTime(LocalDateTime.now());
        hist.setWorkedId(workOrder.getCreatedEmp().getEmpId());

        lotTraceService.registLotHistory(hist);

        // 2) 마지막 공정이면 LOT_MASTER 상태도 PROD_DONE 으로
        if (!hasLaterStep) {
            LotMaster lot = lotMasterRepository.findByLotNo(lotNo)
                    .orElseThrow(() -> new IllegalArgumentException("LOT_MASTER 없음: " + lotNo));
            lot.setCurrentStatus("PROD_DONE"); // LOT_STATUS 테이블 참조
            lot.setStatusChangeDate(LocalDateTime.now());
            
            // WIP -> FIN 변경
            if ("WIP".equals(lot.getLotType())) {
                lot.setLotType("FIN");
            }
        }
    }


    // =========================================================================
    // 4. 공정 메모
    // =========================================================================
    @Transactional
    public WorkOrderProcessStepDTO updateStepMemo(String orderId, Integer stepSeq, String memo) {

        WorkOrderProcess proc = workOrderProcessRepository
                .findByWorkOrderOrderIdAndStepSeq(orderId, stepSeq)
                .orElseThrow(() -> new IllegalArgumentException("해당 공정 단계가 존재하지 않습니다."));

        proc.setMemo(memo);

        WorkOrderProcess saved = workOrderProcessRepository.save(proc);
        return toStepDTO(saved);
    }

    /**
     * 엔티티 -> 단계 DTO 변환 (start/finish/메모 응답용)
     */
    private WorkOrderProcessStepDTO toStepDTO(WorkOrderProcess proc) {
        WorkOrderProcessStepDTO dto = new WorkOrderProcessStepDTO();
        dto.setOrderId(proc.getWorkOrder().getOrderId());
        dto.setStepSeq(proc.getStepSeq());
        dto.setProcessId(proc.getProcess().getProcessId());
        dto.setProcessName(proc.getProcess().getProcessName());
        dto.setStatus(proc.getStatus());
        dto.setStartTime(proc.getStartTime());
        dto.setEndTime(proc.getEndTime());
        dto.setGoodQty(proc.getGoodQty());
        dto.setDefectQty(proc.getDefectQty());
        dto.setMemo(proc.getMemo());

        dto.setCanStart("READY".equals(proc.getStatus()));
        dto.setCanFinish("IN_PROGRESS".equals(proc.getStatus()));

        return dto;
    }

    // 공정 관리 -> 완료 처리부분
    @Transactional(readOnly = true)
    public List<WorkOrderProcessDTO> getWorkOrderListForDone(LocalDate workDate, String keyword, String status) {

        // 완료/폐기 탭 대상
        List<String> statuses = List.of("COMPLETED", "SCRAPPED");
        
        if (status != null && !status.isBlank()) {
            statuses = statuses.stream()
                    .filter(s -> s.equalsIgnoreCase(status))
                    .toList();
        }

        if (statuses.isEmpty()) return List.of();

        List<WorkOrder> workOrders =
                workOrderRepository.findByStatusInAndOutboundYn(statuses, "Y");

        if (workOrders.isEmpty()) return List.of();

        if (workDate != null) {
            workOrders = workOrders.stream()
                    .filter(w -> w.getActEndDate() != null
                            && w.getActEndDate().toLocalDate().equals(workDate))
                    .toList();
        }
        if (workOrders.isEmpty()) return List.of();

        // 최근 완료/폐기 우선
        workOrders = workOrders.stream()
                .sorted(Comparator
                        .comparing(WorkOrder::getActEndDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(WorkOrder::getOrderId))
                .toList();

        List<String> orderIds = workOrders.stream().map(WorkOrder::getOrderId).toList();

        List<WorkOrderProcess> allProcesses =
                workOrderProcessRepository
                        .findByWorkOrderOrderIdInOrderByWorkOrderOrderIdAscStepSeqAsc(orderIds);

        Map<String, List<WorkOrderProcess>> processMap = allProcesses.stream()
                .collect(Collectors.groupingBy(p -> p.getWorkOrder().getOrderId()));

        List<QcResult> allQcResults = qcResultRepository.findByOrderIdIn(orderIds);
        Map<String, QcResult> qcMap = allQcResults.stream()
                .collect(Collectors.toMap(QcResult::getOrderId, q -> q, (a, b) -> a));

        List<WorkOrderProcessDTO> dtoList = workOrders.stream()
                .map(w -> toProcessSummaryDto(
                        w,
                        processMap.getOrDefault(w.getOrderId(), List.of()),
                        qcMap.get(w.getOrderId())
                ))
                .toList();

        // 키워드 검색
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            dtoList = dtoList.stream()
                    .filter(dto ->
                            (dto.getOrderId() != null && dto.getOrderId().toLowerCase().contains(kw)) ||
                            (dto.getPrdId() != null   && dto.getPrdId().toLowerCase().contains(kw)) ||
                            (dto.getPrdName() != null && dto.getPrdName().toLowerCase().contains(kw))
                    )
                    .toList();
        }

        return dtoList;
    }


}
