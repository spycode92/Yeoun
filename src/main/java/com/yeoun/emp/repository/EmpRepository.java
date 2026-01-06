package com.yeoun.emp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.yeoun.emp.dto.EmpListDTO;
import com.yeoun.emp.entity.Emp;

@Repository
public interface EmpRepository extends JpaRepository<Emp, String> {
	
	// 사원번호로 사용자 조회
	Optional<Emp> findByEmpId(String empId);
	
	// ------ 중복 확인 ------
	boolean existsByEmpId(String candidate);	// 사원번호
	boolean existsByEmail(String email);		// 이메일
	boolean existsByMobile(String mobile);		// 전화번호
	boolean existsByRrn(String rrn);			// 주민등록번호
	
	// 수정용(내 empId는 제외)
    boolean existsByEmailAndEmpIdNot(String email, String empId);
    boolean existsByMobileAndEmpIdNot(String mobile, String empId);
	
	// 사원번호 + 부서 + 역할
	@Query("""
			  SELECT e
			  FROM Emp e
			    LEFT JOIN FETCH e.dept d
			    LEFT JOIN FETCH e.empRoles er
			    LEFT JOIN FETCH er.role r
			  WHERE e.empId = :empId
			""")
	Optional<Emp> findByEmpIdWithDeptAndRoles(@Param("empId") String empId);
	
	
	// 사원 목록 조회
	@Query("""
	        select new com.yeoun.emp.dto.EmpListDTO(
	            e.hireDate,
	            e.empId,
	            e.empName,
	            d.deptName,
	            p.posName,
	            p.rankOrder,
	            e.mobile,
	            e.email,
	            e.status,
		        cc.codeName
	        )
	        from Emp e
	          join e.dept d
	          join e.position p
	          left join CommonCode cc
	          	on cc.codeId = e.status
	          	and cc.parent.codeId = 'EMP_STATUS'
	          	and cc.useYn = 'Y'
	        where
	          (e.status is null or e.status in ('ACTIVE', 'LEAVE'))
      	      and ( :keyword is null or :keyword = '' or
	                e.empId    like concat('%', :keyword, '%') or
	                e.empName  like concat('%', :keyword, '%') or
	                d.deptName like concat('%', :keyword, '%') or
	                p.posName  like concat('%', :keyword, '%') or
	                e.email    like concat('%', :keyword, '%')
	          )
	          and ( :deptId is null or :deptId = '' or d.deptId = :deptId )
            order by
			  cc.codeSeq asc,
			  e.hireDate desc
	        """)
	 Page<EmpListDTO> searchEmpList(@Param("keyword") String keyword,
						            @Param("deptId") String deptId,
						            Pageable pageable);
	
	
	// 재직자 공용 조회 (권한관리)
	@Query("""
	    SELECT new com.yeoun.emp.dto.EmpListDTO(
	        e.hireDate,
	        e.empId,
	        e.empName,
	        d.deptName,
	        p.posName,
	        p.rankOrder,
	        e.mobile,
	        e.email
	    )
	    FROM Emp e
	    JOIN e.dept d
	    JOIN e.position p
	    WHERE e.status = 'ACTIVE'
	      AND (:deptId IS NULL OR d.deptId = :deptId)
	      AND (:posCode IS NULL OR p.posCode = :posCode)
	      AND (
	            :keyword IS NULL
	            OR :keyword = ''
	            OR e.empName LIKE concat('%', :keyword, '%')
	            OR e.empId   LIKE concat('%', :keyword, '%')
	          )
	    ORDER BY d.deptName, p.rankOrder DESC, e.hireDate DESC
	""")
	List<EmpListDTO> searchActiveEmpList(@Param("deptId") String deptId,
									     @Param("posCode") String posCode,
									     @Param("keyword") String keyword);
	
	// HR 발령 화면용: 재직 + 휴직 조회
	@Query("""
	    SELECT new com.yeoun.emp.dto.EmpListDTO(
	        e.hireDate,
	        e.empId,
	        e.empName,
	        d.deptName,
	        p.posName,
	        p.rankOrder,
	        e.mobile,
	        e.email,
	        e.status,
	        cc.codeName  
	    )
	    FROM Emp e
	    JOIN e.dept d
	    JOIN e.position p
	    LEFT JOIN CommonCode cc
	      ON cc.codeId = e.status
	     AND cc.parent.codeId = 'EMP_STATUS'
	     AND cc.useYn = 'Y'
	    WHERE (e.status IS NULL OR e.status IN ('ACTIVE','LEAVE'))
	      AND (:status IS NULL OR :status = '' OR e.status = :status)
	      AND (:deptId  IS NULL OR :deptId  = '' OR d.deptId  = :deptId)
	      AND (:posCode IS NULL OR :posCode = '' OR p.posCode = :posCode)
	      AND (
	            :keyword IS NULL OR :keyword = '' OR
	            e.empName LIKE concat('%', :keyword, '%') OR
	            e.empId   LIKE concat('%', :keyword, '%')
	          )
	    ORDER BY cc.codeSeq ASC, d.deptName, e.hireDate DESC
	""")
	List<EmpListDTO> searchEmpForHrAction(@Param("deptId") String deptId,
	                                      @Param("posCode") String posCode,
	                                      @Param("status") String status,
	                                      @Param("keyword") String keyword);


	
	List<Emp> findByEmpIdIn(List<String> approverIds);

	List<Emp> findByDept_DeptId(String deptId);


}
