package com.yeoun.leave.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.yeoun.leave.entity.AnnualLeaveHistory;

@Repository
public interface LeaveHistoryRepository extends JpaRepository<AnnualLeaveHistory, Long> {

	// 개인 연차 현황(리스트)
	@Query("""
			SELECT h
			FROM AnnualLeaveHistory h
			WHERE h.emp.empId = :empId
			  AND (
			        (h.startDate BETWEEN :startOfYear AND :endOfYear)
			     OR (h.endDate BETWEEN :startOfYear AND :endOfYear)
			  )
			 ORDER BY h.startDate DESC
			""")
	 List<AnnualLeaveHistory> findAnnualLeaveInYear(
			 @Param("empId") String empId,
			 @Param("startOfYear") LocalDate startOfYear,
			 @Param("endOfYear") LocalDate endOfYear
		    );

	// 오늘 날짜 기준으로 휴무인지 확인
	boolean existsByEmp_EmpIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(String empId, LocalDate today1, LocalDate today2);
	
	// 연차 스케줄 조회
	@Query("""
			SELECT h
			FROM AnnualLeaveHistory h
			WHERE (h.startDate  <= :endDate AND h.endDate >= :startDate)
			  AND (h.emp.empId = :empId or h.emp.dept.deptId = :deptId)
			""")
	List<AnnualLeaveHistory> findLeaveHistorySchedule(
			@Param("startDate")LocalDate  startDate
			, @Param("endDate")LocalDate  endDate
			, @Param("empId")String empId
			, @Param("deptId")String deptId);

	// 반차 여부
	@Query("""
			SELECT COUNT(a) > 0
			FROM AnnualLeaveHistory a
			WHERE a.emp.empId = :empId
				AND a.startDate = :date
				AND a.leaveType = 'HALF'
			""")
	boolean existsHalf(@Param("empId") String empId, @Param("date") LocalDate date);
}
