package com.yeoun.pay.controller;

import com.yeoun.pay.entity.PayItemMst;
import com.yeoun.pay.repository.PayItemMstRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/pay/rule_item")
@RequiredArgsConstructor
@Log4j2
public class PayItemMstController {

    private final PayItemMstRepository payItemMstRepository;

    /** 공백 문자열을 null 로 변환 (Validation 깔끔하게) */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    /** 급여항목 페이지 */
    @GetMapping
    public String itemPage(Model model,
                           @RequestParam(value = "msg", required = false) String msg,
                           @RequestParam(value = "err", required = false) String err,
                           @RequestParam Map<String, String> params) {

        model.addAttribute("activeTab", "item");

        // 목록(비페이지네이션) — 템플릿의 items 블록과 매칭
        model.addAttribute("items", payItemMstRepository.findAllByOrderBySortNoAsc());

        // 등록 모달 바인딩 객체 — 템플릿의 th:object="${item}"와 매칭
        if (!model.containsAttribute("item")) {
            model.addAttribute("item", new PayItemMst()); // DTO 쓰면 new PayItemForm()으로 교체
        }

        // 검색 파라미터(셀렉트 유지/입력값 유지용)
        model.addAttribute("params", params);

        if (msg != null) model.addAttribute("msg", msg);
        if (err != null) model.addAttribute("err", err);

        return "pay/pay_item";
    }


    /** 등록 */
    @PostMapping
    public String create(@Valid @ModelAttribute("item") PayItemMst item,
                         BindingResult br,
                         RedirectAttributes ra) {

        if (br.hasErrors()) {
            log.warn("create() validation errors: {}", br.getAllErrors());
            ra.addFlashAttribute("err", "항목 입력값을 확인해주세요.");
            ra.addFlashAttribute("openItemCreate", true);
            return "redirect:/pay/rule_item";
        }

        if (payItemMstRepository.existsByItemCode(item.getItemCode())) {
            ra.addFlashAttribute("err", "중복된 ITEM_CODE 입니다.");
            ra.addFlashAttribute("openItemCreate", true);
            return "redirect:/pay/rule_item";
        }

        try {
            payItemMstRepository.save(item);
            ra.addFlashAttribute("msg", "항목이 등록되었습니다.");
        } catch (DataIntegrityViolationException ex) {
            log.error("create() DataIntegrityViolation", ex);
            ra.addFlashAttribute("err", "저장 중 제약조건 위반이 발생했습니다.");
            ra.addFlashAttribute("openItemCreate", true);
        } catch (Exception ex) {
            log.error("create() unexpected", ex);
            ra.addFlashAttribute("err", "저장 중 오류가 발생했습니다.");
            ra.addFlashAttribute("openItemCreate", true);
        }

        return "redirect:/pay/rule_item";
    }

    /** 수정 */
    @PostMapping("/{itemCode}")
    public String update(@PathVariable("itemCode") String itemCode,
                         @Valid @ModelAttribute("item") PayItemMst form,
                         BindingResult br,
                         RedirectAttributes ra) {

        if (br.hasErrors()) {
            log.warn("update() validation errors: {}", br.getAllErrors());
            ra.addFlashAttribute("err", "항목 입력값을 확인해주세요.");
            ra.addFlashAttribute("openItemEdit", itemCode);
            return "redirect:/pay/rule_item";
        }

        try {
            form.setItemCode(itemCode);
            payItemMstRepository.save(form);
            ra.addFlashAttribute("msg", "항목이 수정되었습니다.");
        } catch (DataIntegrityViolationException ex) {
            log.error("update() DataIntegrityViolation", ex);
            ra.addFlashAttribute("err", "수정 중 제약조건 위반이 발생했습니다.");
            ra.addFlashAttribute("openItemEdit", itemCode);
        } catch (Exception ex) {
            log.error("update() unexpected", ex);
            ra.addFlashAttribute("err", "수정 중 오류가 발생했습니다.");
            ra.addFlashAttribute("openItemEdit", itemCode);
        }

        return "redirect:/pay/rule_item";
    }

    /** 삭제 */
    @GetMapping("/delete/{itemCode}")
    public String delete(@PathVariable("itemCode") String itemCode,
                         RedirectAttributes ra) {
        try {
            payItemMstRepository.deleteById(itemCode);
            ra.addFlashAttribute("msg", "항목이 삭제되었습니다.");
        } catch (Exception e) {
            log.error("delete() error, itemCode={}", itemCode, e);
            ra.addFlashAttribute("err", "삭제 중 오류가 발생했습니다.");
        }
        return "redirect:/pay/rule_item";
    }
}
