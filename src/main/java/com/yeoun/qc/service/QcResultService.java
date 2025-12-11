package com.yeoun.qc.service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.lot.dto.LotHistoryDTO;
import com.yeoun.lot.entity.LotMaster;
import com.yeoun.lot.repository.LotMasterRepository;
import com.yeoun.lot.service.LotTraceService;
import com.yeoun.masterData.entity.QcItem;
import com.yeoun.masterData.repository.QcItemRepository;
import com.yeoun.order.entity.WorkOrder;
import com.yeoun.order.repository.WorkOrderRepository;
import com.yeoun.process.entity.WorkOrderProcess;
import com.yeoun.process.repository.WorkOrderProcessRepository;
import com.yeoun.qc.dto.QcDetailRowDTO;
import com.yeoun.qc.dto.QcRegistDTO;
import com.yeoun.qc.dto.QcResultListDTO;
import com.yeoun.qc.dto.QcResultViewDTO;
import com.yeoun.qc.dto.QcSaveRequestDTO;
import com.yeoun.qc.entity.QcResult;
import com.yeoun.qc.entity.QcResultDetail;
import com.yeoun.qc.repository.QcResultDetailRepository;
import com.yeoun.qc.repository.QcResultRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QcResultService {
	
	private final QcResultRepository qcResultRepository;
    private final WorkOrderRepository workOrderRepository;
    private final QcItemRepository qcItemRepository;
    private final QcResultDetailRepository qcResultDetailRepository;
    private final WorkOrderProcessRepository workOrderProcessRepository;
    private final EmpRepository empRepository;
    
    // LOT 연동용
    private final LotTraceService lotTraceService;
    private final LotMasterRepository lotMasterRepository;
    
    // --------------------------------------------------------------
    // 캡/펌프 공정 종료 시 호출되는 QC 결과 생성 메서드
    // - 이미 해당 작업지시의 QC_RESULT가 있으면 재생성하지 않고 그대로 반환
    // - 없으면 "검사대기(PENDING)" 상태의 헤더(및 필요시 디테일) 생성
    public QcResult createPendingQcResultForOrder(String orderId) {
    	
    	// 이미 생성된 QC 결과가 있으면 재사용
    	return qcResultRepository.findByOrderId(orderId)
    			.orElseGet(() -> createNewPendingQcResult(orderId));
    }
    
    // 새로운 QC_RESULT(+DETAIL)을 생성하는 메서드
    private QcResult createNewPendingQcResult(String orderId) {
    	
    	// 로그인한 직원 ID 가져오기
    	Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loginEmpId = null;
        
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            loginEmpId = authentication.getName();
        }
    	
        // 1) 작업지시 조회
        WorkOrder workOrder = workOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 작업지시: " + orderId));

        // 2) QC_RESULT 헤더 엔티티 생성
        QcResult qc = new QcResult();

        qc.setOrderId(orderId);                         

        // LOT 번호가 이미 있다면 세팅 (없으면 null/나중에 업데이트)
        // qc.setLotNo(workOrder.getLotNo());

        // 검사 수량: 일단 계획수량 기준으로 세팅 (나중에 필요시 수정 가능)
        qc.setInspectionQty(workOrder.getPlanQty());

        // 검사일시는 아직 미정 ->  QC 완료 시점에 세팅
        qc.setInspectionDate(null);

        // 초기 결과 상태: PENDING (또는 null)
        qc.setOverallResult("PENDING");

        // 초기 양품/불량 수량은 0으로
        qc.setGoodQty(0);
        qc.setDefectQty(0);

        // 비고/실패사유 등은 아직 없음
        qc.setFailReason(null);
        qc.setRemark("자동생성 - 캡/펌프 공정 종료에 따른 QC 대기");

        // 등록자
        qc.setCreatedId(loginEmpId);
        
        // 3) 헤더 저장
        QcResult savedHeader = qcResultRepository.save(qc);
        
        // 4) QC_RESULT_DETAIL 자동 생성
        createEmptyQcDetails(savedHeader, workOrder);

    	return savedHeader;
    }
    
    // QC_RESULT_DETAIL 자동 생성
    private void createEmptyQcDetails(QcResult qcHeader, WorkOrder workOrder) {

        // 0) 로그인한 직원 ID
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loginEmpId = null;

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            loginEmpId = authentication.getName();
        }

        // 1) QC 항목 마스터 조회
        List<QcItem> qcItems =
                qcItemRepository.findByTargetTypeAndUseYnOrderBySortOrderAsc("FINISHED_QC", "Y");

        int seq = 1;

        for (QcItem item : qcItems) {

            QcResultDetail detail = new QcResultDetail();

            // (1) 상세 PK 생성 방식 (예시)
            // QCD-<QC_RESULT_ID>-001 형식
            String dtlId = String.format("QCD-%04d-%03d",
                    qcHeader.getQcResultId(),  // Long
                    seq++);

            detail.setQcResultDtlId(dtlId);

            // (2) 헤더 ID (Detail은 String이라 변환)
            detail.setQcResultId(String.valueOf(qcHeader.getQcResultId()));

            // (3) QC 항목ID
            detail.setQcItemId(item.getQcItemId());

            // (4) 측정값/판정/비고 초기화
            detail.setMeasureValue(null);   // 아직 미측정
            detail.setResult(null);         // PASS/FAIL은 나중에
            detail.setRemark(null);

            // (5) 등록자
            detail.setCreatedUser(loginEmpId);

            qcResultDetailRepository.save(detail);
        }
    }


    // -------------------------------------------------------------
    // QC 등록 목록
    public List<QcRegistDTO> getQcResultListForRegist() {
        return qcResultRepository.findRegistListByStatus("PENDING");
    }

    // QC 등록 모달 목록
	public List<QcDetailRowDTO> getDetailRows(Long qcResultId) {
		
		// QC 결과 ID 조회
		List<QcResultDetail> details = 
				qcResultDetailRepository.findByQcResultId(String.valueOf(qcResultId));
		
		List<QcDetailRowDTO> qcDetailRowDTO = new ArrayList<>();
		
		for (QcResultDetail d : details) {
			
			QcItem item = qcItemRepository.findById(d.getQcItemId())
					.orElse(null);
			
			QcDetailRowDTO qdrDTO = new QcDetailRowDTO();
			
			qdrDTO.setQcResultDtlId(d.getQcResultDtlId());
			qdrDTO.setQcItemId(d.getQcItemId());
			
			if(item != null) {
				qdrDTO.setItemName(item.getItemName());
				qdrDTO.setUnit(item.getUnit());
				qdrDTO.setStdText(buildStdText(item));
			}
			
			qdrDTO.setMeasureValue(d.getMeasureValue());
			qdrDTO.setResult(d.getResult());
			qdrDTO.setRemark(d.getRemark());
			
			qcDetailRowDTO.add(qdrDTO);
			
		}
		
		return qcDetailRowDTO;
	}
	
	// 기준값 문자열 메서드
	private String buildStdText(QcItem item) {
		
		if (item == null) return "";
		
		// 설명형 기준값이 있으면 우선
		if (item.getStdText() != null && !item.getStdText().isBlank()) {
			return item.getStdText();
		}
		
		// 수지형 기준값(min/max) 사용
		BigDecimal min = item.getMinValue();
		BigDecimal max = item.getMaxValue();
		
		if (min != null && max != null) {
			return min.stripTrailingZeros().toPlainString()
					+ " ~ "
					+ max.stripTrailingZeros().toPlainString();
		} else if (min != null) {
			return "≥ " + min.stripTrailingZeros().toPlainString();
		} else if (max != null) {
			return "≤ " + max.stripTrailingZeros().toPlainString();
		}
		
		// 정말 아무것도 없을 경우
		return "";
	}

	// ----------------------------------------------------------
	// QC 결과 목록 조회
	public List<QcResultListDTO> getQcResultListForView() {
		return qcResultRepository.findResultListForView();
	}

	// ----------------------------------------------------------
	// QC 결과 저장 (등록 모달에서 입력한 값 반영)
	@Transactional
	public void saveQcResult(Long qcResultId, QcSaveRequestDTO qcSaveRequestDTO) {
		
		List<QcDetailRowDTO> detailRows = qcSaveRequestDTO.getDetailRows();
		
		// 0) 로그인한 직원 ID (검사자 / 수정자 공통)
	    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	    String loginEmpId = null;

	    if (authentication != null && authentication.isAuthenticated()
	            && !"anonymousUser".equals(authentication.getPrincipal())) {
	        loginEmpId = authentication.getName();
	    }
		
		// 1) QC 결과 헤더 조회 (없으면 예외)
		QcResult header = qcResultRepository.findById(qcResultId)
	            .orElseThrow(() -> new IllegalArgumentException("QC 결과가 존재하지 않습니다. ID = " + qcResultId));
		
		// 2) 상세 항목 반복 처리
		boolean allPass = true;   // 전체 판정 계산용
		
		for (QcDetailRowDTO row : detailRows) {

	        // 2-1) 상세 엔티티 조회 (pk: qcResultDtlId)
	        QcResultDetail detail = qcResultDetailRepository.findById(row.getQcResultDtlId())
	                .orElseThrow(() -> new IllegalArgumentException(
	                        "QC 상세가 존재하지 않습니다. ID = " + row.getQcResultDtlId()));

	        // 2-2) 모달에서 입력한 값 반영
	        detail.setMeasureValue(row.getMeasureValue()); // 측정값
	        detail.setResult(row.getResult());             // PASS / FAIL
	        detail.setRemark(row.getRemark());             // 비고

	        // 2-3) 수정자 
	        detail.setUpdatedUser(loginEmpId);

	        // 2-3) 전체 판정 계산용 체크
	        if (row.getResult() != null && row.getResult().equalsIgnoreCase("FAIL")) {
	            allPass = false;
	        }

	        // 2-4) 상세 저장
	        qcResultDetailRepository.save(detail);
	    }
		
		// 3) 전체 판정 계산
		header.setOverallResult(allPass ? "PASS" : "FAIL");
	    header.setInspectionDate(LocalDate.now());
	    header.setInspectorId(loginEmpId);  // 실제 검사한 사람
	    header.setUpdatedId(loginEmpId);    // 수정자(최종 저장한 사람)
	    
	    // 수량/사유/비고는 요청 DTO에서 그대로 사용
	    if (qcSaveRequestDTO.getGoodQty() != null) {
	        header.setGoodQty(qcSaveRequestDTO.getGoodQty());
	    }
	    if (qcSaveRequestDTO.getDefectQty() != null) {
	        header.setDefectQty(qcSaveRequestDTO.getDefectQty());
	    }

	    // 검사 수량은 good+defect로 자동 계산
	    if (header.getGoodQty() != null && header.getDefectQty() != null) {
	        header.setInspectionQty(header.getGoodQty() + header.getDefectQty());
	    }

	    if (qcSaveRequestDTO.getFailReason() != null && !qcSaveRequestDTO.getFailReason().isBlank()) {
	        header.setFailReason(qcSaveRequestDTO.getFailReason());
	    }
	    if (qcSaveRequestDTO.getRemark() != null && !qcSaveRequestDTO.getRemark().isBlank()) {
	        header.setRemark(qcSaveRequestDTO.getRemark());
	    }

	    qcResultRepository.save(header);
	    
	    String orderId = header.getOrderId();
	    
	    // 3-1) QC 공정(WOP)에 양품/불량 수량 + 상태 반영
	    workOrderProcessRepository
	            .findByWorkOrderOrderIdAndProcessProcessId(orderId, "PRC-QC")
	            .ifPresent(qcProc -> {
	                // 양품/불량 수량 반영
	                qcProc.setGoodQty(header.getGoodQty());
	                qcProc.setDefectQty(header.getDefectQty());

	                // 상태/종료시간도 같이 정리 (기존 4) 로직 통합)
	                if (!"DONE".equals(qcProc.getStatus())) {
	                    qcProc.setStatus("DONE");
	                }
	                if (qcProc.getEndTime() == null) {
	                    qcProc.setEndTime(LocalDateTime.now());
	                }

	                workOrderProcessRepository.save(qcProc);
	            });

	    // 3-2) 마지막 공정(WOP)에도 양품/불량 수량 복사
	    //      -> 포장 완료 시 saveProductInbound()에서 사용
	    List<WorkOrderProcess> allSteps =
	            workOrderProcessRepository.findByWorkOrderOrderIdOrderByStepSeqAsc(orderId);

	    if (!allSteps.isEmpty()) {
	        WorkOrderProcess lastStep = allSteps.get(allSteps.size() - 1);
	        lastStep.setGoodQty(header.getGoodQty());
	        lastStep.setDefectQty(header.getDefectQty());
	        workOrderProcessRepository.save(lastStep);
	    }

	    // LOT / WORK_ORDER 연동
	    applyQcResultToLotAndWorkOrder(header);

	}
	
	// ----------------------------------------------------------
	// QC 결과에 따라 LOT_MASTER + LOT_HISTORY + WORK_ORDER 상태 반영
	// - LOT_HISTORY.EVENT_TYPE = QC_RESULT
	// - LOT_HISTORY.STATUS     = LOT_STATUS(IN_PROCESS / SCRAPPED 등)
	// - 필요 시 WORK_ORDER.STATUS = QC_HOLD / QC_FAIL 등으로 변경
	// ----------------------------------------------------------
	@Transactional
	private void applyQcResultToLotAndWorkOrder(QcResult header) {

	    String orderId = header.getOrderId();
	    String result  = header.getOverallResult();   // PASS / FAIL / PENDING / HOLD 등

	    // 1) 작업지시 조회
	    WorkOrder workOrder = workOrderRepository.findById(orderId)
	            .orElseThrow(() -> new IllegalArgumentException("작업지시 없음: " + orderId));

	    // 2) 이 작업지시의 생산 LOT_NO (1단계 WOP 기준)
	    //    - 공정 서비스에서 1단계 시작 시 lotNo를 WOP에 세팅했으므로 그대로 사용
	    var firstProc = workOrderProcessRepository
	            .findByWorkOrderOrderIdAndStepSeq(orderId, 1)
	            .orElseThrow(() -> new IllegalStateException("1단계 공정 정보 없음: " + orderId));

	    String lotNo = firstProc.getLotNo();
	    if (lotNo == null || lotNo.isBlank()) {
	        // 이론상 나오면 안 되지만 방어코드
	        return;
	    }

	    // QC_RESULT에 LOT_NO가 비어있으면 같이 세팅해 두기 (조회용)
	    if (header.getLotNo() == null || header.getLotNo().isBlank()) {
	        header.setLotNo(lotNo);
	    }

	    // 3) LOT_MASTER 조회
	    LotMaster lot = lotMasterRepository.findByLotNo(lotNo)
	            .orElseThrow(() -> new IllegalArgumentException("LOT_MASTER 없음: " + lotNo));

	    // 4) LOT_HISTORY : QC_RESULT 이벤트 1건 기록
	    LotHistoryDTO hist = new LotHistoryDTO();
	    hist.setLotNo(lotNo);
	    hist.setOrderId(orderId);
	    hist.setEventType("QC_RESULT");                // LOT_EVENT_TYPE
	    hist.setStatus(mapQcResultToLotStatus(result)); // LOT_STATUS
	    hist.setLocationType("LINE");                  // 검사 위치(생산라인 기준)
	    hist.setLocationId(workOrder.getLine().getLineId());

	    // 검사 수량 기준으로 기록 (양품+불량이 더 정확하면 그걸로 써도 됨)
	    Integer qty = header.getInspectionQty();
	    if (qty == null && header.getGoodQty() != null && header.getDefectQty() != null) {
	        qty = header.getGoodQty() + header.getDefectQty();
	    }
	    hist.setQuantity(qty);

	    // 검사자/검사시간
	    hist.setWorkedId(header.getInspectorId());
	    hist.setEndTime(LocalDateTime.now());

	    lotTraceService.registLotHistory(hist);

	    // 5) LOT_MASTER 및 WORK_ORDER 상태 반영
	    switch (result) {
	        case "PASS" -> {
	            // PASS: 일단 계속 공정 진행 예정이므로 IN_PROCESS 유지
	            lot.setCurrentStatus("IN_PROCESS");
	            lot.setStatusChangeDate(LocalDateTime.now());
	            // 작업지시 상태는 기존 공정 로직에서 마지막 공정 완료 시 COMPLETED 처리
	        }
	        case "HOLD" -> {
	            // HOLD: LOT은 물리적으로 그대로지만, 작업지시는 QC_HOLD 상태로 묶어두기
	            lot.setCurrentStatus("IN_PROCESS");
	            lot.setStatusChangeDate(LocalDateTime.now());
	            workOrder.setStatus("QC_HOLD");  // ⚠️ 새 코드이니 코드테이블 쓰면 같이 추가 필요
	        }
	        case "FAIL" -> {
	            // FAIL: 불량/폐기 처리
	            lot.setCurrentStatus("SCRAPPED");    // LOT_STATUS
	            lot.setStatusChangeDate(LocalDateTime.now());
	            workOrder.setStatus("QC_FAIL");      // ⚠️ 마찬가지로 새 상태코드
	        }
	        default -> {
	            // PENDING 등은 상태 변경 없음
	        }
	    }
	}
	
	// QC 결과 → LOT_STATUS 매핑
	private String mapQcResultToLotStatus(String result) {
	    return switch (result) {
	        case "PASS" -> "IN_PROCESS";  // 계속 공정 진행
	        case "HOLD" -> "IN_PROCESS";  // 대기지만 LOT 입장에서는 생산중 상태로 유지
	        case "FAIL" -> "SCRAPPED";    // 폐기
	        default -> "IN_PROCESS";
	    };
	}

	// ----------------------------------------------------------
	// QC 결과 상세 조회 (결과 보기 모달용)
	// - 헤더 + 디테일 정보를 한 번에 DTO로 반환
	@Transactional(readOnly = true)
	public QcResultViewDTO getQcResultView(Long qcResultId) {

	    // 1) 헤더 조회
	    QcResult header = qcResultRepository.findById(qcResultId)
	            .orElseThrow(() -> new IllegalArgumentException(
	                    "QC 결과가 존재하지 않습니다. ID = " + qcResultId));

	    // 2) 작업지시 조회 (제품/수량용)
	    WorkOrder workOrder = workOrderRepository.findById(header.getOrderId())
	            .orElse(null);

	    // 3) 디테일 목록 조회
	    List<QcResultDetail> details =
	            qcResultDetailRepository.findByQcResultId(String.valueOf(qcResultId));

	    // 4) 디테일 → QcDetailRowDTO 변환
	    List<QcDetailRowDTO> detailDtos = new ArrayList<>();

	    for (QcResultDetail d : details) {

	        QcItem item = qcItemRepository.findById(d.getQcItemId())
	                .orElse(null);

	        QcDetailRowDTO dto = new QcDetailRowDTO();
	        dto.setQcResultDtlId(d.getQcResultDtlId());
	        dto.setQcItemId(d.getQcItemId());

	        if (item != null) {
	            dto.setItemName(item.getItemName());
	            dto.setUnit(item.getUnit());
	            dto.setStdText(buildStdText(item));
	        }

	        dto.setMeasureValue(d.getMeasureValue());
	        dto.setResult(d.getResult());
	        dto.setRemark(d.getRemark());

	        detailDtos.add(dto);
	    }

	    // 5) 헤더 + 디테일 합쳐서 ViewDTO 세팅
	    QcResultViewDTO view = new QcResultViewDTO();
	    view.setQcResultId(qcResultId);
	    view.setOrderId(header.getOrderId());
	    view.setLotNo(header.getLotNo());
	    view.setInspectionDate(header.getInspectionDate());
	    view.setOverallResult(header.getOverallResult());
	    view.setFailReason(header.getFailReason());
	    view.setInspectionQty(header.getInspectionQty());
	    view.setGoodQty(header.getGoodQty());
	    view.setDefectQty(header.getDefectQty());

	    // 검사자 정보
	    view.setInspectorId(header.getInspectorId());
	    if (header.getInspectorId() != null) {
	        empRepository.findById(header.getInspectorId())
	                .ifPresent(emp -> view.setInspectorName(emp.getEmpName()));
	    }

	    // 작업지시(제품) 정보
	    if (workOrder != null) {
	        view.setPlanQty(workOrder.getPlanQty());
	        if (workOrder.getProduct() != null) {
	            view.setProductCode(workOrder.getProduct().getPrdId());
	            view.setProductName(workOrder.getProduct().getPrdName());
	        }
	    }

	    view.setDetails(detailDtos);

	    return view;
	}


    
    
    
    
    
    
    
    
    
    
    
    

} // QcResultService 끝
