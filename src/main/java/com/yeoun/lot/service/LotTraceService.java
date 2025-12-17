package com.yeoun.lot.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeoun.emp.entity.Dept;
import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.equipment.entity.ProdEquip;
import com.yeoun.equipment.repository.ProdEquipRepository;
import com.yeoun.inbound.entity.InboundItem;
import com.yeoun.inbound.repository.InboundItemRepository;
import com.yeoun.inventory.entity.Inventory;
import com.yeoun.inventory.entity.MaterialOrder;
import com.yeoun.inventory.repository.InventoryRepository;
import com.yeoun.inventory.repository.MaterialOrderRepository;
import com.yeoun.lot.constant.LotStatus;
import com.yeoun.lot.dto.LotEquipInfoDTO;
import com.yeoun.lot.dto.LotHistoryDTO;
import com.yeoun.lot.dto.LotMasterDTO;
import com.yeoun.lot.dto.LotMaterialDetailDTO;
import com.yeoun.lot.dto.LotMaterialNodeDTO;
import com.yeoun.lot.dto.LotProcessDetailDTO;
import com.yeoun.lot.dto.LotProcessNodeDTO;
import com.yeoun.lot.dto.LotRootDTO;
import com.yeoun.lot.dto.LotRootDetailDTO;
import com.yeoun.lot.dto.LotUsedProductNodeDTO;
import com.yeoun.lot.entity.LotHistory;
import com.yeoun.lot.entity.LotMaster;
import com.yeoun.lot.entity.LotRelationship;
import com.yeoun.lot.repository.LotHistoryRepository;
import com.yeoun.lot.repository.LotMasterRepository;
import com.yeoun.lot.repository.LotRelationshipRepository;
import com.yeoun.equipment.entity.Equipment;
import com.yeoun.masterData.entity.MaterialMst;
import com.yeoun.masterData.entity.ProcessMst;
import com.yeoun.equipment.entity.ProdLine;
import com.yeoun.masterData.entity.ProductMst;
import com.yeoun.masterData.repository.ProcessMstRepository;
import com.yeoun.order.entity.WorkOrder;
import com.yeoun.order.entity.WorkerProcess;
import com.yeoun.order.repository.WorkOrderRepository;
import com.yeoun.order.repository.WorkerProcessRepository;
import com.yeoun.process.constant.ProcessStepStatus;
import com.yeoun.process.entity.WorkOrderProcess;
import com.yeoun.process.repository.WorkOrderProcessRepository;
import com.yeoun.qc.entity.QcResult;
import com.yeoun.qc.repository.QcResultRepository;
import com.yeoun.sales.entity.Client;
import com.yeoun.sales.repository.ClientRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class LotTraceService {
	private final LotMasterRepository lotMasterRepository;
	private final LotHistoryRepository historyRepository;
	private final LotRelationshipRepository lotRelationshipRepository;
	private final ProcessMstRepository processMstRepository;
	private final EmpRepository empRepository;
	private final WorkOrderProcessRepository workOrderProcessRepository; 
	private final WorkOrderRepository workOrderRepository;
	private final WorkerProcessRepository workerProcessRepository;
	private final InboundItemRepository inboundItemRepository;
	private final InventoryRepository inventoryRepository;
	private final MaterialOrderRepository materialOrderRepository;
	private final ClientRepository clientRepository;
	private final ProdEquipRepository prodEquipRepository;
	private final QcResultRepository qcResultRepository;

	// ----------------------------------------------------------------------------
	// LOT 생성
	@Transactional
	public String registLotMaster(LotMasterDTO lotMasterDTO, String line) {
		
		// Lot번호 생성
		String LotNo = generateLotId(lotMasterDTO.getLotType(), lotMasterDTO.getPrdId(), line, LocalDate.now());
		
		lotMasterDTO.setLotNo(LotNo);
		
		LotMaster lotMaster = lotMasterDTO.toEntity();
		
		lotMasterRepository.save(lotMaster);
				
		return LotNo;
	}
	
	//Lot 이력 생성
	@Transactional
	public void registLotHistory(LotHistoryDTO historyDTO) {
		LotMaster lot = lotMasterRepository.findByLotNo(historyDTO.getLotNo())
				.orElseThrow(() -> new NoSuchElementException("LOT 없음"));
		
		// 엔티티로 변환
		LotHistory lotHistory = historyDTO.toEntity();
		
		lotHistory.setLot(lot);
		
		// 공정 정보 조회 후 엔티티에 주입
		if (historyDTO.getProcessId() != null) {
			ProcessMst processMst = processMstRepository.findByProcessId(historyDTO.getProcessId())
					.orElse(null);	
			lotHistory.setProcess(processMst);
		}
		
		// 직원 조회 후 엔티티에 주입
		if (historyDTO.getWorkedId() != null) {
			Emp emp = empRepository.findByEmpId(historyDTO.getWorkedId())
					.orElseThrow(() -> new NoSuchElementException("직원 없음"));
			lotHistory.setWorker(emp);
		}
		
		historyRepository.save(lotHistory);
	}
	
	// LOT번호 생성
	// 형식: [LOT유형]-[제품코드5자리]-[YYYYMMDD]-[라인]-[시퀀스3자리]
	public String generateLotId(String lotType, String prdId, String line, LocalDate date) {
		String dateStr  = date.format(DateTimeFormatter.BASIC_ISO_DATE); 
		
		// 최근 시퀀스 조회
		String lastSeq = lotMasterRepository.findLastSeq(lotType, prdId, dateStr, line);
		
		// 마지막 시퀀스 값이 없을 경우 1로 설정 값이 있으면 1씩 증가
		int next = (lastSeq == null) ? 1 : Integer.parseInt(lastSeq) + 1;
		// 시퀀스가 3자리 만들기
		String nextSeq = String.format("%03d", next);
		
		return String.format(
			"%s-%s-%s-%s-%s",	
			lotType,
			prdId,
			dateStr,
			line,
			nextSeq
		);
	}

	// ================================================================================
	// [ROOT - 완제품 LOT]
	// ================================================================================
	// 완제품 ROOT 목록
	@Transactional(readOnly = true)
	public List<LotRootDTO> getFinishedLots() {
	    return getFinishedLots(null, null, null);
	}
	
	@Transactional(readOnly = true)
	public List<LotRootDTO> getFinishedLots(String keyword, String status, String type) {
		
		// 완제품 공정에 속한 LOT 타입
		List<String> lotTypes = List.of("WIP", "FIN");
		
		// 1) FIN WIP LOT 목록 조회
		List<LotMaster> lots = 
				lotMasterRepository.findRootLotsOrdered(lotTypes);
		
		final String kw = (keyword == null) ? "" : keyword.trim();
		final String st = (status == null) ? "" : status.trim();
		final String tp = (type == null) ? "" : type.trim();
		
		return lots.stream()
		        .filter(lm -> {
		            if (kw.isEmpty()) return true;
		            return (lm.getLotNo() != null && lm.getLotNo().contains(kw))
		                || (lm.getDisplayName() != null && lm.getDisplayName().contains(kw));
		        })
		        .filter(lm -> {
		            if (st.isEmpty()) return true;
		            return st.equals(lm.getCurrentStatus());
		        })
		        .filter(lm -> {
		            if (tp.isEmpty()) return true;
		            return tp.equals(lm.getLotType());
		        })
		        .map(lm -> {
		            LotStatus statusEnum = LotStatus.fromCode(lm.getCurrentStatus());
		            String statusLabel = (statusEnum != null) ? statusEnum.getLabel() : lm.getCurrentStatus();

		            return new LotRootDTO(lm.getLotNo(), lm.getDisplayName(), statusLabel);
		        })
		        .toList();
	}
	
	// LOT ROOT 상세 정보 조회
	@Transactional(readOnly = true)
	public LotRootDetailDTO getRootLotDetail(String lotNo) {

	    LotMaster lot = lotMasterRepository.findByLotNo(lotNo)
	            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 LOT : " + lotNo));

	    WorkOrder wo = null;
	    if (lot.getOrderId() != null) {
	        wo = workOrderRepository.findById(lot.getOrderId()).orElse(null);
	    }

	    // 제품
	    String productCode = null;
	    String productName = null;
	    String productType = null;
	    if (wo != null && wo.getProduct() != null) {
	        ProductMst p = wo.getProduct();
	        productCode = p.getPrdId();
	        productName = p.getPrdName();
	        productType = p.getPrdCat(); 
	    }

	    // 상태 라벨
	    LotStatus lotStatusEnum = LotStatus.fromCode(lot.getCurrentStatus());
	    String statusLabel = (lotStatusEnum != null)
	            ? lotStatusEnum.getLabel()
	            : lot.getCurrentStatus();


	    // 수량
	    Integer planQty = (wo != null) ? wo.getPlanQty() : null;

	    // 양품/불량 = work_order_process 에서 마지막 결과 가져오기 
	    Integer goodQty = null;
	    Integer defectQty = null;
	    if (wo != null) {
	        List<WorkOrderProcess> processes =
	                workOrderProcessRepository
	                        .findByWorkOrderOrderIdOrderByStepSeqAsc(wo.getOrderId());

	        WorkOrderProcess lastWithResult = processes.stream()
	                .filter(p -> p.getGoodQty() != null || p.getDefectQty() != null)
	                .reduce((a, b) -> b)
	                .orElse(null);

	        if (lastWithResult != null) {
	            goodQty   = lastWithResult.getGoodQty();
	            defectQty = lastWithResult.getDefectQty();
	        }
	    }

	    // 일정
	    LocalDate startDate = null;
	    LocalDate expectedEndDate = null;
	    if (wo != null) {
	        startDate       = (wo.getActStartDate() != null)
	                ? wo.getActStartDate().toLocalDate()
	                : wo.getPlanStartDate().toLocalDate();
	        expectedEndDate = wo.getPlanEndDate().toLocalDate();
	    }

	    // 라우트 요약 : RT-SC2501-5G (블렌딩 → 여과 → )
	    String routeId = null;
	    String routeSteps = null;

	    if (wo != null) {
	        routeId = wo.getRouteId();   // 예: RT-LG030

	        List<WorkOrderProcess> processes =
	                workOrderProcessRepository
	                        .findByWorkOrderOrderIdOrderByStepSeqAsc(wo.getOrderId());

	        routeSteps = processes.stream()
	                .map(p -> p.getProcess().getProcessName())    // 예: 블렌딩, 여과, 충전...
	                .collect(Collectors.joining(" \u2192 "));     // "블렌딩 → 여과 → 충전 → ..."
	    }

	    return LotRootDetailDTO.builder()
	            .lotNo(lot.getLotNo())
	            .productCode(productCode)
	            .productName(productName)
	            .productType(labelOf(productType))
	            .lotStatusLabel(statusLabel)
	            .workOrderId(wo != null ? wo.getOrderId() : null)
	            .planQty(planQty)
	            .goodQty(goodQty)
	            .defectQty(defectQty)
	            .startDate(startDate)
	            .expectedEndDate(expectedEndDate)
	            .routeId(routeId)
	            .routeSteps(routeSteps)
	            .build();
	}
	
	// ================================================================================
	// 공정 정보
	// ================================================================================
	// 선택 LOT 기준 1차 공정 LOT 트리용 노드
	@Transactional(readOnly = true)
	public List<LotProcessNodeDTO> getProcessNodesForLot(String lotNo) {

	    LotMaster lot = lotMasterRepository.findByLotNo(lotNo)
	            .orElseThrow(() -> new IllegalArgumentException("LOT 없음: " + lotNo));

	    String orderId = lot.getOrderId();
	    if (orderId == null) return List.of();

	    List<WorkOrderProcess> processes =
	            workOrderProcessRepository
	                    .findByWorkOrderOrderIdOrderByStepSeqAsc(orderId);

	    return processes.stream()
	            .map(proc -> {
	                String processId = proc.getProcess().getProcessId();
	                String processName = proc.getProcess().getProcessName();
	                // 공정 상태 코드 
	                ProcessStepStatus stepStatus = ProcessStepStatus.fromCode(proc.getStatus());
	                String statusLabel = (stepStatus != null)
	                        ? stepStatus.getLabel()
	                        : proc.getStatus();

	                return new LotProcessNodeDTO(
	                        proc.getStepSeq(),
	                        processId,
	                        processName,
	                        statusLabel,
	                        orderId
	                );
	            })
	            .toList();
	}

	// 공정 상세 조회
	@Transactional(readOnly = true)
	public LotProcessDetailDTO getProcessDetail(String orderId, Integer stepSeq) {
		
		// 1) 공정 엔티티 조회 (작업지시 + 공정 단계)
		WorkOrderProcess wop = workOrderProcessRepository.findByWorkOrderOrderIdAndStepSeq(orderId, stepSeq)
				.orElseThrow(() -> new IllegalArgumentException("공정 정보를 찾을 수 없습니다. orderId = " + orderId + ", stepSeq = " + stepSeq));
		
		// 2) 공정 마스터 (ProcessMst) - 공정명 + 공정ID
		ProcessMst process = wop.getProcess();
		String processId = process != null ? process.getProcessId() : null;
		String processName = process != null ? process.getProcessName()	: null;
		
		// 3) 작업지시 - 라인
		WorkOrder workOrder = wop.getWorkOrder();
		String lineId = null;
		ProdLine line = null;
		
		if (workOrder != null) {
		    line = workOrder.getLine();  
		    if (line != null) {
		        lineId = line.getLineId();   
		    }
		}
		
		// 4) 공정 담당자 조회 (WorkerProcess)
		Emp worker = null;
		String workerId = null;
		String workerName = null;
		String deptName = null;
		
		Long qcResultId = null;

		if ("PRC-QC".equals(processId)) {

		    QcResult latest = qcResultRepository
		            .findFirstByOrderIdOrderByQcResultIdDesc(orderId)
		            .orElse(null);

		    if (latest != null) {
		        qcResultId = latest.getQcResultId();

		        String inspectorId = latest.getInspectorId();
		        if (inspectorId != null) {
		            worker = empRepository.findByEmpId(inspectorId).orElse(null);
		        }
		    }

		} else {
		    WorkerProcess workerProcess =
		            workerProcessRepository
		                    .findFirstBySchedule_Work_OrderIdAndProcess_ProcessId(orderId, processId)
		                    .orElse(null);

		    worker = (workerProcess != null) ? workerProcess.getWorker() : null;
		}

		if (worker != null) {
		    workerId = worker.getEmpId();
		    workerName = worker.getEmpName();
		    Dept dept = worker.getDept();
		    deptName = (dept != null) ? dept.getDeptName() : null;
		}
		
		// 5) 설비 정보 조회
		List<LotEquipInfoDTO> equipments = List.of();

		if (lineId != null && processId != null && !"PRC-QC".equals(processId)) {

		    List<ProdEquip> prodEquips = prodEquipRepository.findForLineAndProcess(lineId, processId);

		    equipments = prodEquips.stream()
		        .map(pe -> {
		            Equipment mst = pe.getEquipment();
		            return LotEquipInfoDTO.builder()
		                .prodEquipId(pe.getEquipId()) 
		                .equipCode(mst != null ? mst.getEquipId() : null)
		                .equipName(pe.getEquipName())
		                .status(labelOf(pe.getStatus()))
		                .stdName(mst != null ? mst.getEquipName() : null)
		                .koName(mst != null ? mst.getKoName() : null)
		                .build();
		        })
		        .toList();
		}

		
		// 6) 양품/불량 + 불량률 계산
		Long goodQty   = (wop.getGoodQty()   == null) ? 0L : wop.getGoodQty().longValue();
		Long defectQty = (wop.getDefectQty() == null) ? 0L : wop.getDefectQty().longValue();

		double defectRate = 0.0;
		long total = goodQty + defectQty;
		if (total > 0) {
		    defectRate = (double) defectQty * 100.0 / total;
		}

		// 7) DTO 생성 및 반환
		return LotProcessDetailDTO.builder()
		        .processId(processId)
		        .processName(processName)
		        .status(labelOf(wop.getStatus()))
		        .deptName(deptName)
		        .workerId(workerId)
		        .workerName(workerName)
		        .startTime(wop.getStartTime())
		        .endTime(wop.getEndTime())
		        .goodQty(goodQty)
		        .defectQty(defectQty)
		        .defectRate(defectRate)
		        .lineId(lineId)
		        .equipments(equipments)
		        .qcResultId(qcResultId)
		        .build();
	}

	// ================================================================================
	// 자재 정보
	// ================================================================================
	// 선택 LOT 기준 2차 자재 LOT 트리용 노드
	public List<LotMaterialNodeDTO> getMaterialNodesForLot(String lotNo) {
		
		List<LotRelationship> rels = lotRelationshipRepository.findByOutputLot_LotNo(lotNo);
		
		if (rels.isEmpty()) {
			return List.of();
		}
		
		return rels.stream()
				.map(rel -> {
					
					LotMaster child = rel.getInputLot();	// 자재 LOT
					
					// child 자체가 null일 가능성도 방어
	                String lotNoChild   = (child != null) ? child.getLotNo() : null;
	                String name         = (child != null) ? child.getDisplayName() : "[LOT 없음]";

	                String unit = null;
	                if (child != null && child.getMaterial() != null) {
	                    unit = child.getMaterial().getMatUnit();
	                } else {
	                    // 임시 방편: 원자재 매핑이 끊어진 LOT
	                    unit = null;       
	                }
					
					return new LotMaterialNodeDTO(
							lotNoChild,
							name, 
							rel.getUsedQty(), 
							unit
					);
				})
				.toList();
	}

	// 자재 상세 정보
	public LotMaterialDetailDTO getMaterialDetail(String outputLotNo, String inputLotNo) {
		
		// 1) 자재 엔티티 조회 (부모 LOT + 자식 LOT)
		LotRelationship lr = lotRelationshipRepository.findByOutputLot_LotNoAndInputLot_LotNo(outputLotNo, inputLotNo)
			        .orElseThrow(() -> new IllegalArgumentException("관계 없음: output=" + outputLotNo + ", input=" + inputLotNo));
		
		// 2) 원자재 LOT 꺼내기
	    LotMaster lot = lr.getInputLot();          // inputLot == 원자재 LOT
	    MaterialMst m = (lot != null) ? lot.getMaterial() : null;

	    // 3) lotNo로 입고/재고
	    InboundItem ii = inboundItemRepository.findTopByLotNoOrderByInboundItemIdDesc(inputLotNo)
	    		.orElse(null);
	    
	    Inventory inv = inventoryRepository.findTopByLotNoOrderByIvIdDesc(inputLotNo)
	    		.orElse(null);
	    
	    // 4) 거래처 정보
	    Client client = null;

	    if (ii != null && ii.getInbound() != null) {
	        String materialOrderId = ii.getInbound().getMaterialId(); // 발주 ID

	        if (materialOrderId != null) {
	            MaterialOrder mo = materialOrderRepository.findById(materialOrderId)
	                    .orElse(null);

	            if (mo != null) {
	                client = clientRepository.findById(mo.getClientId())
	                        .orElse(null);
	            }
	        }
	    }


	    return LotMaterialDetailDTO.builder()
	        .usedQty(lr.getUsedQty())
	        .lotNo(lot.getLotNo())
	        .lotType(labelOf(lot.getLotType()))
	        .lotStatus(labelOf(lot.getCurrentStatus()))
	        .lotCreatedDate(lot.getCreatedDate())
	        .matId(m != null ? m.getMatId() : null)
	        .matName(m != null ? m.getMatName() : lot.getDisplayName())
	        .matType(labelOf(m != null ? m.getMatType() : null))
	        .matUnit(m != null ? m.getMatUnit() : null)
	        .inboundId(ii != null && ii.getInbound() != null ? ii.getInbound().getInboundId() : null)
	        .inboundAmount(ii != null ? ii.getInboundAmount() : null)
	        .manufactureDate(ii != null ? ii.getManufactureDate() : null)
	        .expirationDate(ii != null ? ii.getExpirationDate() : null)
	        .inboundLocationId(ii != null ? ii.getLocationId() : null)
	        .ivAmount(inv != null ? inv.getIvAmount() : null)
	        .ivStatus(labelOf(inv != null ? inv.getIvStatus() : null))
	        .inventoryLocationId(inv != null && inv.getWarehouseLocation() != null ? inv.getWarehouseLocation().getLocationId() : null)
	        .ibDate(inv != null ? inv.getIbDate() : null)
	        .clientId(client != null ? client.getClientId() : null)
	        .clientName(client != null ? client.getClientName() : null)
	        .businessNo(client != null ? client.getBusinessNo() : null)
	        .managerTel(client != null ? client.getManagerTel() : null)
	        .build();
	}

	

	private static final Map<String, String> STATUS_LABEL = Map.ofEntries(
		// LOT 상태
	    Map.entry("NEW", "신규"),
	    Map.entry("IN_PROCESS", "진행 중"),
	    Map.entry("PROD_DONE", "생산 완료"),
	    
	    Map.entry("IN_PROGRESS", "진행 중"),
	    Map.entry("READY", "대기"),
	    Map.entry("DONE", "완료"),
	    
	    // 재고 상태
	    Map.entry("NORMAL", "정상"),
	    Map.entry("HOLD", "보류"),
	    Map.entry("SCRAPPED", "폐기"),
	    Map.entry("EXPIRED", "유통기한 경과"),
	    
	    // LOT/자재 유형
	    Map.entry("RAW", "원재료"),
	    Map.entry("SUB", "부자재"),
	    Map.entry("PKG", "포장재"),
	    Map.entry("FIN", "완제품"),
	    Map.entry("FINISHED_GOODS", "완제품"),

	    // 설비 상태
	    Map.entry("RUN", "가동"),
	    Map.entry("STOP", "정지"),
	    Map.entry("BREAKDOWN", "고장"),
	    Map.entry("MAINTENANCE", "점검")
	);

	private String labelOf(String code) {
	    if (code == null) return "-";
	    String key = code.trim();          // 공백 제거
	    if (key.isEmpty()) return "-";

	    // 영어 코드들은 대문자로 통일해서 매칭 (한글은 영향 없음)
	    String upper = key.toUpperCase();

	    return STATUS_LABEL.getOrDefault(upper, key);
	}

	// ================================================================================================
	@Transactional(readOnly = true)
	public List<LotRootDTO> getMaterialRootLots(String keyword, String status, String type) {

	    List<String> lotTypes = List.of("RAW", "SUB", "PKG");
	    List<LotMaster> lots = lotMasterRepository.findRootLotsOrdered(lotTypes);

	    final String kw = (keyword == null) ? "" : keyword.trim();
	    final String st = (status == null) ? "" : status.trim();
	    final String tp = (type == null) ? "" : type.trim();

	    return lots.stream()
	        .filter(lm -> kw.isEmpty()
	            || (lm.getLotNo() != null && lm.getLotNo().contains(kw))
	            || (lm.getDisplayName() != null && lm.getDisplayName().contains(kw)))
	        .filter(lm -> st.isEmpty() || st.equals(lm.getCurrentStatus()))
	        .filter(lm -> tp.isEmpty() || tp.equals(lm.getLotType()))
	        .map(lm -> {
	            LotStatus statusEnum = LotStatus.fromCode(lm.getCurrentStatus());
	            String statusLabel = (statusEnum != null) ? statusEnum.getLabel() : lm.getCurrentStatus();
	            return new LotRootDTO(lm.getLotNo(), lm.getDisplayName(), statusLabel);
	        })
	        .toList();
	}

	
	@Transactional(readOnly = true)
	public LotMaterialDetailDTO getMaterialRootDetail(String lotNo) {

	    LotMaster lot = lotMasterRepository.findByLotNo(lotNo)
	        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 LOT : " + lotNo));

	    MaterialMst m = lot.getMaterial();

	    InboundItem ii = inboundItemRepository.findTopByLotNoOrderByInboundItemIdDesc(lotNo).orElse(null);
	    Inventory inv = inventoryRepository.findTopByLotNoOrderByIvIdDesc(lotNo).orElse(null);

	    Client client = null;
	    if (ii != null && ii.getInbound() != null) {
	        String materialOrderId = ii.getInbound().getMaterialId();
	        if (materialOrderId != null) {
	            MaterialOrder mo = materialOrderRepository.findById(materialOrderId).orElse(null);
	            if (mo != null) client = clientRepository.findById(mo.getClientId()).orElse(null);
	        }
	    }

	    return LotMaterialDetailDTO.builder()
	        // 마스터
	        .matId(m != null ? m.getMatId() : null)
	        .matName(m != null ? m.getMatName() : lot.getDisplayName())
	        .matType(labelOf(m != null ? m.getMatType() : null))
	        .matUnit(m != null ? m.getMatUnit() : null)

	        // LOT
	        .lotNo(lot.getLotNo())
	        .lotType(labelOf(lot.getLotType()))
	        .lotStatus(labelOf(lot.getCurrentStatus()))
	        .lotCreatedDate(lot.getCreatedDate())

	        // ROOT 상세에서는 “완제품 기준 투입수량” 의미 없음 → null 또는 0
	        .usedQty(null)

	        // 입고
	        .inboundId(ii != null && ii.getInbound() != null ? ii.getInbound().getInboundId() : null)
	        .inboundAmount(ii != null ? ii.getInboundAmount() : null)
	        .manufactureDate(ii != null ? ii.getManufactureDate() : null)
	        .expirationDate(ii != null ? ii.getExpirationDate() : null)
	        .inboundLocationId(ii != null ? ii.getLocationId() : null)

	        // 재고
	        .ivAmount(inv != null ? inv.getIvAmount() : null)
	        .ivStatus(labelOf(inv != null ? inv.getIvStatus() : null))
	        .inventoryLocationId(inv != null && inv.getWarehouseLocation() != null
	                ? inv.getWarehouseLocation().getLocationId() : null)
	        .ibDate(inv != null ? inv.getIbDate() : null)

	        // 거래처
	        .clientId(client != null ? client.getClientId() : null)
	        .clientName(client != null ? client.getClientName() : null)
	        .businessNo(client != null ? client.getBusinessNo() : null)
	        .managerTel(client != null ? client.getManagerTel() : null)
	        .build();
	}

	@Transactional(readOnly = true)
	public List<LotUsedProductNodeDTO> getUsedProductsByMaterialLot(String inputLotNo) {

	    List<LotRelationship> rels = lotRelationshipRepository.findByInputLot_LotNo(inputLotNo);
	    if (rels == null || rels.isEmpty()) return List.of();

	    return rels.stream().map(rel -> {
	        LotMaster out = rel.getOutputLot();
	        LotMaster in  = rel.getInputLot(); // 자재 lot

	        String unit = null;
	        if (in != null && in.getMaterial() != null) {
	            unit = in.getMaterial().getMatUnit();
	        }

	        // 상태 라벨까지 넣고 싶으면
	        String statusLabel = null;
	        if (out != null) {
	            LotStatus s = LotStatus.fromCode(out.getCurrentStatus());
	            statusLabel = (s != null) ? s.getLabel() : out.getCurrentStatus();
	        }

	        return LotUsedProductNodeDTO.builder()
	            .lotNo(out != null ? out.getLotNo() : null)
	            .displayName(out != null ? out.getDisplayName() : null)
	            .usedQty(rel.getUsedQty())   // Integer
	            .unit(unit)
	            .status(statusLabel)         // 선택
	            .build();
	    }).toList();
	}

	
	
	
	
	
	
	
	
	
	
	
} // LotTraceService 끝
