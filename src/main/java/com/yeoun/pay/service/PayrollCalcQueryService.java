package com.yeoun.pay.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.yeoun.pay.dto.PayslipDetailDTO;
import com.yeoun.pay.dto.PayslipItemDTO;
import com.yeoun.pay.dto.PayslipViewDTO;
import com.yeoun.pay.entity.EmpPayItem;
import com.yeoun.pay.entity.PayrollPayslip;
import com.yeoun.pay.repository.EmpPayItemRepository;
import com.yeoun.pay.repository.PayrollPayslipRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PayrollCalcQueryService {

    private final PayrollPayslipRepository payslipRepo;
    private final EmpPayItemRepository empPayItemRepo;


    /** 화면 표시용 급여명세서 조회 */
    public List<PayslipViewDTO> findForView(String yyyymm) {
        return payslipRepo.findPayslipsWithEmpAndDept(yyyymm, null);
    }

    /** 합계 계산 */
    public long[] totals(List<PayslipViewDTO> list) {
        long totPay = 0L, totDed = 0L, net = 0L;
        for (PayslipViewDTO p : list) {
            totPay += n(p.getTotAmt());
            totDed += n(p.getDedAmt());
            net += n(p.getNetAmt());
        }
        return new long[]{totPay, totDed, net};
    }

    /** BigDecimal → long 변환 */
    private long n(BigDecimal v) {
        return (v == null) ? 0L : v.longValue();
    }


    /** ============================================================
     *  급여내역 상세보기
     * ============================================================ */
    public PayslipDetailDTO getPayslipDetail(String yyyymm, String empId) {

        // 1) PAYROLL_PAYSLIP 조회
        PayrollPayslip payslip = payslipRepo
                .findByPayYymmAndEmpId(yyyymm, empId)
                .orElseThrow(() -> new IllegalArgumentException("급여계산이 불가능합니다"));

        Long payslipId = payslip.getPayslipId();


        // 2) 상세항목 전체 조회
        List<EmpPayItem> allItems = empPayItemRepo
                .findByPayslipPayslipIdOrderBySortNo(payslipId);


        // 3) 지급 항목(ALW)
        List<PayslipItemDTO> payItems = allItems.stream()
                .filter(i -> "ALW".equals(i.getItemType()))
                .map(i -> new PayslipItemDTO(
                        i.getItemName(),
                        i.getAmount(),
                        i.getSortNo()
                ))
                .toList();


        // 4) 공제 항목(DED)
        List<PayslipItemDTO> dedItems = allItems.stream()
                .filter(i -> "DED".equals(i.getItemType()))
                .map(i -> new PayslipItemDTO(
                        i.getItemName(),
                        i.getAmount(),
                        i.getSortNo()
                ))
                .toList();


        // 5) 최종 DTO 반환
        return PayslipDetailDTO.builder()
        		.payYymm(payslip.getPayYymm()) 
                .empId(payslip.getEmpId())
                .empName(payslip.getEmpName())  
                .deptId(payslip.getDeptId())
                .deptName(payslip.getDept() != null ? payslip.getDept().getDeptName() : null)
                .posName(payslip.getEmp() != null &&  payslip.getEmp().getPosition() != null
                	        ? payslip.getEmp().getPosition().getPosName() : null	)

                .baseAmt(payslip.getBaseAmt())
                .alwAmt(payslip.getAlwAmt())
                .incAmt(payslip.getIncAmt())
                .dedAmt(payslip.getDedAmt())
                .totAmt(payslip.getTotAmt())
                .netAmt(payslip.getNetAmt())
                .confirmUser(payslip.getConfirmUser())
                .confirmDate(payslip.getConfirmDate())

                .payItems(payItems)
                .dedItems(dedItems)
                .build();
    }
    
   
}
