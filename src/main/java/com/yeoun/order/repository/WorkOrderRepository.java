package com.yeoun.order.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.yeoun.order.entity.WorkOrder;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, String> {
	
	// 공정현황 전용 (상태 + 출고완료)
	List<WorkOrder> findByStatusInAndOutboundYn(List<String> statuses, String outboundYn);

	// 가장 최근 작업지시 번호 조회
	Optional<WorkOrder> findTopByOrderIdStartingWithOrderByOrderIdDesc(String todayPrefix);

	// 해당 생산계획으로 생성한 workOrder 갯수 조회
	@Query(value = """
        SELECT COALESCE(SUM(w.PLAN_QTY), 0) 
            FROM WORK_ORDER w
            WHERE w.PLAN_ID = :planId
    """, nativeQuery = true)
	Integer sumWorkOrderQty(@Param("planId")String planId);

	// 출고 상태가 'N'인 작업지시서 조회
	List<WorkOrder> findByOutboundYn(String outboundYn);

	// 작업지시서 조회
	Optional<WorkOrder> findByOrderId(String workOrderId);
	
	// 같은 PLAN_ID 아래 아직 완료 안 된 작업지시가 있는지 확인
	boolean existsByPlanIdAndStatusNot(String planId, String status);
	
	
	// ===================================================
	// 대시보드 KPI 전용
	// ===================================================
	// 1) 오늘 생성된 작업지시 수
	@Query("""
        SELECT COUNT(w)
        FROM WorkOrder w
        WHERE w.createdDate >= :start
          AND w.createdDate < :end
    """)
	long countCreatedBetween(@Param("start") LocalDateTime start,
	                         @Param("end") LocalDateTime end);

	// 2) 상태별 작업지시 수 (예: IN_PROGRESS, DONE 등)
	long countByStatus(String status);

	// 3) 지연 작업지시 수
	@Query("""
        SELECT COUNT(w)
        FROM WorkOrder w
        WHERE w.planEndDate < :now
          AND w.status <> 'DONE'
    """)
	long countDelayedOrders(@Param("now") LocalDateTime now);


}
