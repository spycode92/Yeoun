package com.yeoun.qc.repository;

import java.time.LocalDate;
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
            wop.lotNo,
            q.createdDate,
            w.line.lineName
        )
        FROM QcResult q
        JOIN WorkOrder w ON w.orderId = q.orderId
        LEFT JOIN WorkOrderProcess wop ON wop.workOrder = w AND wop.stepSeq = 1
        WHERE q.overallResult = :status
        ORDER BY q.qcResultId ASC
        """)
    List<QcRegistDTO> findRegistListByStatus(@Param("status") String status);
    
    // QC 결과 조회
    @Query("""
		select new com.yeoun.qc.dto.QcResultListDTO(
		    r.qcResultId,
		    r.orderId,
		    p.prdId,
		    p.prdName,
		    r.inspectionDate,
		    r.overallResult,
		    r.failReason,
		    e.empName
		)
		from QcResult r
		  join WorkOrder w on r.orderId = w.orderId
		  join w.product p
		  left join Emp e on r.inspectorId = e.empId
		where r.overallResult <> 'PENDING'
		  and (:result = 'ALL' or r.overallResult = :result)
		  and (:start is null or r.inspectionDate >= :start)
		  and (:end   is null or r.inspectionDate <= :end)
		  and (
		        :kw is null
		        or upper(r.orderId) like concat('%', :kw, '%')
		        or upper(p.prdName) like concat('%', :kw, '%')
		      )
		order by r.inspectionDate desc nulls last, r.qcResultId desc
		""")
	List<QcResultListDTO> findResultListForView(@Param("start") LocalDate start,
										        @Param("end") LocalDate end,
										        @Param("kw") String kw,
										        @Param("result") String result);
    
    // 작업지시 기준으로 가장 최근에 생성된 QC 결과 1건 조회
    // (QC 결과가 여러 건 존재할 수 있으므로 최신 데이터 기준)
    Optional<QcResult> findFirstByOrderIdOrderByQcResultIdDesc(String orderId);

    // =========================
    // 생산관리 대시보드
    // =========================
    // QC FAIL 집계
    long countByOverallResultAndInspectionDateGreaterThanEqualAndInspectionDateLessThan(
            String overallResult,
            LocalDate start,
            LocalDate end
    );


    
}
