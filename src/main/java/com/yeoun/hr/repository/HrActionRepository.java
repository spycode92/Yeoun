package com.yeoun.hr.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.yeoun.hr.entity.HrAction;

@Repository
public interface HrActionRepository extends JpaRepository<HrAction, Long> {
	
	// 인사 발령 목록
	List<HrAction> findAll();
	
	// 인사 발령 목록 (검색)
	@Query("""
	    select a
	    from HrAction a
	      join a.emp e
	    where
	      a.status = '적용완료'
	      and (:keyword is null or :keyword = '' 
	             or e.empId like concat('%', :keyword, '%')
	             or e.empName like concat('%', :keyword, '%'))
	      and (:actionType is null or :actionType = '' or a.actionType = :actionType)
	      and (:startDate is null or a.effectiveDate >= :startDate)
	      and (:endDate is null or a.effectiveDate <= :endDate)
	    order by a.effectiveDate desc
	    """)
    List<HrAction> searchHrActions(@Param("keyword") String keyword,
                                   @Param("actionType") String actionType,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    // 전자결재 연결
    Optional<HrAction> findByApprovalId(Long approvalId);
    
    // 스케줄러에서 사용할 조회 메서드
    List<HrAction> findByStatusAndAppliedYnAndEffectiveDateLessThanEqual (
    	String status,				// 상태가 '승인완료'인 것만
    	String appliedYn,			// 아직 'N'인 것만
    	LocalDate effectiveDate		// 효력일이 오늘 이전/오늘까지 도달한 것만
    );


}
