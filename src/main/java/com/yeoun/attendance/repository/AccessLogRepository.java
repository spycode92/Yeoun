package com.yeoun.attendance.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.yeoun.attendance.dto.AccessLogDTO;
import com.yeoun.attendance.entity.AccessLog;

import org.springframework.data.repository.query.Param;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

	// 오늘 날짜의 외근 기록 있는지 조회
	List<AccessLog> findByEmp_EmpIdAndAccessDate(String empId, LocalDate today);

	// 가장 최근 로그 조회
	@Query("""
		    SELECT a
		      FROM AccessLog a
		     WHERE a.emp.empId = :empId
		       AND a.accessDate = :today
		       AND a.returnTime IS NULL
		     ORDER BY a.outTime DESC
		""")
	 List<AccessLog> findOutLogsWithoutReturn(@Param("empId") String empId, @Param("today") LocalDate today);

	// 건물 출입 데이터 
	@Query("""
			SELECT a
			FROM AccessLog a
			JOIN FETCH a.emp e
			JOIN FETCH e.dept
			JOIN FETCH e.position
			WHERE a.accessDate >= :start
            AND a.accessDate <= :end
            ORDER BY a.accessDate DESC, a.outTime DESC
			""")
	List<AccessLog> findByAccessDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

	// 마지막 OUT 시간 조회
	@Query("""
		    SELECT a
		    FROM AccessLog a
		    WHERE a.emp.empId = :empId
		      AND a.accessDate = :targetDate
		      AND a.returnTime IS NULL
		      AND (a.accessType = 'OUT' OR a.accessType = 'OUTWORK')
		    ORDER BY a.outTime DESC
		""")
	Optional<AccessLog> findLastOutTime(@Param("empId") String empId, @Param("targetDate") LocalDate targetDate);

	// 외근 기록 조회
	List<AccessLog> findAllByAccessDateBetweenAndEmp_EmpIdAndAccessType(LocalDate start, LocalDate end,
			String empId, String accessType);
}
