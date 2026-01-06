package com.yeoun.attendance.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yeoun.attendance.entity.Attendance;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

	// 사원 ID 및 현재 날짜 기준으로 출/퇴근 데이터 조회
	Optional<Attendance> findByEmp_EmpIdAndWorkDate(String empId, LocalDate workDate);

	// 출근 기록 있는지 확인
	boolean existsByEmp_EmpIdAndWorkDate(String empId, LocalDate today);

	// 개인 출퇴근 기록 조회
	List<Attendance> findByEmp_EmpIdAndWorkDateBetweenOrderByWorkDateDesc(String empId, LocalDate startDate, LocalDate endDate);

	// 전체직원 출퇴근 기록 조회
	@Query("""
		    SELECT a FROM Attendance a
		    JOIN FETCH a.emp e
		    JOIN FETCH e.dept
		    JOIN FETCH e.position
		    WHERE a.workDate BETWEEN :startDate AND :endDate
		    ORDER BY a.workDate DESC
		""")
	List<Attendance> findByWorkDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

	// 부서별 출퇴근 기록 조회
	@Query("""
		    SELECT a FROM Attendance a
		    JOIN FETCH a.emp e
		    JOIN FETCH e.dept d
		    JOIN FETCH e.position p
		    WHERE d.deptId = :deptId
		      AND a.workDate BETWEEN :startDate AND :endDate
		    ORDER BY a.workDate DESC
		""")
	List<Attendance> findByEmp_Dept_DeptIdAndWorkDateBetween(
			@Param("deptId") String deptId, 
			@Param("startDate") LocalDate startDate, 
			@Param("endDate") LocalDate endDate);

	// 출근 했지만 퇴근하지 않은 사람 조회
	List<Attendance> findByWorkDateAndWorkOutIsNull(LocalDate targetDate);


}
