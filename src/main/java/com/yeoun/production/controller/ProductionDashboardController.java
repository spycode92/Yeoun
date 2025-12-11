package com.yeoun.production.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.yeoun.production.dto.ProductionDashboardDTO;
import com.yeoun.production.service.ProductionDashboardService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/production")
@RequiredArgsConstructor
public class ProductionDashboardController {
	
	private final ProductionDashboardService productionDashboardService;
	
	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		
		// 1) 서비스에서 대시보드용 데이터 한 번에 조회
		ProductionDashboardDTO dashboard = productionDashboardService.getDashboardData();
		
		model.addAttribute("dashboard", dashboard);
				
		return "/production/dashboard";
	}
	

}
