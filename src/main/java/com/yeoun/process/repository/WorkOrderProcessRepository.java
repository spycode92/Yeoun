package com.yeoun.process.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
	
	// QC 공정 이후 모든 공정 단계 조회 (QC 실패 시 미진행 공정 정리용)
	List<WorkOrderProcess> findByWorkOrderOrderIdAndStepSeqGreaterThan(String orderId, int qcStepSeq);
	
	// ==========================
	// 생산관리 대시보드
	// ==========================
    // QC 대기 상태
    @Query("""
            select count(p)
            from WorkOrderProcess p
            where p.status = 'QC_PENDING'
              and p.process.processId = :qcProcessId
        """)
    long countQcPendingOnly(@Param("qcProcessId") String qcProcessId);
    
    // QC 불합격: QC 공정에서 defectQty > 0 인 건
    @Query("""
        select count(p)
        from WorkOrderProcess p
        where p.process.processId = :qcProcessId
          and coalesce(p.defectQty, 0) > 0
          and p.endTime >= :start
          and p.endTime <  :end
    """)
    long countQcFailToday(@Param("qcProcessId") String qcProcessId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);
    
    // 진행중 공정만 + 라인/공정마스터까지 한 번에 가져오기(N+1 방지)
    @Query("""
    	    select wop
    	    from WorkOrderProcess wop
    	      join fetch wop.workOrder wo
    	      join fetch wo.line ln
    	      join fetch wop.process pm
    	    where wop.status in :statuses
    	      and wop.stepSeq between 1 and 6
    	      and wo.status in :orderStatuses
    	""")
	List<WorkOrderProcess> findForHeatmap(@Param("statuses") List<String> statuses,
    	    							  @Param("orderStatuses") List<String> orderStatuses);

}
