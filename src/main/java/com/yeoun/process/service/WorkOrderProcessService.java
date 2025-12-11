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
import com.yeoun.order.entity.WorkOrder;
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
    @Transactional(readOnly = true)
    public List<WorkOrderProcessDTO> getWorkOrderListForStatus() {
        // 기존에 쓰이던 기본 버전
        // => "검색조건 없음"으로 호출
        return getWorkOrderListForStatus(null, null, null, null);
    }
    
    // 1. 공정현황 메인 목록
    @Transactional(readOnly = true)
    public List<WorkOrderProcessDTO> getWorkOrderListForStatus(LocalDate workDate, String processName, String status, String keyword) {

        // 1) 공정현황 대상이 되는 작업지시 조회 (RELEASE, IN_PROGRESS)
        List<String> statuses = List.of("RELEASED", "IN_PROGRESS");
        List<WorkOrder> workOrders = workOrderRepository.findByStatusInAndOutboundYn(statuses, "Y");

        if (workOrders.isEmpty()) {
            return List.of();
        }
        
        // 날짜 필터: 작성일자(createdDate)를 "작업지시일자"로 사용
        if (workDate != null) {
            workOrders = workOrders.stream()
                    .filter(w -> w.getCreatedDate() != null &&
                                 w.getCreatedDate().toLocalDate().equals(workDate))
                    .collect(Collectors.toList());
        }
        
        // 상태 필터: 셀렉트에서 넘어온 status 값과 동일한 것만
        if (status != null && !status.isBlank()) {
            workOrders = workOrders.stream()
                    .filter(w -> status.equals(w.getStatus()))
                    .collect(Collectors.toList());
        }
        
        if (workOrders.isEmpty()) {
            return List.of();
        }

        // 작업지시번호 리스트 추출
        // 2) 정렬 (상태 우선순위 + 작업지시번호)
        workOrders.sort(
        	    Comparator.comparing((WorkOrder w) -> statusPriority(w.getStatus()))
        	              .thenComparing(WorkOrder::getOrderId)
    	);

        // 3) 작업지시번호 리스트 추출
        List<String> orderIds = workOrders.stream()
                .map(WorkOrder::getOrderId)
                .toList();

        // 4) 모든 공정 데이터를 한 번에 조회 (orderId + stepSeq 순)
        List<WorkOrderProcess> allProcesses =
                workOrderProcessRepository.findByWorkOrderOrderIdInOrderByWorkOrderOrderIdAscStepSeqAsc(orderIds);

        // orderId -> 공정 리스트 맵핑
        Map<String, List<WorkOrderProcess>> processMap = allProcesses.stream()
                .collect(Collectors.groupingBy(p -> p.getWorkOrder().getOrderId()));

        // 5) 모든 QC 결과를 한 번에 조회
        List<QcResult> allQcResults = qcResultRepository.findByOrderIdIn(orderIds);

        Map<String, QcResult> qcMap = allQcResults.stream()
                .collect(Collectors.toMap(
                        QcResult::getOrderId,
                        qc -> qc,
                        (q1, q2) -> q1 // 중복 시 첫 번째 사용
                ));

        // 6) 각 작업지시를 공정현황 DTO로 변환
        List<WorkOrderProcessDTO> dtoList = workOrders.stream()
                .map(w -> {
                    List<WorkOrderProcess> processes =
                            processMap.getOrDefault(w.getOrderId(), List.of());
                    QcResult qcResult = qcMap.get(w.getOrderId());
                    return toProcessSummaryDto(w, processes, qcResult);
                })
                .collect(Collectors.toList());
        
        // 7) 현재공정 필터 (DTO 단)
        if (processName != null && !processName.isBlank()) {
            dtoList = dtoList.stream()
                    .filter(dto -> processName.equals(dto.getCurrentProcess()))
                    .toList();
        }
        
        // 8) 검색어 필터 (작업지시번호 / 제품ID / 제품명)
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();

            dtoList = dtoList.stream()
                    .filter(dto ->
                            (dto.getOrderId() != null &&
                             dto.getOrderId().toLowerCase().contains(kw))
                         || (dto.getPrdId() != null &&
                             dto.getPrdId().toLowerCase().contains(kw))
                         || (dto.getPrdName() != null &&
                             dto.getPrdName().toLowerCase().contains(kw))
                    )
                    .toList();
        }

        return dtoList;
    }
    
    private int statusPriority(String status) {
        return switch (status) {
            case "IN_PROGRESS" -> 1;
            case "RELEASED"    -> 2;
            default            -> 3;
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
        if (qcResult != null && qcResult.getGoodQty() != null) { // 필드명 맞게 수정
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

        int progressRate = calculateProgressRate(processes);
        String currentProcess = resolveCurrentProcess(processes);
        String elapsedTime = calculateElapsedTime(processes);

        WorkOrderProcessDTO dto = new WorkOrderProcessDTO();
        dto.setOrderId(workOrder.getOrderId());
        dto.setPrdId(workOrder.getProduct().getPrdId());
        dto.setPrdName(workOrder.getProduct().getPrdName());
        dto.setPlanQty(workOrder.getPlanQty());
        dto.setStatus(workOrder.getStatus());
        
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
     * 진행률 계산 (DONE + QC_PENDING 단계 수 / 전체 단계 수 * 100)
     */
    private int calculateProgressRate(List<WorkOrderProcess> processes) {
        int totalSteps = processes.size();
        if (totalSteps == 0) {
            return 0;
        }

        long doneCount = processes.stream()
                .filter(p -> "DONE".equals(p.getStatus()) || "QC_PENDING".equals(p.getStatus()))
                .count();

        return (int) Math.round(doneCount * 100.0 / totalSteps);
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

        // IN_PROGRESS 공정 우선
        WorkOrderProcess inProgress = processes.stream()
                .filter(p -> "IN_PROGRESS".equals(p.getStatus()))
                .findFirst()
                .orElse(null);

        if (inProgress != null) {
            return inProgress.getProcess().getProcessName();
        }

        // READY 중 가장 stepSeq가 작은 공정
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
     * 경과시간 계산 (첫 START_TIME ~ 현재)
     */
    private String calculateElapsedTime(List<WorkOrderProcess> processes) {
        LocalDateTime firstStart = processes.stream()
                .map(WorkOrderProcess::getStartTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        if (firstStart == null) {
            return "-";
        }

        Duration d = Duration.between(firstStart, LocalDateTime.now());
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

        // QC 결과 PASS 여부 (포장 공정 시작 조건)
        boolean isQcPassed = qcResultRepository.existsByOrderIdAndOverallResult(orderId, "PASS");

        // 6) 공정 단계 DTO 리스트 + 버튼 플래그 세팅
        List<WorkOrderProcessStepDTO> stepDTOs = buildStepDtos(steps, processMap, isQcPassed);

        return new WorkOrderProcessDetailDTO(headerDto, stepDTOs);
    }

    private List<WorkOrderProcessStepDTO> buildStepDtos(
            List<RouteStep> steps,
            Map<String, WorkOrderProcess> processMap,
            boolean isQcPassed
    ) {

        List<WorkOrderProcessStepDTO> stepDTOs = steps.stream()
                .map(step -> {
                    WorkOrderProcessStepDTO dto = new WorkOrderProcessStepDTO();
                    dto.setStepSeq(step.getStepSeq());
                    dto.setProcessId(step.getProcess().getProcessId());
                    dto.setProcessName(step.getProcess().getProcessName());

                    WorkOrderProcess proc = processMap.get(step.getRouteStepId());

                    if (proc != null) {
                        dto.setStatus(proc.getStatus());
                        dto.setStartTime(proc.getStartTime());
                        dto.setEndTime(proc.getEndTime());
                        dto.setGoodQty(proc.getGoodQty());
                        dto.setDefectQty(proc.getDefectQty());
                        dto.setMemo(proc.getMemo());
                    } else {
                        dto.setStatus("READY");
                        dto.setStartTime(null);
                        dto.setEndTime(null);
                        dto.setGoodQty(null);
                        dto.setDefectQty(null);
                        dto.setMemo(null);
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        // 버튼 활성화 플래그 계산
        for (int i = 0; i < stepDTOs.size(); i++) {
            WorkOrderProcessStepDTO dto = stepDTOs.get(i);
            WorkOrderProcessStepDTO prevDto = (i > 0) ? stepDTOs.get(i - 1) : null;

            boolean prevDone = (i == 0) || "DONE".equals(prevDto.getStatus());
            boolean isPrevBlocking = (prevDto != null) && "QC_PENDING".equals(prevDto.getStatus());

            // 포장 공정은 QC PASS 필요
            if ("PRC-PACK".equals(dto.getProcessId())) {
                dto.setCanStart("READY".equals(dto.getStatus()) && isQcPassed);
            } else {
                dto.setCanStart("READY".equals(dto.getStatus()) && prevDone && !isPrevBlocking);
            }

            dto.setCanFinish("IN_PROGRESS".equals(dto.getStatus()));
        }

        return stepDTOs;
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

        // ★ 여과/충전 시작 시 LOC만 바뀌는 경우가 있으면 processId로 분기해서 처리
        // if ("PRC-FLTR".equals(processId)) { ... }
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

        // (옵션) WOP에 lotNo 저장하고 싶으면
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
    public WorkOrderProcessStepDTO finishStep(String orderId, Integer stepSeq) {

        WorkOrderProcess proc = workOrderProcessRepository
                .findByWorkOrderOrderIdAndStepSeq(orderId, stepSeq)
                .orElseThrow(() -> new IllegalArgumentException("해당 공정 단계가 존재하지 않습니다."));

        if (!"IN_PROGRESS".equals(proc.getStatus())) {
            throw new IllegalStateException("진행중(IN_PROGRESS) 상태인 공정만 종료할 수 있습니다.");
        }

        String processId = proc.getProcess().getProcessId();
        
        // 1) 공정 상태는 항상 DONE 으로
        proc.setStatus("DONE");
        proc.setEndTime(LocalDateTime.now());

        // 2) 캡/펌프 공정인 경우에만 QC_RESULT PENDING 생성
        if ("PRC-CAP".equals(processId)) {
            qcResultService.createPendingQcResultForOrder(orderId);
        }

        // 3) 마지막 단계인지 확인
        WorkOrder workOrder = proc.getWorkOrder();
        boolean hasLaterStep =
                workOrderProcessRepository.existsByWorkOrderOrderIdAndStepSeqGreaterThan(orderId, stepSeq);

        if (!hasLaterStep) {
            // 1) 작업지시 완료 처리
            workOrder.setStatus("COMPLETED");
            workOrder.setActEndDate(LocalDateTime.now());

            String planId = workOrder.getPlanId();
            if (planId != null) {

                // 🔹 같은 PLAN_ID 아래에 아직 COMPLETED 아닌 작업지시가 있는지 확인
                boolean existsNotCompletedWo =
                        workOrderRepository.existsByPlanIdAndStatusNot(planId, "COMPLETED");

                if (!existsNotCompletedWo) {
                    // (1) 생산계획 헤더 DONE
                    ProductionPlan plan = productionPlanRepository.findById(planId)
                            .orElseThrow(() -> new IllegalStateException("생산계획을 찾을 수 없습니다. planId=" + planId));
                    plan.setStatus(ProductionStatus.DONE);

                    // (2) 해당 계획의 PlanItem들도 전부 DONE으로 덮어쓰기
                    List<ProductionPlanItem> items =
                            productionPlanItemRepository.findByPlanId(planId);
                    for (ProductionPlanItem item : items) {
                        item.setStatus(ProductionStatus.DONE);
                    }
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
            
            // WIP → FIN 변경
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

}
