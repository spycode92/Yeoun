package com.yeoun.warehouse.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/inventory/warehouse")
public class WarehouseController {
	
	// 창고 관리 페이지
	@GetMapping
	public String warehouse() {
		return "warehouse/warehouse";
	}
}
