package com.yeoun.pay.controller;

import com.yeoun.pay.dto.EmpPayslipResponseDTO;
import com.yeoun.pay.service.PayrollHistoryService;
import com.yeoun.pay.service.PayslipDetailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/pay/emp_pay")
@RequiredArgsConstructor
@Slf4j
public class EmpPayController {

    private final PayslipDetailService payslipDetailService;
    private final PayrollHistoryService payrollHistoryService;

    /** 🔥 첫 화면: 이번 달 급여명세서 상세 */
    @GetMapping
    public String detailThisMonth(
            @RequestParam(value = "yymm", required = false) String yymm,
            Model model
    ) {

        // 로그인 사원
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String empId = auth.getName();

        // 기본값: 이번달 (yymm 없거나 null이거나 "null" 문자열인 경우)
        if (yymm == null || yymm.equals("null") || yymm.isBlank()) {
            yymm = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        }

        // 🔥 prev / next 계산
        LocalDate base = LocalDate.parse(yymm + "01", DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prev = base.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM"));
        String next = base.plusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMM"));

        model.addAttribute("prev", prev);
        model.addAttribute("next", next);

        // payslipId 찾기
        Long payslipId = payrollHistoryService.findConfirmedPayslipId(empId, yymm);


        if (payslipId == null) {
            model.addAttribute("header", null);
            model.addAttribute("items", null);
            model.addAttribute("yymm", yymm);
            model.addAttribute("error", "해당 월의 급여명세서가 없습니다.");
            return "pay/emp_payDetail";
        }

        // 정상 데이터
        EmpPayslipResponseDTO result = payslipDetailService.getDetail(payslipId);
        
        model.addAttribute("payslipId", payslipId);
        model.addAttribute("empId", empId);
        model.addAttribute("header", result.getHeader());
        model.addAttribute("items", result.getItems());
        model.addAttribute("yymm", yymm);

        return "pay/emp_payDetail";
    }

}
