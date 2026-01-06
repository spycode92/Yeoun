package com.yeoun.pay.repository;

import com.yeoun.emp.entity.Emp;
import com.yeoun.pay.dto.EmpForPayrollProjection;
import com.yeoun.pay.dto.EmpPayslipDetailDTO;
import com.yeoun.pay.entity.PayCalcRule;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmpNativeRepository extends JpaRepository<Emp, String> {
  
	/** 활성화 부서, 전체 사원 조회*/
	 @Query(value = """
		        SELECT
		            e.EMP_ID      AS empId,
		            e.EMP_NAME    AS empName,		            
		            e.STATUS      AS status,
		            e.HIRE_DATE   AS hireDate,
		            e.DEPT_ID     AS deptId
		        FROM EMP e
		        JOIN DEPT d
		          ON d.DEPT_ID = e.DEPT_ID
		         AND d.USE_YN = 'Y'
		        WHERE e.STATUS = 'ACTIVE'
		        ORDER BY e.EMP_ID
		        """, nativeQuery = true)
		    List<EmpForPayrollProjection> findActiveEmpForPayroll();
	 
	 /** 특정 사원 1명 급여계산용 조회   */
	 @Query(value = """
	         SELECT
	             e.EMP_ID      AS empId,
	             e.EMP_NAME    AS empName,	            
	             e.STATUS      AS status,
	             e.HIRE_DATE   AS hireDate,
	             e.DEPT_ID     AS deptId,
	             d.DEPT_NAME   AS deptName,
	             e.POS_CODE    AS posCode,
	             p.POS_NAME    AS posName
	         FROM EMP e
	         LEFT JOIN DEPT d
	             ON d.DEPT_ID = e.DEPT_ID
	         LEFT JOIN POSITION p
	             ON p.POS_CODE = e.POS_CODE
	         WHERE e.EMP_ID = :empId
	           AND e.STATUS = 'ACTIVE'
	         ORDER BY e.EMP_ID
	         """, nativeQuery = true)
	 List<EmpForPayrollProjection> findActiveEmpForPayrollByEmpId(@Param("empId") String empId);


				


    /** 사원명 조회 */
    @Query(value="""
    		SELECT e.empName 
              FROM Emp e 
              WHERE e.empId = :empId
              """)
    Optional<String> findEmpNameById(@Param("empId") String empId);


    /** ⬅️ 부서명 조회 */
    @Query(value = """
        SELECT d.DEPT_NAME
          FROM EMP e
          JOIN DEPT d ON d.DEPT_ID = e.DEPT_ID
         WHERE e.EMP_ID = :empId
        """, nativeQuery = true)
    Optional<String> findDeptNameById(@Param("empId") String empId);



    /** 확정사원명 조회*/
    @Query(value = """
            SELECT E.EMP_NAME 
            FROM EMP E 
            WHERE E.EMP_ID = :empId
            """, nativeQuery = true)
    String findEmpNameByEmpId(@Param("empId") String empId);
    
    
    /**직급 조회*/
    @Query(value = """
    	    SELECT e.POS_CODE 
    	    FROM EMP e 
    	    WHERE e.EMP_ID = :empId
    	""", nativeQuery = true)
    	Optional<String> findEmpPositionById(@Param("empId") String empId);

    /**사용연차 조회*/
    	@Query(value = """
    	    SELECT NVL(a.remain_days, 0)
    	    FROM ANNUAL_LEAVE a
    	    WHERE a.EMP_ID = :empId
    	""", nativeQuery = true)
    	Optional<Integer> findUsedAnnualById(@Param("empId") String empId);

    	/** 특정 연도의 잔여 연차(remain_days) 조회 */
    	@Query(value = """
    	    SELECT NVL(a.remain_days, 0)
    	      FROM ANNUAL_LEAVE a
    	     WHERE a.EMP_ID = :empId
    	       AND a.use_year = :year
    	    """, nativeQuery = true)
    	Optional<Integer> findRemainDaysByYear(
    	        @Param("empId") String empId,
    	        @Param("year") int year);

    	 // =========================
        // Projection 내부 인터페이스
        // =========================
        interface EmpSimpleProjection {
            String getEmpId();
            String getEmpName();
            String getDeptName();
            String getPosName();
        }


        /** 사원 선택 리스트 조회 */
        @Query(value = """
                SELECT 
                    e.EMP_ID       AS empId,
                    e.EMP_NAME     AS empName,
                    d.DEPT_NAME    AS deptName,
                    p.POS_NAME     AS posName
                FROM EMP e
                LEFT JOIN DEPT d 
                    ON d.DEPT_ID = e.DEPT_ID
                LEFT JOIN POSITION p
                    ON p.POS_CODE = e.POS_CODE
                WHERE e.STATUS = 'ACTIVE'
                ORDER BY e.EMP_NAME
                """, nativeQuery = true)
        List<EmpSimpleProjection> findActiveEmpList();

        @Query(value = """
                SELECT 
                    e.EMP_ID   AS empId,
                    e.EMP_NAME AS empName
                FROM EMP e
                WHERE e.STATUS = 'ACTIVE'
                  AND (
                        e.EMP_NAME LIKE '%' || :keyword || '%'
                        OR e.EMP_ID LIKE '%' || :keyword || '%'
                      )
                ORDER BY e.EMP_NAME
                FETCH FIRST 10 ROWS ONLY
                """,
                nativeQuery = true)
        List<EmpNativeRepository.EmpSimpleProjection> searchActiveEmp(@Param("keyword") String keyword);





}
