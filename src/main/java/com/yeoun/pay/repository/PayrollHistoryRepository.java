package com.yeoun.pay.repository;

import com.yeoun.pay.dto.EmpPayslipDetailDTO;
import com.yeoun.pay.dto.PayrollHistoryProjection;
import com.yeoun.pay.entity.PayrollPayslip;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PayrollHistoryRepository extends JpaRepository<PayrollPayslip, Long> {

    /**
     * 🔥 관리자 통합 검색
     */
    @Query(value = """
        SELECT
            p.EMP_ID AS empId,
            e.EMP_NAME AS empName,
            d.DEPT_NAME AS deptName,
            p.PAY_YYMM AS payYymm,
            p.BASE_AMT AS baseAmt,
            p.ALW_AMT AS alwAmt,
            p.DED_AMT AS dedAmt,
            p.TOT_AMT AS totAmt,
            p.NET_AMT AS netAmt,
            p.CALC_STATUS AS calcStatus
        FROM PAYROLL_PAYSLIP p
          JOIN EMP e ON e.EMP_ID = p.EMP_ID
          LEFT JOIN DEPT d ON d.DEPT_ID = p.DEPT_ID
        WHERE 
            (:keyword IS NULL OR :keyword = '' 
                OR e.EMP_NAME LIKE '%' || :keyword || '%'
                OR e.EMP_ID LIKE '%' || :keyword || '%')
          AND (:deptName IS NULL OR :deptName = '' 
                OR d.DEPT_NAME LIKE '%' || :deptName || '%')
          AND (:yymm IS NULL OR :yymm = '' OR p.PAY_YYMM = :yymm)
        ORDER BY p.PAY_YYMM DESC, e.EMP_ID
        """, nativeQuery = true)
    List<PayrollHistoryProjection> searchAll(
            @Param("keyword") String keyword,
            @Param("deptName") String deptName,
            @Param("yymm") String yymm
    );



    /**
     * 🔥 사원용 목록 조회
     */
    @Query(value = """
        SELECT
            p.EMP_ID AS empId,
            e.EMP_NAME AS empName,
            d.DEPT_NAME AS deptName,
            p.PAY_YYMM AS payYymm,
            p.BASE_AMT AS baseAmt,
            p.ALW_AMT AS alwAmt,
            p.DED_AMT AS dedAmt,
            p.TOT_AMT AS totAmt,
            p.NET_AMT AS netAmt,
            p.CALC_STATUS AS calcStatus
        FROM PAYROLL_PAYSLIP p
          JOIN EMP e ON e.EMP_ID = p.EMP_ID
          LEFT JOIN DEPT d ON d.DEPT_ID = p.DEPT_ID
        WHERE p.EMP_ID = :empId
        ORDER BY p.PAY_YYMM DESC
        """, nativeQuery = true)
    List<PayrollHistoryProjection> findByEmpId(@Param("empId") String empId);



    /**
     * 🔥 급여명세서 상세조회 (사원용)
     */
    @Query(value = """
        SELECT 
            p.EMP_ID AS empId,
            e.EMP_NAME AS empName,
            d.DEPT_NAME AS deptName,
            p.DEPT_ID AS deptId,

            e.POS_CODE AS posCode,          
            pos.POS_NAME AS posName,       

            p.PAY_YYMM AS payYymm,
            p.BASE_AMT AS baseAmt,
            p.ALW_AMT AS alwAmt,
            p.DED_AMT AS dedAmt,
            p.INC_AMT AS incAmt,
            p.TOT_AMT AS totAmt,
            p.NET_AMT AS netAmt,
            p.REMARK AS remark
        FROM PAYROLL_PAYSLIP p
          JOIN EMP e ON e.EMP_ID = p.EMP_ID
          LEFT JOIN DEPT d ON d.DEPT_ID = p.DEPT_ID
          LEFT JOIN POSITION pos ON pos.POS_CODE = e.POS_CODE  
        WHERE p.PAYSLIP_ID = :payslipId
        """, nativeQuery = true)
    EmpPayslipDetailDTO findDetail(@Param("payslipId") Long payslipId);

    
    
    /*특정 사원(empId) + **특정 지급월(yymm)**의 payslipId 조회*/
    @Query("""
            SELECT p.payslipId 
              FROM PayrollPayslip p
             WHERE p.empId = :empId
               AND p.payYymm = :yymm
            """)
    Long findPayslipId(@Param("empId") String empId,
                       @Param("yymm") String yymm);

    
    

}
