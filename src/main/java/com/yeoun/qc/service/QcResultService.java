package com.yeoun.qc.service;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.yeoun.common.dto.FileAttachDTO;
import com.yeoun.common.entity.Dispose;
import com.yeoun.common.entity.FileAttach;
import com.yeoun.common.repository.DisposeRepository;
import com.yeoun.common.repository.FileAttachRepository;
import com.yeoun.common.util.FileUtil;
import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.inbound.service.InboundService;
import com.yeoun.lot.dto.LotHistoryDTO;
import com.yeoun.lot.entity.LotMaster;
import com.yeoun.lot.repository.LotMasterRepository;
import com.yeoun.lot.service.LotTraceService;
import com.yeoun.masterData.entity.QcItem;
import com.yeoun.masterData.repository.QcItemRepository;
import com.yeoun.order.entity.WorkOrder;
import com.yeoun.order.repository.WorkOrderRepository;
import com.yeoun.outbound.entity.Outbound;
import com.yeoun.outbound.entity.OutboundItem;
import com.yeoun.outbound.repository.OutboundRepository;
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

import jakarta.persistence.EntityNotFoundException;
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
    
    // QC 실패 시
    private final InboundService inboundService;
    private final OutboundRepository outboundRepository;
    private final DisposeRepository disposeRepository;
    
    // LOT 연동용
    private final LotTraceService lotTraceService;
    private final LotMasterRepository lotMasterRepository;
    
    // 파일
    private final FileUtil fileUtil;
    private final FileAttachRepository fileAttachRepository;
    
    // ===================================================
	private static final String QC_PROCESS_ID = "PRC-QC"; 
	private static final String STATUS_READY   = "READY";
	private static final String STATUS_SKIPPED = "SKIPPED";
    
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
				
				qdrDTO.setMinValue(item.getMinValue());
			    qdrDTO.setMaxValue(item.getMaxValue());
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
	/**
	 * QC 검사 시작 처리
	 * - QC는 별도 화면에서 진행되므로, '검사 시작' 클릭 시점을 QC 공정 시작으로 기록한다.
	 * - QC_PENDING/READY 상태일 때만 IN_PROGRESS로 전환하고 start_time을 세팅한다.
	 */
	@Transactional
	public void startQc(String orderId) {

	    WorkOrderProcess qcProc = workOrderProcessRepository
	            .findByWorkOrderOrderIdAndProcessProcessId(orderId, "PRC-QC")
	            .orElseThrow(() -> new IllegalStateException("QC 공정 단계가 없습니다. orderId=" + orderId));

	    // 이미 완료된 QC는 재시작 불가(필요하면 정책 변경)
	    if ("DONE".equals(qcProc.getStatus())) {
	        throw new IllegalStateException("이미 완료된 QC입니다.");
	    }

	    // QC_PENDING(대기) 또는 READY에서만 시작 가능
	    if (!"QC_PENDING".equals(qcProc.getStatus()) && !"READY".equals(qcProc.getStatus())) {
	        return; // 이미 IN_PROGRESS면 그냥 무시(중복 클릭 방지)
	    }

	    qcProc.setStatus("IN_PROGRESS");
	    qcProc.setStartTime(LocalDateTime.now());
	    workOrderProcessRepository.save(qcProc);
	}

	/**
	 * QC 검사 중단 처리(저장 없이 닫기)
	 * - IN_PROGRESS 상태에서만 QC_PENDING으로 되돌리고 start_time을 초기화한다.
	 * - 다음에 다시 검사 시작 시 새로운 start_time이 찍히도록 하기 위함.
	 */
	@Transactional
	public void cancelQc(String orderId) {

	    WorkOrderProcess qcProc = workOrderProcessRepository
	            .findByWorkOrderOrderIdAndProcessProcessId(orderId, "PRC-QC")
	            .orElseThrow(() -> new IllegalStateException("QC 공정 단계가 없습니다. orderId=" + orderId));

	    if ("IN_PROGRESS".equals(qcProc.getStatus())) {
	        qcProc.setStatus("QC_PENDING");  // 또는 READY (너희 정책대로)
	        qcProc.setStartTime(null);
	        workOrderProcessRepository.save(qcProc);
	    }
	}

	// ----------------------------------------------------------
	// QC 결과 목록 조회
	public List<QcResultListDTO> getQcResultListForView(String startDate, String endDate, String keyword, String result) {
		
		LocalDate start = (startDate == null || startDate.isBlank()) ? null : LocalDate.parse(startDate);
	    LocalDate end   = (endDate   == null || endDate.isBlank())   ? null : LocalDate.parse(endDate);

	    String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toUpperCase();
		
		return qcResultRepository.findResultListForView(start, end, kw, result);
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
	        
	        // 해당 QC 항목 조회 (min/max 기준 확인용)
	        QcItem item = qcItemRepository.findById(detail.getQcItemId())
	                .orElse(null);

	        // 2-2) 모달에서 입력한 값 반영
	        detail.setMeasureValue(row.getMeasureValue()); // 측정값
	        
	        // 기본은 사용자가 선택한 결과
	        String result = row.getResult();
	        
	        // min/max 기준 있는 항목이면 자동판정
	        if (item != null && (item.getMinValue() != null || item.getMaxValue() != null)) {

	            String mv = row.getMeasureValue();

	            if (mv != null && !mv.isBlank()) {
	                try {
	                    BigDecimal value = new BigDecimal(mv);

	                    boolean pass = true;

	                    if (item.getMinValue() != null &&
	                            value.compareTo(item.getMinValue()) < 0) {
	                        pass = false;
	                    }
	                    if (item.getMaxValue() != null &&
	                            value.compareTo(item.getMaxValue()) > 0) {
	                        pass = false;
	                    }

	                    result = pass ? "PASS" : "FAIL";

	                } catch (NumberFormatException e) {
	                    result = "FAIL";
	                }
	            } else {
	                result = "FAIL";
	            }
	        }
	        
	        detail.setResult(result);
	        detail.setRemark(row.getRemark());             // 비고
	        detail.setUpdatedUser(loginEmpId);

	        // 2-3) 전체 판정 계산용 체크
	        if ("FAIL".equalsIgnoreCase(result)) {
	            allPass = false;
	        }

	        // 2-4) 상세 저장
	        qcResultDetailRepository.save(detail);
	    }
		
		// 검사자 / 검사일자 확정
		if (loginEmpId != null && (header.getInspectorId() == null || header.getInspectorId().isBlank())) {
		    header.setInspectorId(loginEmpId);
		}

		if (header.getInspectionDate() == null) {
		    header.setInspectionDate(LocalDate.now());
		}
		
		// 3) 수동 전체판정
		// 사용자가 선택한 전체 판정 우선 적용
		String overall = qcSaveRequestDTO.getOverallResult();

		// 유효성 방어 (PASS/FAIL만 허용)
		if (!"PASS".equalsIgnoreCase(overall) && !"FAIL".equalsIgnoreCase(overall)) {
		    throw new IllegalArgumentException("전체 판정이 올바르지 않습니다.");
		}

		header.setOverallResult(overall.toUpperCase());
	    
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
	                
	                // 시작시간 방어 처리
	                if (qcProc.getStartTime() == null) {
	                    qcProc.setStartTime(LocalDateTime.now());
	                }

	                // QC 종료시간 + 상태 확정
	                qcProc.setStatus("DONE");

	                if (qcProc.getEndTime() == null) {
	                    qcProc.setEndTime(LocalDateTime.now());
	                }
	                
	                workOrderProcessRepository.save(qcProc);
	            });

	    // LOT / WORK_ORDER 연동
	    applyQcResultToLotAndWorkOrder(header);
	}
	
	// ----------------------------------------------------------
	// QC 결과에 따라 LOT_MASTER + LOT_HISTORY + WORK_ORDER 상태 반영
	// - LOT_HISTORY.EVENT_TYPE = QC_RESULT
	// - LOT_HISTORY.STATUS     = LOT_STATUS(IN_PROCESS / SCRAPPED 등)
	// ----------------------------------------------------------
	@Transactional
	private void applyQcResultToLotAndWorkOrder(QcResult header) {

	    String orderId = header.getOrderId();
	    String result  = header.getOverallResult();   // PASS / FAIL

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
	        qcResultRepository.save(header);
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
	        }
	        case "FAIL" -> {
	            // FAIL: 불량/폐기 처리
	            lot.setCurrentStatus("SCRAPPED");    // LOT_STATUS
	            lot.setStatusChangeDate(LocalDateTime.now());
	            
	            workOrder.setStatus("SCRAPPED");
	            workOrder.setActEndDate(LocalDateTime.now());
	            
	            // QC FAIL이면 이후 공정 단계는 중단 처리
	            skipAfterQcSteps(orderId);
	            
	            // 사용 원자재 폐기
	            disposeMaterialByQcFail(orderId, header.getInspectorId());
	            
	            inboundService.saveReInbound(orderId);
	        }
	        default -> {
	            // PENDING 등은 상태 변경 없음
	        }
	    }
	    lotMasterRepository.save(lot);
	    workOrderRepository.save(workOrder);
	}
	
	// QC 결과 → LOT_STATUS 매핑
	private String mapQcResultToLotStatus(String result) {
	    return switch (result) {
	        case "PASS" -> "IN_PROCESS";  // 계속 공정 진행
	        case "FAIL" -> "SCRAPPED";    // 폐기
	        default -> "IN_PROCESS";
	    };
	}
	// QC 실패 시 QC 이후 단계(포장 등) 상태 SKIPPED 처리
	private void skipAfterQcSteps(String orderId) {

	    // 1) QC 공정 stepSeq 조회 (하드코딩 제거)
		WorkOrderProcess qcProc = workOrderProcessRepository
	            .findByWorkOrderOrderIdAndProcessProcessId(orderId, QC_PROCESS_ID)
	            .orElseThrow(() -> new IllegalStateException("QC 공정 없음: " + orderId));

		int qcStepSeq = qcProc.getStepSeq();

	    // 2) QC 이후 단계 조회
		List<WorkOrderProcess> afterSteps =
	            workOrderProcessRepository.findByWorkOrderOrderIdAndStepSeqGreaterThan(orderId, qcStepSeq);


	    // 3) READY인 단계만 SKIPPED로 정리
		for (WorkOrderProcess p : afterSteps) {
		    if (STATUS_READY.equals(p.getStatus())) {
		        p.setStatus(STATUS_SKIPPED);
		        p.setEndTime(LocalDateTime.now());
		    }
		}
		workOrderProcessRepository.saveAll(afterSteps);
	}
	
	// QC 실패 시 사용 자재 폐기 (RAW / SUB 타입 자재를 자동으로 폐기)
	@Transactional
	public void disposeMaterialByQcFail(String orderId, String empId) {
		
		// 1. 작업지시 기준 출고 정보 조회
		Outbound outbound = outboundRepository.findByWorkOrderId(orderId)
		        .orElseThrow(() -> new IllegalStateException("출고 정보 없음: " + orderId));
		
		// 2. 출고된 자재 목록 반복 처리
		for (OutboundItem item : outbound.getItems()) {
			
			// RAW / SUB 자재만 폐기 대상
			if (!"RAW".equals(item.getItemType()) && !"SUB".equals(item.getItemType())) {
				continue;
			}
			
			// 중복 방지 (이미 QC_FAIL 폐기 이력 있으면 skip)
	        boolean alreadyDisposed =
	            disposeRepository.existsByWorkTypeAndLotNo("QC_FAIL", item.getLotNo());

	        if (alreadyDisposed) {
	            continue;
	        }
			
			// 폐기 이력만 기록 (재고 차감 X)
	        Dispose dispose = Dispose.builder()
        		.lotNo(item.getLotNo())
                .itemId(item.getItemId())
                .workType("QC_FAIL")
                .empId(empId)
                .disposeAmount(item.getOutboundAmount())
                .disposeReason("QC 검사 불합격")
                .createdDate(LocalDateTime.now())
	            .build();
	        
	        disposeRepository.save(dispose);

		}
	}


	// -----------------------------------------------------------------------------------------------
	// QC 상세 항목별 첨부파일 저장
    @Transactional
    public void saveQcDetailFiles(String qcResultDtlId, List<MultipartFile> files) throws IOException {

        if (files == null || files.isEmpty()) {
            return;
        }

        // 1) 상세 엔티티 조회
        QcResultDetail detail = qcResultDetailRepository.findById(qcResultDtlId)
                .orElseThrow(() -> new EntityNotFoundException("QC 상세가 존재하지 않습니다. ID=" + qcResultDtlId));

        // 2) FileUtil로 실제 파일 업로드 (공지와 동일)
        List<FileAttach> fileList = fileUtil.uploadFile(detail, files)
                .stream()
                .map(FileAttachDTO::toEntity)
                .toList();

        // 3) FILE_ATTACH DB 저장
        fileAttachRepository.saveAll(fileList);
    }

    // QC 상세 항목별 첨부파일 조회
    @Transactional
    public List<FileAttachDTO> getQcDetailFiles(String qcResultDtlId) {
    	
    	Long fileRefId = QcFileKeyUtil.toFileRefId(qcResultDtlId);

        // REF_TABLE, REF_ID 기준으로 FILE_ATTACH 조회
        List<FileAttach> fileList =
                fileAttachRepository.findByRefTableAndRefId("QC_RESULT_DETAIL", fileRefId);

        return fileList.stream()
                .map(FileAttachDTO::fromEntity)
                .toList();
    }
    
    // String -> Long
    public class QcFileKeyUtil {

        // "QCD-0012-003" -> 12003 이런 식으로 변환
        public static Long toFileRefId(String qcResultDtlId) {
            if (qcResultDtlId == null || qcResultDtlId.isBlank()) {
                return null;
            }

            // "QCD-0012-003" 기준
            String[] parts = qcResultDtlId.split("-");
            if (parts.length != 3) {
                throw new IllegalArgumentException("QC_RESULT_DTL_ID 형식 오류: " + qcResultDtlId);
            }

            long header = Long.parseLong(parts[1]); // 0012 -> 12
            long seq    = Long.parseLong(parts[2]); // 003  -> 3

            // 헤더당 최대 999개 항목까지 커버 
            return header * 1000L + seq;
        }
    }




    
    
    
    
    
    
    
    
    
    
    
    

} // QcResultService 끝
