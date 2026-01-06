package com.yeoun.pay.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.yeoun.pay.entity.PayrollPayslip;
import com.yeoun.pay.enums.CalcStatus;
import com.yeoun.pay.dto.PayslipViewDTO;

public interface PayrollPayslipRepository extends JpaRepository<PayrollPayslip, Long> {

    /** 특정 사원의 특정월 명세 존재 여부 */
    boolean existsByPayYymmAndEmpId(String payYymm, String empId);

    /** 특정 사원의 특정월 명세 조회 */
    Optional<PayrollPayslip> findByPayYymmAndEmpId(String payYymm, String empId);

    /** ✅ 특정 월 전체 계산 여부 확인 */
    boolean existsByPayYymm(String payYymm);

    /** ✅ 특정 월 전체 건수 */
    long countByPayYymm(String payYymm);
    
    /** 특정 월 확정(CALC_STATUS = CONFIRMED) 건수 */
    long countByPayYymmAndCalcStatus(String payYymm, CalcStatus status);


    /** ✅ 특정 월 총 지급액 합계 */
    @Query("SELECT COALESCE(SUM(p.totAmt), 0) FROM PayrollPayslip p WHERE p.payYymm = :payYymm")
    BigDecimal sumTotalByYymm(@Param("payYymm") String payYymm);

    /** ✅ 특정 월 총 공제액 합계 */
    @Query("SELECT COALESCE(SUM(p.dedAmt), 0) FROM PayrollPayslip p WHERE p.payYymm = :payYymm")
    BigDecimal sumDeductByYymm(@Param("payYymm") String payYymm);

    /** ✅ 특정 월 총 실수령액 합계 */
    @Query("SELECT COALESCE(SUM(p.netAmt), 0) FROM PayrollPayslip p WHERE p.payYymm = :payYymm")
    BigDecimal sumNetByYymm(@Param("payYymm") String payYymm);

    /** ✅ 월 전체 확정 (확정자/확정일 포함) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PayrollPayslip p
           set p.calcStatus = :status,
               p.confirmUser = :userId,
               p.confirmDate = :now
         where p.payYymm = :payYymm
        """)
    int confirmMonth(@Param("payYymm") String payYymm,
                     @Param("status") CalcStatus status,
                     @Param("userId") String userId,
                     @Param("now") LocalDateTime now);

    /** ✅ 특정 사원만 확정 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PayrollPayslip p
           set p.calcStatus = :status,
               p.confirmUser = :userId,
               p.confirmDate = :now
         where p.payYymm = :payYymm
           and p.empId    = :empId
        """)
    int confirmOne(@Param("payYymm") String payYymm,
                   @Param("empId") String empId,
                   @Param("status") CalcStatus status,
                   @Param("userId") String userId,
                   @Param("now") LocalDateTime now);


    /* ✅ 사원이름 + 부서명 포함 전체 조회 (상태 구분 없이) */
    @Query(value = """
    	    SELECT
    	        p.PAYSLIP_ID AS payslipId,
    	        p.PAY_YYMM AS payYymm,
    	        p.EMP_ID AS empId,
    	        e.EMP_NAME AS empName,
    	        p.DEPT_ID AS deptId,
    	        d.DEPT_NAME AS deptName,
    	        p.BASE_AMT AS baseAmt,
    	        p.ALW_AMT AS alwAmt,
    	        p.DED_AMT AS dedAmt,
    	        p.NET_AMT AS netAmt,
    	        p.TOT_AMT AS totAmt,
    	        p.CALC_STATUS AS calcStatus
    	    FROM PAYROLL_PAYSLIP p
    	    LEFT JOIN EMP e ON e.EMP_ID = p.EMP_ID
    	    LEFT JOIN DEPT d ON d.DEPT_ID = p.DEPT_ID
    	    WHERE p.PAY_YYMM = :payYymm
    	      AND (:status IS NULL OR p.CALC_STATUS = :status)
    	    ORDER BY e.EMP_ID
    	""", nativeQuery = true)
    	List<PayslipViewDTO> findPayslipsWithEmpAndDept(
    	        @Param("payYymm") String payYymm,
    	        @Param("status") String status);

    
    /* 전체 계산 대표 상태 조회 */
    /* 전체 계산 상태 — 상태 상관없이 한 건만 조회 */
    @Query(value = """
            SELECT CALC_STATUS
            FROM PAYROLL_PAYSLIP
            WHERE PAY_YYMM = :yyyymm
            FETCH FIRST 1 ROWS ONLY
            """,
            nativeQuery = true)
    Optional<String> findFirstStatusByYyyymm(@Param("yyyymm") String yyyymm);

    
    
    /*개별사원 계산 상태 값 변경*/
    @Query(value = """
            SELECT CALC_STATUS
            FROM PAYROLL_PAYSLIP
            WHERE PAY_YYMM = :yyyymm
              AND EMP_ID = :empId
            """,
            nativeQuery = true)
    Optional<String> findCalcStatus(
            @Param("yyyymm") String yyyymm,
            @Param("empId") String empId
    );
    
    /*계산 갯수 정보 조회*/

    @Query("""
    	    SELECT COUNT(p)
    	      FROM PayrollPayslip p
    	     WHERE p.payYymm = :yyyymm
    	       AND p.calcStatus = 'SIMULATED'
    	""")
    	long countSimulated(@Param("yyyymm") String yyyymm);

    	@Query("""
    	    SELECT COUNT(p)
    	      FROM PayrollPayslip p
    	     WHERE p.payYymm = :yyyymm
    	       AND p.calcStatus = 'CALCULATED'
    	""")
    	long countCalculated(@Param("yyyymm") String yyyymm);
    	
    	
    	/*확정자 정보 조회*/
    	@Query("""
    		    SELECT p.confirmUser, p.confirmDate
    		    FROM PayrollPayslip p
    		    WHERE p.payYymm = :yyyymm
    		    ORDER BY p.confirmDate DESC
    		    """)
    		List<Object[]> findLastConfirmInfo(@Param("yyyymm") String yyyymm);

    
    	/*확정된 사람만 명세서 보여주기*/
    	@Query("""
    		    select p.payslipId
    		    from PayrollPayslip p
    		    where p.payYymm = :yymm
    		      and p.empId = :empId
    		      and p.calcStatus = 'CONFIRMED'
    		""")
    		Long findConfirmedPayslipId(
    		        @Param("empId") String empId,
    		        @Param("yymm") String yymm
    		);

}



