package com.yeoun.approval.repository;


import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeoun.approval.dto.ApprovalFormDTO;
import org.springframework.stereotype.Repository; 
import com.yeoun.approval.entity.ApprovalDoc;
import com.yeoun.approval.entity.ApprovalForm;
import com.yeoun.emp.entity.Dept;
import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.entity.Position;

import java.util.List;
import java.util.Optional;


@Repository
public interface ApprovalDocRepository extends JpaRepository<ApprovalDoc, Long> {
	//Optional<ApprovalDoc> findByEmpId(String empId);
	Optional<Emp> findByEmpId(String empId);
	
	// 사원 목록 조회
	@Query("SELECT m FROM Emp m WHERE status='ACTIVE'")
    List<Emp> findAllMember();

	// 퇴사안한 사원목록 조회
	@Query(value ="""
			 SELECT emp_id,emp_name 
			 FROM emp 
			 WHERE status='ACTIVE'
			 ORDER BY emp_name
			""",nativeQuery = true)
    List<Object[]> findAllMember2();

	// 직급정보 불러오기
	@Query(value ="""
			SELECT * FROM position
			""",nativeQuery = true)
	List<Position> findPosition();
	//기안서 양식종류 - 부서별 다름
    @Query(value ="""
    		SELECT afede.form_code
				 ,afede.form_name
				 ,afede.dept_id
				 ,afede.dept_name
				 ,afede.approver_1
				 ,afede.approver_name1
				 ,afede.approver_2
				 ,afede.approver_name2
				 ,afede.approver_3
				 ,e3.emp_name
			FROM (SELECT afed.form_code
				 		,afed.form_name
				 		,afed.dept_id
				 		,afed.dept_name
				 		,afed.approver_1
				 		,afed.approver_name1
				 		,afed.approver_2 
				 		,e2.emp_name as approver_name2
				 		,afed.approver_3		 
				 FROM (SELECT af.form_code
				 				,af.form_name
				 				,af.dept_id
				 				,d.dept_name
				 				,af.approver_1
				 				,e1.emp_name as approver_name1
				 				,af.approver_2
				 				,af.approver_3
				 		FROM approval_form af
				 		INNER JOIN dept d ON af.dept_id = d.dept_id
				 		LEFT OUTER JOIN emp e1 ON af.approver_1 = e1.emp_id) afed
				LEFT OUTER JOIN emp e2 ON afed.approver_2 = e2.emp_id) afede
				LEFT OUTER JOIN emp e3 ON afede.approver_3 = e3.emp_id
			WHERE afede.dept_id = :deptId
    		""",nativeQuery = true)
    List<ApprovalForm> findAllFormTypes(@Param("deptId") String deptId);
	
	//부서목록조회
	@Query("SELECT d FROM Dept d")
	List<Dept> findAllDepartments();

	//main에사용하는쿼리 현재결재권한자로서 결재해야할 것과 내가 올린 결재만
	@Query("select a from ApprovalDoc a " 
			+ "where (a.docStatus not in ('완료', '반려') and a.approver = :empId)" 
			+ "or a.empId = :empId "
			+ "order by createdDate desc")
	Page<ApprovalDoc> getSummaryApprovalPage(@Param("empId") String empId, Pageable pageable);
			


}
