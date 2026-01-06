package com.yeoun.pay.service;

import com.yeoun.pay.dto.PayrollHistoryProjection;
import com.yeoun.pay.repository.PayrollHistoryRepository;
import com.yeoun.pay.repository.PayrollPayslipRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollHistoryService {

    private final PayrollHistoryRepository repo;
    private final PayrollPayslipRepository payslipRepository;

    /**
     * 🔥 관리자용 급여 이력 검색
     * - mode: 검색 모드(지금은 사용 안 함, 확장 가능)
     * - keyword : 사번 또는 이름
     * - deptName : 부서명
     * - year/month → YYYYMM 으로 변환
     *
     * 📌 기능 설명
     * 1. 연도/월 정보가 둘 다 있을 때 "YYYYMM" 문자열 생성
     * 2. PayrollHistoryRepository.searchAll() 호출
     * 3. Projection 기반 결과 반환
     */
    public List<PayrollHistoryProjection> search(
            String mode,
            String keyword,
            String deptName,
            String year,
            String month
    ) {

        // year + month 로 YYYYMM 만들기
        String yymm = (year != null && month != null 
                && !year.isEmpty() && !month.isEmpty())
                ? year + month 
                : null;

        log.info("관리자 검색 실행: keyword={}, dept={}, yymm={}", keyword, deptName, yymm);

        return repo.searchAll(keyword, deptName, yymm);
    }



    /**
     * 🔥 사원 포털용: 본인 급여명세서 전체 목록 조회
     * - empId 한 개로만 조회
     *
     * 예)
     *  empId = "EMP001"
     *  → EMP001 이 가진 모든 급여명세서 목록 조회
     */
    public List<PayrollHistoryProjection> getHistoryByEmpId(String empId) {

        log.info("사원 급여명세서 목록 조회: empId={}", empId);

        return repo.findByEmpId(empId);
    }


    /** payslipID 조회 */
    public Long findPayslipId(String empId, String yymm) {
        return repo.findPayslipId(empId, yymm);
    }
    
    
    
    public Long findConfirmedPayslipId(String empId, String yymm) {
        return payslipRepository.findConfirmedPayslipId(empId, yymm);
    }


}
