package com.yeoun.pay.controller;

import com.yeoun.emp.repository.DeptRepository;
import com.yeoun.emp.repository.PositionRepository;
import com.yeoun.pay.entity.PayCalcRule;
import com.yeoun.pay.enums.ActiveStatus;
import com.yeoun.pay.enums.RuleType;
import com.yeoun.pay.enums.TargetType;
import com.yeoun.pay.repository.EmpNativeRepository;
import com.yeoun.pay.repository.PayItemMstRepository;
import com.yeoun.pay.service.PayCalcRuleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.yeoun.pay.entity.PayItemMst;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/pay/rule_calc")
@RequiredArgsConstructor
@Log4j2
public class PayCalcRuleController {

    private final PayCalcRuleService service;
    private final PayItemMstRepository itemRepo;
    private final DeptRepository deptRepo;
    private final PositionRepository positionRepo;
    private final EmpNativeRepository empNativeRepository;

    /** 페이지 진입 */
    @GetMapping
    public String page(Model model) {

        model.addAttribute("activeTab", "calc");
        model.addAttribute("calcRules", service.findAllOrderByPriority());
        model.addAttribute("items", itemRepo.findAll());
        model.addAttribute("ruleTypes", RuleType.values());
        model.addAttribute("targetTypes", TargetType.values());
        model.addAttribute("statuses", ActiveStatus.values());
        model.addAttribute("depts", deptRepo.findActive());
        model.addAttribute("positions", positionRepo.findActive());
       


        // 등록 폼 기본값 주입
        if (!model.containsAttribute("createForm")) {
            PayCalcRule blank = PayCalcRule.builder()
                    .startDate(LocalDate.now())
                    .priority(100)
                    .status(ActiveStatus.ACTIVE)
                    .item(new PayItemMst()) 
                    .build();
            model.addAttribute("createForm", blank);
            
        }

        return "pay/pay_calc";
    }

    /** 등록 */
    @PostMapping("/create")
    public String create(@ModelAttribute("createForm") PayCalcRule form,
                         Model model) {

        try {
            service.save(form);
            return "redirect:/pay/rule_calc";

        } catch (Exception e) {
            log.error("등록 오류", e);

            // 모달 다시 열기 + 오류 메시지 표시
            model.addAttribute("createErrorMsg", e.getMessage());
            model.addAttribute("openCreateModal", true);
            model.addAttribute("createForm", form);

            setCommonModel(model);

            return "pay/pay_calc"; // redirect ❌
        }
    }

    /** 수정 */
    @PostMapping("/{id}/update")
    public String update(@PathVariable("id") Long id,
                         @ModelAttribute("editForm") PayCalcRule form,
                         Model model) {

        try {
            form.setRuleId(id);
            service.save(form);
            return "redirect:/pay/rule_calc";

        } catch (Exception e) {
            log.error("수정 오류", e);

            model.addAttribute("editErrorMsg", e.getMessage());
            model.addAttribute("openEditModalId", id);
            model.addAttribute("editForm", form);
            
            if (!model.containsAttribute("createForm")) {
                model.addAttribute("createForm", new PayCalcRule());
            }

            setCommonModel(model);

            return "pay/pay_calc"; // redirect ❌
        }
    }

    /** 삭제 */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            service.delete(id);
            ra.addFlashAttribute("msg", "삭제되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("err", "삭제 중 오류가 발생했습니다.");
        }
        return "redirect:/pay/rule_calc";
    }

    /** 공통 모델 로딩 */
    private void setCommonModel(Model model) {
        model.addAttribute("activeTab", "calc");
        model.addAttribute("calcRules", service.findAllOrderByPriority());
        model.addAttribute("items", itemRepo.findAll());
        model.addAttribute("ruleTypes", RuleType.values());
        model.addAttribute("targetTypes", TargetType.values());
        model.addAttribute("statuses", ActiveStatus.values());
        model.addAttribute("depts", deptRepo.findActive());
        model.addAttribute("positions", positionRepo.findActive());
    }
    
    
    /**사원 자동완성*/
    @GetMapping("/searchEmployee")
    @ResponseBody
    public List<Map<String, String>> searchEmployee(@RequestParam("keyword") String keyword) {

        List<EmpNativeRepository.EmpSimpleProjection> list =
                empNativeRepository.searchActiveEmp(keyword);

        return list.stream()
                .map(e -> Map.of(
                        "empId", e.getEmpId(),
                        "empName", e.getEmpName()
                ))
                .toList();
    }
    
    @GetMapping("/checkPriority")
    @ResponseBody
    public boolean checkPriority(
            @RequestParam("itemCode") String itemCode,
            @RequestParam("priority") int priority,
            @RequestParam(value = "ruleId", required = false) Long ruleId
    ) {
        return service.isPriorityDuplicate(itemCode, priority, ruleId);
    }



}
