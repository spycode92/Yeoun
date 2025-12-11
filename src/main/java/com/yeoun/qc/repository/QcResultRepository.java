package com.yeoun.qc.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.yeoun.qc.dto.QcRegistDTO;
import com.yeoun.qc.dto.QcResultListDTO;
import com.yeoun.qc.entity.QcResult;

@Repository
public interface QcResultRepository extends JpaRepository<QcResult, Long> {
	
	// 포장공정 시작 가능 여부 체크용 (품질 검사 PASS 여부)
	boolean existsByOrderIdAndOverallResult(String orderId, String overallResult);
	
	// 작업지시 기준 결과 1건 조회 (공정 목록에서 양품수량 가져오기)
    Optional<QcResult> findByOrderId(String orderId);

    // 여러 작업지시에 대한 QC 결과를 한 번에 조회
    List<QcResult> findByOrderIdIn(List<String> orderIds);
    
    // QC 등록 목록용 DTO 조회 (PENDING 상태만)
    @Query("""
        SELECT new com.yeoun.qc.dto.QcRegistDTO(
    		q.qcResultId,
            q.orderId,
            w.product.prdId,
            w.product.prdName,
            w.planQty,
            q.overallResult,
            q.inspectionDate,
            wop.lotNo
        )
        FROM QcResult q
        JOIN WorkOrder w ON w.orderId = q.orderId
        LEFT JOIN WorkOrderProcess wop ON wop.workOrder = w AND wop.stepSeq = 1
        WHERE q.overallResult = :status
        ORDER BY w.createdDate DESC
        """)
    List<QcRegistDTO> findRegistListByStatus(@Param("status") String status);
    
    //
    @Query("""
            select new com.yeoun.qc.dto.QcResultListDTO(
                r.qcResultId,
                r.orderId,
                p.prdId,
                p.prdName,
                r.inspectionDate,
                r.overallResult,
                r.failReason
            )
            from QcResult r
              join WorkOrder w on r.orderId = w.orderId
              join w.product p
            where r.overallResult <> 'PENDING'
            order by r.inspectionDate desc nulls last
            """)
	List<QcResultListDTO> findResultListForView();
    
}
