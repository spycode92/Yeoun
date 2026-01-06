package com.yeoun.pay.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yeoun.pay.dto.EmpForPayrollProjection;
import com.yeoun.pay.dto.PayCalcStatusDTO;
import com.yeoun.pay.dto.PayslipDetailDTO;
import com.yeoun.pay.dto.PayslipViewDTO;
import com.yeoun.pay.repository.EmpNativeRepository;
import com.yeoun.pay.repository.PayrollPayslipRepository;
import com.yeoun.pay.service.PayCalcStatusService;
import com.yeoun.pay.service.PayrollCalcQueryService;
import com.yeoun.pay.service.PayrollCalcService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
@RequestMapping("/pay/calc")
@RequiredArgsConstructor
public class PayCalcPageController {

    private final PayCalcStatusService statusSvc;
    private final PayrollCalcService payrollCalcService;
    private final PayrollCalcQueryService querySvc;
    private final PayrollPayslipRepository payslipRepo;
    private final PayrollCalcQueryService payrollCalcQueryService;
    private final EmpNativeRepository empNativeRepository;

    /* 최근 12개월 */
    private List<String> getRecentMonths() {
        List<String> months = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 0; i < 12; i++) {
            months.add(now.minusMonths(i).format(DateTimeFormatter.ofPattern("yyyyMM")));
        }
        return months;
    }

    /* 타겟 월 결정 */
    private String getTargetMonth(String yyyymm) {
        return (yyyymm == null || yyyymm.isBlank())
                ? PayrollCalcService.currentYymm()
                : yyyymm;
    }

    /* AJAX - 상태 조회 */
    @GetMapping("/status")
    @ResponseBody
    public PayCalcStatusDTO status(@RequestParam("yyyymm") String yyyymm) {
        return statusSvc.getStatus(yyyymm);
    }

    /* 기본 페이지 → 전체 페이지로 리다이렉트 */
    @GetMapping
    public String redirectToAll(@RequestParam(name = "yyyymm", required = false) String yyyymm,
                                Model model) {
        return pageAll(yyyymm, model);
    }

    /* 급여 계산 메인 선택 화면 */
    @GetMapping("/main")
    public String calcMain() {
        return "pay/pay_calc_main";
    }

    /* 전체 계산 화면 */
    @GetMapping("/all")
    public String pageAll(@RequestParam(name = "yyyymm", required = false) String yyyymm,
                          Model model) {

        String mm = getTargetMonth(yyyymm);

        List<PayslipViewDTO> slips = querySvc.findForView(mm);
        var totals = querySvc.totals(slips);

        model.addAttribute("months", getRecentMonths());
        model.addAttribute("yyyymm", mm);
        model.addAttribute("status", statusSvc.getStatus(mm));
        model.addAttribute("slips", slips);
        model.addAttribute("sumPay", totals[0]);
        model.addAttribute("sumDed", totals[1]);
        model.addAttribute("sumNet", totals[2]);

        return "pay/pay_calc_all";
    }

    /* 사원별 계산 화면 */
    @GetMapping("/emp")
    public String pageEmp(@RequestParam(name = "yyyymm", required = false) String yyyymm,
                          Model model) {

        String mm = getTargetMonth(yyyymm);

        model.addAttribute("months", getRecentMonths());
        model.addAttribute("yyyymm", mm);
        model.addAttribute("empList", empNativeRepository.findActiveEmpList());

        return "pay/pay_calc_emp";
    }

    /* ==============================
     * 전체/개별 가계산(POST)
     * ============================== */
    @PostMapping("/simulate")
    public String simulate(
            @RequestParam(name = "yyyymm") String yyyymm,
            @RequestParam(name = "overwrite", defaultValue = "true") boolean overwrite,
            @RequestParam(name = "empId", required = false) String empId,
            RedirectAttributes ra) {

        try {
            int cnt;
            if (empId != null && !empId.isBlank()) {
                cnt = payrollCalcService.simulateOne(yyyymm, empId, overwrite);
                ra.addFlashAttribute("msg", "사원 " + empId + " 가계산 완료 (" + cnt + "건)");
                return "redirect:/pay/calc/emp?yyyymm=" + yyyymm;
            } else {
                cnt = payrollCalcService.simulateMonthly(yyyymm, overwrite);
                ra.addFlashAttribute("msg", "전체 가계산 완료 (" + cnt + "건)");
                return "redirect:/pay/calc/all?yyyymm=" + yyyymm;
            }

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            if (empId != null && !empId.isBlank())
                return "redirect:/pay/calc/emp?yyyymm=" + yyyymm;
            else
                return "redirect:/pay/calc/all?yyyymm=" + yyyymm;
        }
    }

    /* ==============================
     * 전체/개별 확정(POST)
     * ============================== */
    @PostMapping("/confirm")
    public String confirm(
            @RequestParam(name = "yyyymm") String yyyymm,
            @RequestParam(name = "overwrite", defaultValue = "true") boolean overwrite,
            @RequestParam(name = "empId", required = false) String empId,
            RedirectAttributes ra) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String loginEmpId = auth.getName();
        String loginEmpName = empNativeRepository.findEmpNameByEmpId(loginEmpId);

        try {
            int cnt;

            if (empId != null && !empId.isBlank()) {
                cnt = payrollCalcService.confirmOne(yyyymm, empId, overwrite, loginEmpName);
                ra.addFlashAttribute("msg", "사원 " + empId + " 확정 완료 (" + cnt + "건)");
                return "redirect:/pay/calc/emp?yyyymm=" + yyyymm;

            } else {
                cnt = payrollCalcService.confirmMonthly(yyyymm, overwrite, loginEmpName);
                ra.addFlashAttribute("msg", loginEmpName + " 님이 전체 급여 확정 완료했습니다.");
                return "redirect:/pay/calc/all?yyyymm=" + yyyymm;
            }

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            if (empId != null && !empId.isBlank())
                return "redirect:/pay/calc/emp?yyyymm=" + yyyymm;
            else
                return "redirect:/pay/calc/all?yyyymm=" + yyyymm;
        }
    }

    /* 상세 조회 (AJAX) */
    @GetMapping("/detail")
    @ResponseBody
    public PayslipDetailDTO detail(
            @RequestParam("yyyymm") String yyyymm,
            @RequestParam("empId") String empId) {

        return payrollCalcQueryService.getPayslipDetail(yyyymm, empId);
    }

    /* 사원 정보 조회 (AJAX) */
    @GetMapping("/emp/info")
    @ResponseBody
    public Map<String, Object> getEmpInfo(
            @RequestParam("empId") String empId,
            @RequestParam("yyyymm") String yyyymm
    ) {

        EmpForPayrollProjection emp = empNativeRepository
                .findActiveEmpForPayrollByEmpId(empId)
                .stream()
                .findFirst()
                .orElse(null);

        if (emp == null) return Map.of("error", "NOT_FOUND");

        String calcStatus = payslipRepo
                .findCalcStatus(yyyymm, empId)
                .orElse("READY");

        return Map.of(
                "empId", emp.getEmpId(),
                "empName", emp.getEmpName(),
                "deptName", emp.getDeptName(),
                "posName", emp.getPosName(),
                "calcStatus", calcStatus
        );
    }

    /* ==============================
     * 사원별 가계산 AJAX
     * ============================== */
    @PostMapping("/emp/simulate")
    @ResponseBody
    public Map<String, Object> simulateOneAjax(
            @RequestParam(name = "empId") String empId,
            @RequestParam(name = "yyyymm") String yyyymm
    ) {
        try {
            payrollCalcService.simulateOne(yyyymm, empId, true);
            return Map.of(
                    "success", true,
                    "data", payrollCalcQueryService.getPayslipDetail(yyyymm, empId)
            );

        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", e.getMessage()
            );
        }
    }

    /* ==============================
     * 사원별 확정 AJAX
     * ============================== */
    @PostMapping("/emp/confirm")
    @ResponseBody
    public Map<String, Object> confirmOneAjax(
            @RequestParam(name = "empId") String empId,
            @RequestParam(name = "yyyymm") String yyyymm
    ) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String loginEmpId = auth.getName();
            String loginEmpName = empNativeRepository.findEmpNameByEmpId(loginEmpId);

            payrollCalcService.confirmOne(yyyymm, empId, true, loginEmpName);

            return Map.of(
                    "success", true,
                    "data", payrollCalcQueryService.getPayslipDetail(yyyymm, empId)
            );

        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", e.getMessage()
            );
        }
    }

    /* 사원 검색 자동완성 */
    @GetMapping("/searchEmployee")
    @ResponseBody
    public List<Map<String, String>> searchEmployee(@RequestParam("keyword") String keyword) {

        List<EmpForPayrollProjection> list = empNativeRepository.findActiveEmpForPayroll();
        List<Map<String, String>> result = new ArrayList<>();

        for (EmpForPayrollProjection e : list) {

            boolean match =
                    (e.getEmpName() != null && e.getEmpName().contains(keyword)) ||
                    (e.getEmpId()   != null && e.getEmpId().contains(keyword));

            if (match) {
                result.add(Map.of(
                        "empId", e.getEmpId(),
                        "empName", e.getEmpName()
                ));
            }
        }
        return result;
    }

    @PostMapping("/simulateJson")
    @ResponseBody
    public Map<String, Object> simulateJson(
            @RequestParam("yyyymm") String yyyymm
    ) {
        try {
            int cnt = payrollCalcService.simulateMonthly(yyyymm, true);

            return Map.of(
                    "success", true,
                    "message", "전체 가계산 완료 (" + cnt + "건)"
            );

        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", e.getMessage()
            );
        }
    }

    
    @PostMapping("/confirmJson")
    @ResponseBody
    public Map<String, Object> confirmJson(
            @RequestParam("yyyymm") String yyyymm
    ) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String loginEmpId = auth.getName();
            String loginEmpName = empNativeRepository.findEmpNameByEmpId(loginEmpId);

            int cnt = payrollCalcService.confirmMonthly(yyyymm, true, loginEmpName);

            return Map.of(
                    "success", true,
                    "message", loginEmpName + " 님이 전체 급여 확정 완료 (" + cnt + "건)"
            );

        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", e.getMessage()
            );
        }
    }
    

    
    @GetMapping("/list")
    @ResponseBody
    public List<PayslipViewDTO> getList(@RequestParam("yyyymm") String yyyymm) {
        return querySvc.findForView(yyyymm);
    }



    
}