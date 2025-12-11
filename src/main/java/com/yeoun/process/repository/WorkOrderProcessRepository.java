package com.yeoun.process.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeoun.process.entity.WorkOrderProcess;

@Repository
public interface WorkOrderProcessRepository extends JpaRepository<WorkOrderProcess, String> {
	
	// 목록 조회용 - 여러 작업지시에 대한 공정 전체 목록을 한 번에 조회
	List<WorkOrderProcess> findByWorkOrderOrderIdInOrderByWorkOrderOrderIdAscStepSeqAsc(List<String> orderIds);
	
    // 상세 모달용 - 작업지시번호 기준 공정 전체 목록 조회
    List<WorkOrderProcess> findByWorkOrderOrderIdOrderByStepSeqAsc(String orderId);
    
    // 상세 모달 내 공정 시작/종료 - 작업지시 + 단계순번으로 공정 1건 조회
    Optional<WorkOrderProcess> findByWorkOrderOrderIdAndStepSeq(String orderId, Integer stepSeq);
    
 	// 마지막 단계 판별 - 현재 작업지시에서 나보다 뒤(stepSeq가 더 큰) 공정 단계가 존재하는지 여부
    boolean existsByWorkOrderOrderIdAndStepSeqGreaterThan(String orderId, Integer stepSeq);

    // 공정 id와 status로 탐색하여 해당하는 갯수 세기
    Integer countByWorkOrder_OrderIdAndStatus (String orderId, String status);
    
	// QC 공정 한 건 조회 (orderId + processId 기준)
    Optional<WorkOrderProcess> findByWorkOrderOrderIdAndProcessProcessId(String orderId, String processId);

    // 공정 Id로 정보 조회
	Optional<WorkOrderProcess> findByWopId(String wopId);
    

}
