package com.yeoun.inventory.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    // 재고 리스트 페이지
    @GetMapping("/list")
    public String list() {
        return "inventory/list";
    }

    // 재고 대시보드 페이지
    @GetMapping("/dashboard")
    public String dashboard() {
        return "inventory/dashboard";
    }

    // 재고 이력 페이지
    @GetMapping("/history")
    public String history() {
        return "inventory/history";
    }

    // 재고 실사 페이지
    @GetMapping("/stock-take")
    public String stockTake() {
        return "inventory/stock_take";
    }
}
