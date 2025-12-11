package com.yeoun.lot.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.lot.dto.LotHistoryDTO;
import com.yeoun.lot.dto.LotMasterDTO;
import com.yeoun.lot.dto.LotMaterialNodeDTO;
import com.yeoun.lot.dto.LotProcessNodeDTO;
import com.yeoun.lot.dto.LotRootDTO;
import com.yeoun.lot.entity.LotHistory;
import com.yeoun.lot.entity.LotMaster;
import com.yeoun.lot.entity.LotRelationship;
import com.yeoun.lot.repository.LotHistoryRepository;
import com.yeoun.lot.repository.LotMasterRepository;
import com.yeoun.lot.repository.LotRelationshipRepository;
import com.yeoun.masterData.entity.ProcessMst;
import com.yeoun.masterData.repository.ProcessMstRepository;
import com.yeoun.process.entity.WorkOrderProcess;
import com.yeoun.process.repository.WorkOrderProcessRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class LotTraceService {
	private final LotMasterRepository lotMasterRepository;
	private final LotHistoryRepository historyRepository;
	private final ProcessMstRepository processMstRepository;
	private final EmpRepository empRepository;
	private final LotHistoryRepository lotHistoryRepository;
	private final WorkOrderProcessRepository workOrderProcessRepository; 
	private final LotRelationshipRepository lotRelationshipRepository;
	
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
	// [LOT 추적 1단계]
	// 완제품 LOT(FIN) 목록 조회
	@Transactional(readOnly = true)
	public List<LotRootDTO> getFinishedLots() {
		
		// 1) FIN LOT 목록 조회
		List<LotMaster> lots = 
				lotMasterRepository.findByLotTypeOrderByCreatedDateDesc("FIN");
		
		return lots.stream()
	            .map(lm -> new LotRootDTO(
	                    lm.getLotNo(),
	                    lm.getDisplayName(),
	                    lm.getCurrentStatus()
	            ))
	            .toList();
	}
	
	// [LOT 추적] 오른쪽 상세 카드에 표시할 LOT 기본 정보 조회
	@Transactional(readOnly = true)
    public LotMaster getLotDetail(String lotNo) {

        return lotMasterRepository.findByLotNo(lotNo)
                .orElseThrow(() ->
                        new IllegalArgumentException("LOT_MASTER에 존재하지 않는 LOT : " + lotNo));
    }
	
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

	                String status = lotHistoryRepository
	                        .findTopByLot_LotNoAndProcess_ProcessIdOrderByHistIdDesc(lotNo, processId)
	                        .map(LotHistory::getStatus)
	                        .orElse("NEW");

	                return new LotProcessNodeDTO(
	                        proc.getStepSeq(),
	                        processId,
	                        processName,
	                        status
	                );
	            })
	            .toList();
	}


	// ================================================================================
	// 선택 LOT 기준 2차 자재 LOT 트리용 노드
	public List<LotMaterialNodeDTO> getMaterialNodesForLot(String lotNo) {
		
		List<LotRelationship> rels = 
				lotRelationshipRepository.findByOutputLot_LotNo(lotNo);
		
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
							child.getLotNo(), 
							name, 
							rel.getUsedQty(), 
							unit
					);
				})
				.toList();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
} // LotTraceService 끝
