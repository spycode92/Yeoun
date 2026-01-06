package com.yeoun.emp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.entity.EmpRole;

@Repository
public interface EmpRoleRepository extends JpaRepository<EmpRole, Long> {
	
	// 사번으로 역할 코드 목록 조회
	@Query("""
		    select er.role.roleCode
		    from EmpRole er
		    where er.emp.empId = :empId
		""")
	List<String> findRoleCodesByEmpId(@Param("empId") String empId);
	
	// 사번 기준 역할 코드 전체 삭제 (재할당/수정 시 필요)
	void deleteByEmp_EmpId(String empId);
	
	// 해당 사원의 EmpRole 엔티티 전체 조회
	List<EmpRole> findByEmp_EmpId(String empId);


}
