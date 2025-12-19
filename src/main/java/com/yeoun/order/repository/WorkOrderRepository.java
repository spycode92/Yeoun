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
            AND w.STATUS != 'CANCELED'
            AND w.STATUS != 'SCRAPPED'
    """, nativeQuery = true)
	Integer sumWorkOrderQty(@Param("planId")String planId);

	// 출고 상태가 'N'인 작업지시서 조회
	@Query("""
		    SELECT w
		    FROM WorkOrder w
		    WHERE w.outboundYn = :outboundYn
		    AND w.status != 'CANCELED'
		""")
	List<WorkOrder> findByOutboundYn(@Param("outboundYn") String outboundYn);

	// 작업지시서 조회
	Optional<WorkOrder> findByOrderId(String workOrderId);
	
	// 같은 PLAN_ID 아래 아직 완료 안 된 작업지시가 있는지 확인
	boolean existsByPlanIdAndStatusNot(String planId, String status);
	
	@Query("""
		    SELECT wo
		    FROM WorkOrder wo
		    LEFT JOIN FETCH wo.product
		    LEFT JOIN FETCH wo.line
		    WHERE wo.orderId = :orderId
		    """)
	Optional<WorkOrder> findByIdWithFetch(@Param("orderId") String orderId);
	
	
	// ==========================
	// 생산관리 대시보드
	// ==========================
	// 오늘 완료된 작업지시 수
	long countByStatusAndActEndDateBetween(String status, LocalDateTime start, LocalDateTime end);
	
	// 진행 중 공정 단계
    long countByStatus(String status);
    
    // 지연 작업지시 수 (예정완료시간 초과)
    long countByStatusAndPlanEndDateBefore(String status, LocalDateTime now);
    
	// 오늘 작업지시(작성일 기준)
    @Query("""
        select count(w)
        from WorkOrder w
        where w.createdDate >= :start
          and w.createdDate <  :end
    """)
    long countTodayOrders(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);


}
