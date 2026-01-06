package com.yeoun.pay.controller;

import com.yeoun.pay.entity.PayRule;
import com.yeoun.pay.enums.ActiveStatus;
import com.yeoun.pay.service.PayRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pay/rule")
@RequiredArgsConstructor
@Log4j2
public class PayRuleController {

    private final PayRuleService payRuleService;

    /** 급여기준정보 페이지 */
    @GetMapping
    public String rulePage(Model model) {

        model.addAttribute("activeTab", "rule");
        
        List<PayRule> sortedRules = payRuleService.findAll()
                .stream()
                .sorted(Comparator.comparing(PayRule::getRuleId))
                .toList();
        
        
        model.addAttribute("rules", payRuleService.findAll());

        // 등록 모달 바인딩 객체 준비
        if (!model.containsAttribute("newRule")) {
            model.addAttribute("newRule", new PayRule());
        }

        return "pay/pay_rule";
    }

    /** 등록 */
    @PostMapping
    public String create(@Valid @ModelAttribute("newRule") PayRule form,
                         BindingResult br,
                         RedirectAttributes ra) {

        if (br.hasErrors()) {
            ra.addFlashAttribute("createErrorMsg", "입력값을 확인하세요.");
            ra.addFlashAttribute("newRule", form);
            ra.addFlashAttribute("openCreateModal", true);
            return "redirect:/pay/rule";
        }

        try {
            payRuleService.save(form);
            ra.addFlashAttribute("pageMsg", "등록되었습니다."); // 화면 상단 메시지

        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("createErrorMsg", e.getMessage());
            ra.addFlashAttribute("newRule", form);
            ra.addFlashAttribute("openCreateModal", true);

        } catch (Exception e) {
            ra.addFlashAttribute("createErrorMsg", "등록 중 오류가 발생했습니다.");
            ra.addFlashAttribute("newRule", form);
            ra.addFlashAttribute("openCreateModal", true);
        }

        return "redirect:/pay/rule";
    }

    /** 수정 */
    @PostMapping("/{id}")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("newRule") PayRule form,
                         BindingResult br,
                         RedirectAttributes ra) {

        if (br.hasErrors()) {
            ra.addFlashAttribute("editErrorMsg", "입력값을 확인하세요.");
            ra.addFlashAttribute("newRule", form);
            ra.addFlashAttribute("openEditModalId", id);
            return "redirect:/pay/rule";
        }

        try {
            form.setRuleId(id);
            payRuleService.update(id, form);
            ra.addFlashAttribute("pageMsg", "수정되었습니다.");

        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("editErrorMsg", e.getMessage());
            ra.addFlashAttribute("newRule", form);
            ra.addFlashAttribute("openEditModalId", id);

        } catch (Exception e) {
            ra.addFlashAttribute("editErrorMsg", "수정 중 오류가 발생했습니다.");
            ra.addFlashAttribute("newRule", form);
            ra.addFlashAttribute("openEditModalId", id);
        }

        return "redirect:/pay/rule";
    }

    /** 상태 변경 */
    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam("value") ActiveStatus value,
                               RedirectAttributes ra) {
        try {
            payRuleService.changeStatus(id, value);
            ra.addFlashAttribute("pageMsg", "상태가 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("pageMsgErr", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("pageMsgErr", "상태 변경 중 오류가 발생했습니다.");
        }
        return "redirect:/pay/rule";
    }

    /** 삭제 */
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            payRuleService.delete(id);
            ra.addFlashAttribute("pageMsg", "삭제되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("pageMsgErr", "삭제 중 오류가 발생했습니다.");
        }
        return "redirect:/pay/rule";
    }
}
