package com.yeoun.pay.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeoun.emp.entity.Emp;
import com.yeoun.pay.dto.EmpForPayrollProjection;
import com.yeoun.pay.dto.PayCalcStatusDTO;
import com.yeoun.pay.dto.PayslipDetailDTO;
import com.yeoun.pay.entity.EmpPayItem;
import com.yeoun.pay.entity.PayCalcRule;
import com.yeoun.pay.entity.PayItemMst;
import com.yeoun.pay.entity.PayRule;
import com.yeoun.pay.entity.PayrollPayslip;
import com.yeoun.pay.enums.ActiveStatus;
import com.yeoun.pay.enums.CalcStatus;
import com.yeoun.pay.enums.ItemGroup;
import com.yeoun.pay.repository.EmpNativeRepository;
import com.yeoun.pay.repository.EmpPayItemRepository;
import com.yeoun.pay.repository.PayCalcRuleRepository;
import com.yeoun.pay.repository.PayItemMstRepository;
import com.yeoun.pay.repository.PayRuleRepository;
import com.yeoun.pay.repository.PayrollPayslipRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class PayrollCalcService {

    /* -----------------------------------------------------
       10원 단위 절사 → 소수점 없이 정수로 관리하는 핵심 유틸
    ----------------------------------------------------- */
    private static BigDecimal safe(BigDecimal v) {
        if (v == null) return BigDecimal.ZERO;

        // 10원 단위 절사
        BigDecimal tenUnit = v.divide(BigDecimal.TEN, 0, RoundingMode.DOWN)
                              .multiply(BigDecimal.TEN);

        // 소수점 제거(정수)
        return tenUnit.setScale(0, RoundingMode.UNNECESSARY);
    }

    @Getter
    @AllArgsConstructor
    static class AllowanceResult {
        private BigDecimal allowance;
        private BigDecimal incentive;
        private BigDecimal annual;
        private BigDecimal longserv;
        private BigDecimal bunus;
    }


    private final PayrollPayslipRepository payslipRepo;
    private final PayRuleRepository payRuleRepo;
    private final PayItemMstRepository itemRepo;
    private final PayCalcRuleRepository calcRuleRepo;
    private final EmployeeQueryPort employeePort;
    private final EmpPayItemRepository empPayItemRepo;
    private final EmpNativeRepository empNativeRepository;

    private static final JexlEngine JEXL = new JexlBuilder().create();

    @PersistenceContext
    private EntityManager em;
    
    
    /*월 잠금 여부 확인*/
    private void checkMonthLocked(String yyyymm) {

        // 실제 전체 직원 수
        long employeeTotal = empNativeRepository.findActiveEmpList().size();

        // 확정된 건수
        long confirmed = payslipRepo.countByPayYymmAndCalcStatus(yyyymm, CalcStatus.CONFIRMED);

        // 전체 직원이 모두 확정되었을 때만 잠금
        if (confirmed > 0 && confirmed == employeeTotal) {
            throw new IllegalStateException("해당 월은 전체 확정 상태여서 수정할 수 없습니다.");
        }
    }

    /* ========================= 사원 개별 잠금 여부 확인 ========================= */
    private void checkEmployeeEditable(String yyyymm, String empId) {

        Optional<PayrollPayslip> slipOpt = payslipRepo.findByPayYymmAndEmpId(yyyymm, empId);
        if (slipOpt.isEmpty()) return; // 아직 생성 안 된 상태 → 계산 가능

        PayrollPayslip slip = slipOpt.get();

        if (slip.getCalcStatus() == CalcStatus.CONFIRMED) {
            throw new IllegalStateException("해당 사원은 확정 상태여서 수정할 수 없습니다.");
        }
    }


    /* ========================= 전체 시뮬레이션 ========================= */
    @Transactional
    public int simulateMonthly(String yyyymm, boolean overwrite) {

        checkMonthLocked(yyyymm);

        //  확정된(CONFIRMED) 사원은 전체 계산에서 제외한다
        List<String> editableEmpIds =
                empNativeRepository.findActiveEmpList().stream()
                        .map(EmpNativeRepository.EmpSimpleProjection::getEmpId)
                        .filter(empId ->
                                !payslipRepo.findCalcStatus(yyyymm, empId)
                                        .orElse("READY")
                                        .equals("CONFIRMED")
                        )
                        .toList();

        return runMonthlyBatch(yyyymm, overwrite, null, true, null, editableEmpIds);
    }
    

    /* ========================= 개별 시뮬레이션 ========================= */
    @Transactional
    public int simulateOne(String yyyymm, String empId, boolean overwrite) {

        checkMonthLocked(yyyymm);
        checkEmployeeEditable(yyyymm, empId);

        return runMonthlyBatch(yyyymm, overwrite, null, true, empId, null);
    }

    /* ========================= 전체 확정 ========================= */
    @Transactional
    public int confirmMonthly(String yyyymm, boolean overwrite, String userId) {

        checkMonthLocked(yyyymm);

        // 🔥 확정된 사원을 제외
        List<String> editableEmpIds =
                empNativeRepository.findActiveEmpList().stream()
                        .map(EmpNativeRepository.EmpSimpleProjection::getEmpId)
                        .filter(empId ->
                                !payslipRepo.findCalcStatus(yyyymm, empId)
                                        .orElse("READY")
                                        .equals("CONFIRMED"))
                        .toList();

        int calcCnt = runMonthlyBatch(yyyymm, overwrite, null, false, null, editableEmpIds);

        payslipRepo.confirmMonth(
                yyyymm,
                CalcStatus.CONFIRMED,
                optUser(userId),
                LocalDateTime.now()
        );

        return calcCnt;
    }

    /* ========================= 개별 확정 ========================= */
    @Transactional
    public int confirmOne(String yyyymm, String empId, boolean overwrite, String userId) {

        checkMonthLocked(yyyymm);
        checkEmployeeEditable(yyyymm, empId);

        int calcCnt = runMonthlyBatch(yyyymm, overwrite, null, false, empId, null);

        payslipRepo.confirmOne(
                yyyymm,
                empId,
                CalcStatus.CONFIRMED,
                optUser(userId),
                LocalDateTime.now()
        );

        return calcCnt;
    }

    /* =====================================================================
     핵심 계산 함수 (전체·개별 모두 여기로 모임)
    - targetEmpId = null → 전체 계산
    - editableEmpIds = 확정 제외한 사원 목록 (전체 계산일 때만 사용)
   ===================================================================== */
    @Transactional
    public int runMonthlyBatch(String payYymm,
                               boolean overwrite,
                               Long jobId,
                               boolean simulated,
                               String targetEmpId,
                               List<String> editableEmpIds) {

        final CalcStatus status = simulated ? CalcStatus.SIMULATED : CalcStatus.CALCULATED;

        // 1) 급여월 기준 asOf 날짜 먼저 생성
        LocalDate asOf = LocalDate.of(
                Integer.parseInt(payYymm.substring(0,4)),
                Integer.parseInt(payYymm.substring(4,6)),
                1
        ).withDayOfMonth(20); // 급여규칙 적용 기준 날짜

        // 2) 급여 기준정보(PayRule)를 "해당 급여월의 규칙" 기준으로 조회
        List<PayRule> rules = payRuleRepo.findActiveValidRules(
                ActiveStatus.ACTIVE, asOf);

        // 3) 급여항목 마스터 조회
        List<PayItemMst> items = itemRepo.findAll();

        // 4) 급여 계산 규칙(PayCalcRule)도 동일한 asOf 기준으로 조회
        List<PayCalcRule> calcRules = calcRuleRepo.findActiveRules(asOf);

        // 5) 우선순위 정렬
        calcRules.sort(Comparator.comparingInt(PayCalcRule::getPriority));

        
    /* -----------------------------
		    대상 사원 조회
	 ----------------------------- */
        
        
        List<SimpleEmp> employees;

        if (targetEmpId != null && !targetEmpId.isBlank()) {
            // 사원 1명 조회
            SimpleEmp e = employeePort.findOneEmployee(targetEmpId);
            if (e == null) return 0;
            employees = List.of(e);
        } else {
            employees = employeePort.findActiveEmployees();
        }

        if (employees == null || employees.isEmpty())
            return 0;

        int count = 0;

        LocalDate calcDate = LocalDate.parse(payYymm + "01",
                DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDate calcMonthEnd = calcDate.withDayOfMonth(calcDate.lengthOfMonth());

        /* =============================== 
             🔥 사원별 반복 계산 시작 
           =============================== */
        
        
        for (SimpleEmp emp : employees) {
            try {

                if (emp.hireDate() != null && emp.hireDate().isAfter(calcMonthEnd)) {
                    continue;
                }
                //사원개별 재계산 업데이트
                if (!overwrite) {
                    if (payslipRepo.existsByPayYymmAndEmpId(payYymm, emp.empId())) {
                        if (targetEmpId == null) continue; 
                    }
                }
              
             // 🔥 확정된 사원은 전체계산(runMonthlyBatch)에서 제외
                String currentStatus = payslipRepo.findCalcStatus(payYymm, emp.empId())
                        .orElse("READY");

                if (currentStatus.equals("CONFIRMED")) {
                    // 전체(batch)일 때만 제외, 개별(targetEmpId != null) 계산은 허용
                    if (targetEmpId == null) {
                        continue;
                    }
                }


                /* --------------------------------------------
                		기본급 + 수당 + 공제 계산 로직
                -------------------------------------------- */
                BigDecimal baseAmt = calcBase(emp, rules, items, calcRules);
                AllowanceResult ar =
                        calcAllowances(emp, rules, items, calcRules, baseAmt, payYymm);

                BigDecimal alwAmt = ar.getAllowance()
                        .add(ar.getIncentive())
                        .add(ar.getAnnual())
                		.add(ar.getLongserv())
                		.add(ar.getBunus());
                BigDecimal incAmt = ar.getIncentive();
                BigDecimal longserv = ar.getLongserv();   
                BigDecimal dedAmt = calcDeductions(emp, rules, items, calcRules, baseAmt, alwAmt);

                BigDecimal totAmt = baseAmt.add(alwAmt);
                BigDecimal netAmt = totAmt.subtract(dedAmt);

                PayrollPayslip slip = payslipRepo
                        .findByPayYymmAndEmpId(payYymm, emp.empId())
                        .orElse(new PayrollPayslip());

                boolean isNew = (slip.getPayslipId() == null);

                slip.setPayYymm(payYymm);
                slip.setEmpId(emp.empId());
                slip.setDeptId(emp.deptId());
                slip.setBaseAmt(safe(baseAmt));
                slip.setAlwAmt(safe(alwAmt));
                slip.setIncAmt(safe(incAmt));
                slip.setDedAmt(safe(dedAmt));
                slip.setTotAmt(safe(totAmt));
                slip.setNetAmt(safe(netAmt));
                String calcType = simulated
                        ? (targetEmpId == null ? "SIMULATED" : "SIMULATED ONE")
                        : (targetEmpId == null ? "BATCH ALL" : "BATCH ONE");

                slip.setCalcType(calcType);

                slip.setCalcStatus(status);
                slip.setCalcDt(LocalDateTime.now());
                slip.setJobId(jobId);

                if (isNew) {
                    em.persist(slip);
                    em.flush();
                } else {
                    em.merge(slip);
                    em.flush();
                }

                /* 상세 항목 삭제 후 재저장 */
                empPayItemRepo.deleteByPayslipPayslipId(slip.getPayslipId());

                int sort = 1;

                PayRule rule = rules.stream().findFirst().orElse(null);
                if (rule == null) continue;

                BigDecimal mealAmt = BigDecimal.valueOf(
                        rule.getMealAmt() == null ? 0.0 : rule.getMealAmt());

                BigDecimal transAmt = BigDecimal.valueOf(
                        rule.getTransAmt() == null ? 0.0 : rule.getTransAmt());

                BigDecimal total = baseAmt.add(alwAmt);

                BigDecimal penRate = BigDecimal.valueOf(rule.getPenRate());
                BigDecimal hlthRate = BigDecimal.valueOf(rule.getHlthRate());
                BigDecimal empRate = BigDecimal.valueOf(rule.getEmpRate());
                BigDecimal taxRate = BigDecimal.valueOf(rule.getTaxRate());

                /* 지급항목 */
                empPayItemRepo.save(EmpPayItem.builder()
                        .payslip(slip).itemType("ALW")
                        .itemCode("BASE").itemName("기본급")
                        .amount(safe(baseAmt)).sortNo(sort++).build());

                empPayItemRepo.save(EmpPayItem.builder()
                        .payslip(slip).itemType("ALW")
                        .itemCode("MEAL").itemName("식대")
                        .amount(safe(mealAmt)).sortNo(sort++).build());

                empPayItemRepo.save(EmpPayItem.builder()
                        .payslip(slip).itemType("ALW")
                        .itemCode("TRANS").itemName("교통비")
                        .amount(safe(transAmt)).sortNo(sort++).build());

             // 지급: 직급수당
                if (incAmt.compareTo(BigDecimal.ZERO) > 0) {
                    empPayItemRepo.save(EmpPayItem.builder()
                            .payslip(slip)
                            .itemType("ALW")
                            .itemCode("INCENTIVE")
                            .itemName("직급수당")
                            .amount(safe(incAmt))
                            .sortNo(sort++)
                            .build());
                }

                // 지급: 연차수당  
                if (ar.getAnnual().compareTo(BigDecimal.ZERO) > 0) {
                    empPayItemRepo.save(EmpPayItem.builder()
                            .payslip(slip)
                            .itemType("ALW")
                            .itemCode("ANNUAL_PAY")
                            .itemName("연차수당")
                            .amount(safe(ar.getAnnual()))
                            .sortNo(sort++)
                            .build());
                }
                
             // 지급: 근속수당
                if (longserv.compareTo(BigDecimal.ZERO) > 0) {
                    empPayItemRepo.save(EmpPayItem.builder()
                            .payslip(slip)
                            .itemType("ALW")
                            .itemCode("LONGSERV")
                            .itemName("근속수당")
                            .amount(safe(longserv))    
                            .sortNo(sort++)
                            .build());
                }

             // 지급: 보너스
                if (ar.getBunus().compareTo(BigDecimal.ZERO) > 0) {
                    empPayItemRepo.save(EmpPayItem.builder()
                            .payslip(slip)
                            .itemType("ALW")
                            .itemCode("BONUS")
                            .itemName("보너스")
                            .amount(safe(ar.getBunus()))
                            .sortNo(sort++)
                            .build());
                }




                /* 공제항목 */
                BigDecimal pension = safe(total.multiply(penRate));
                BigDecimal health = safe(total.multiply(hlthRate));
                BigDecimal empIns = safe(total.multiply(empRate));
                BigDecimal incomeTax = safe(total.multiply(taxRate));

                empPayItemRepo.save(EmpPayItem.builder()
                        .payslip(slip).itemType("DED")
                        .itemCode("PENSION").itemName("국민연금")
                        .amount(pension).sortNo(sort++).build());

                empPayItemRepo.save(EmpPayItem.builder()
                        .payslip(slip).itemType("DED")
                        .itemCode("HEALTH").itemName("건강보험")
                        .amount(health).sortNo(sort++).build());

                empPayItemRepo.save(EmpPayItem.builder()
                        .payslip(slip).itemType("DED")
                        .itemCode("EMPLOY").itemName("고용보험")
                        .amount(empIns).sortNo(sort++).build());

                empPayItemRepo.save(EmpPayItem.builder()
                        .payslip(slip).itemType("DED")
                        .itemCode("TAX").itemName("소득세")
                        .amount(incomeTax).sortNo(sort++).build());

                count++;

            } catch (Exception e) {
                log.error("[{}] 급여 계산 실패", emp.empId(), e);
            }
        }

        return count;
    }

    private BigDecimal calcBase(SimpleEmp emp,
                                List<PayRule> rules,
                                List<PayItemMst> items,
                                List<PayCalcRule> calcRules) {

        PayRule rule = rules.stream().findFirst().orElse(null);
        if (rule == null) return BigDecimal.ZERO;

        return safe(rule.getBaseAmt());
    }

    /* ========================= 상세 조회 ========================= */
    public PayslipDetailDTO getPayslipDetail(String yyyymm, String empId) {

        PayrollPayslip slip =
                payslipRepo.findByPayYymmAndEmpId(yyyymm, empId)
                        .orElseThrow(() -> new RuntimeException("데이터 없음"));

        String empName = employeePort.getEmpName(empId);
        String deptName = employeePort.getDeptName(slip.getDeptId());

        List<EmpPayItem> items =
                empPayItemRepo.findByPayslipPayslipIdOrderBySortNo(
                        slip.getPayslipId());

        List<PayslipDetailDTO.Item> itemDtos =
                items.stream()
                        .map(it -> PayslipDetailDTO.Item.builder()
                                .itemName(it.getItemName())
                                .amount(it.getAmount())
                                .type(it.getItemType())
                                .build())
                        .toList();

        return PayslipDetailDTO.builder()
                .empId(empId)
                .empName(empName)
                .deptName(deptName)
                .baseAmt(slip.getBaseAmt())
                .alwAmt(slip.getAlwAmt())
                .dedAmt(slip.getDedAmt())
                .netAmt(slip.getNetAmt())
                .confirmUser(slip.getConfirmUser())
                .confirmDate(slip.getConfirmDate())
                .items(itemDtos)
                .build();
    }

    /* ========================= Allowance 계산 ========================= */
    private AllowanceResult calcAllowances(SimpleEmp emp,
            List<PayRule> rules,
            List<PayItemMst> items,
            List<PayCalcRule> calcRules,
            BigDecimal baseAmt,String payYymm) {

    PayRule rule = rules.stream().findFirst().orElse(null);
    if (rule == null) 
        return new AllowanceResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    BigDecimal meal = BigDecimal.valueOf(Optional.ofNullable(rule.getMealAmt()).orElse(0.0));
    BigDecimal trans = BigDecimal.valueOf(Optional.ofNullable(rule.getTransAmt()).orElse(0.0));

    BigDecimal totalAllowance = meal.add(trans);
    BigDecimal incentiveAmt   = BigDecimal.ZERO;   //직급수당
    BigDecimal annualAmt      = BigDecimal.ZERO;   //연차수당
    BigDecimal LONGSERV      = BigDecimal.ZERO;   //근속수당
    BigDecimal bonusAmt = BigDecimal.ZERO;  //보너스
    
 // 🔥 근속년수 계산 (입사일 기준 → 급여 계산 대상 월 기준)
    int yearsOfService = 0;

    if (emp.hireDate() != null) {

        int year = Integer.parseInt(payYymm.substring(0, 4));
        int month = Integer.parseInt(payYymm.substring(4, 6));

        // 해당 월의 마지막 날짜
        LocalDate calcDate = LocalDate.of(year, month, 1)
                                      .withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth());
        
        // 근속연수 계산 (입사일 ~ 해당 계산월 기준)
        yearsOfService = Period.between(emp.hireDate(), calcDate).getYears();
        if (yearsOfService < 0) yearsOfService = 0;
    }


    for (PayCalcRule cr : calcRules) {
    	log.info("[RULE CHECK] itemCode={}, itemGroup={}, targetType={}, targetCode={}",
    	        cr.getItem().getItemCode(),
    	        cr.getItem().getItemGroup(),
    	        cr.getTargetType(),
    	        cr.getTargetCode());


        if (cr.getItem() == null) continue;
        if (cr.getItem().getItemGroup() == null) continue;

        if (!List.of(ItemGroup.ALLOWANCE, ItemGroup.INCENTIVE, ItemGroup.BONUS).contains(cr.getItem().getItemGroup()))
            continue;

        // 대상 조건 체크
        boolean targetPass = switch (cr.getTargetType()) {
            case ALL   -> true;
            case EMP   -> emp.empId().equals(cr.getTargetCode());
            case DEPT  -> emp.deptId().equals(cr.getTargetCode());
            case GRADE -> employeePort.getEmpPosition(emp.empId()).equals(cr.getTargetCode());
        };
        if (!targetPass) continue;
        
     // 보너스 체크 로그
        log.info("[BONUS CHECK] comparing itemCode={} with 'BONUS'", cr.getItem().getItemCode());

        // JEXL 수식 계산
        Map<String, Object> vars = new HashMap<>();
        vars.put("BASE_AMT", baseAmt);
        vars.put("INC_AMT", incentiveAmt);    //직급수당
        vars.put("YEAR_DIFF", yearsOfService); //근속년수
        vars.put("value", cr.getValueNum());
        vars.put("rate", cr.getValueNum());
       
        /*1년 1회 연차수당 계산*/
        String yyyymm = payYymm;  // 이미 runMonthlyBatch에서 넘어온 값
        int calcYear = Integer.parseInt(yyyymm.substring(0, 4));
        int calcMonth = Integer.parseInt(yyyymm.substring(4, 6));

        // 연차수당은 1월 급여에서만 계산됨
        boolean isAnnualPayMonth = (calcMonth == 1);

        // 1월이면 → 작년 remain_days 사용
        int annualRemain = 0;
        if (isAnnualPayMonth) {
            int targetYear = calcYear - 1;
            annualRemain = employeePort.getAnnualRemainForYear(emp.empId(), targetYear);
        }

        vars.put("remain_days", annualRemain);

        
        BigDecimal result;
        try {
            JexlExpression expr = JEXL.createExpression(cr.getCalcFormula());
            result = new BigDecimal(expr.evaluate(new MapContext(vars)).toString());
            result = safe(result); // 10원 절사
        } catch (Exception e) {
            log.error("수당 계산 오류: empId={}, ruleId={}", emp.empId(), cr.getRuleId());
            continue;
        }

        /* --------------------------------------------------------------
             여기서 근속수당, 연차수당, 직급수당, 일반수당 분리를 수행함
        -------------------------------------------------------------- */
        
        //  근속수당 추가 (item_code = LONGSERV)
        if ("LONGSERV".equals(cr.getItem().getItemCode())) {
        	LONGSERV = LONGSERV.add(result);
            continue;
        }

        // 연차수당
        if ("ANNUAL_PAY".equals(cr.getItem().getItemCode())) {
            annualAmt = annualAmt.add(result);
            continue;
        }

     // 직급수당: 단, 보너스(BONUS)는 제외
        if (cr.getTargetType().name().equals("GRADE")
                && !"BONUS".equals(cr.getItem().getItemCode())) {

            incentiveAmt = incentiveAmt.add(result);
            continue;
        }

        
     //   보너스
        if ("BONUS".equals(cr.getItem().getItemCode())) {
        	bonusAmt = bonusAmt.add(result); 
            totalAllowance = totalAllowance.add(result);
            continue;
        }

        // 일반수당
        totalAllowance = totalAllowance.add(result);
    }

    return new AllowanceResult(
            safe(totalAllowance),
            safe(incentiveAmt),
            safe(annualAmt),
            safe(LONGSERV),
            safe(bonusAmt)
    );
}


    /* ========================= 공제 계산 ========================= */
    private BigDecimal calcDeductions(SimpleEmp emp,
                                      List<PayRule> rules,
                                      List<PayItemMst> items,
                                      List<PayCalcRule> calcRules,
                                      BigDecimal baseAmt,
                                      BigDecimal alwAmt) {

        PayRule rule = rules.stream().findFirst().orElse(null);
        if (rule == null) return BigDecimal.ZERO;

        BigDecimal total = baseAmt.add(alwAmt);

        BigDecimal penRate = BigDecimal.valueOf(rule.getPenRate());
        BigDecimal hlthRate = BigDecimal.valueOf(rule.getHlthRate());
        BigDecimal empRate = BigDecimal.valueOf(rule.getEmpRate());
        BigDecimal taxRate = BigDecimal.valueOf(rule.getTaxRate());

        BigDecimal totalDed =
                safe(total.multiply(penRate))
                        .add(safe(total.multiply(hlthRate)))
                        .add(safe(total.multiply(empRate)))
                        .add(safe(total.multiply(taxRate)));

        return safe(totalDed);
    }

    /* ========================= 기타 ========================= */

    private static String optUser(String userId) {
        return (userId == null || userId.isBlank()) ? "SYSTEM" : userId;
    }

    public interface EmployeeQueryPort {
        List<SimpleEmp> findActiveEmployees();
        SimpleEmp findOneEmployee(String empId);
		String getEmpName(String empId);
        String getDeptName(String deptId);
        String getEmpPosition(String empId);
        int getUsedAnnual(String empId);
        int getAnnualRemainForYear(String empId, int year);
    }

    public record SimpleEmp(String empId, String deptId, LocalDate hireDate) {}

    public PayCalcStatusDTO getStatus(String yyyymm) {

        long totalCount      = payslipRepo.countByPayYymm(yyyymm);
        long confirmedCount  = payslipRepo.countByPayYymmAndCalcStatus(yyyymm, CalcStatus.CONFIRMED);
        long simulatedCount  = payslipRepo.countSimulated(yyyymm);
        long calculatedCount = payslipRepo.countCalculated(yyyymm);

        BigDecimal totAmt  = payslipRepo.sumTotalByYymm(yyyymm);
        BigDecimal dedAmt    = payslipRepo.sumDeductByYymm(yyyymm);
        BigDecimal netAmt    = payslipRepo.sumNetByYymm(yyyymm);

        String calcStatus = payslipRepo.findFirstStatusByYyyymm(yyyymm)
                                   .orElse("READY");

        return PayCalcStatusDTO.builder()
                .payYymm(yyyymm)
                .totalCount(totalCount)
                .simulatedCount(simulatedCount)
                .calculatedCount(calculatedCount)
                .confirmedCount(confirmedCount)
                .totAmt(totAmt)
                .dedAmt(dedAmt)
                .netAmt(netAmt)
                .calcStatus(calcStatus)
                .build();


    }

    public static String currentYymm() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
    }
}
