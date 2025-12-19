package com.yeoun.production.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yeoun.production.dto.ProductionTrendResponseDTO;
import com.yeoun.production.service.ProductionDashboardService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/production")
@RequiredArgsConstructor
public class ProductionDashboardController {
	
	private final ProductionDashboardService productionDashboardService;
	
	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		
        ProductionTrendResponseDTO trend = productionDashboardService.getProductionTrend("day");
        model.addAttribute("trend", trend);
        
		return "/production/dashboard";
	}
	
	// 차트 range(월/주/일) 변경 시 호출하는 API
    @GetMapping("/dashboard/trend")
    @ResponseBody
    public ProductionTrendResponseDTO trend(@RequestParam(name = "range", defaultValue = "day") String range) {
        return productionDashboardService.getProductionTrend(range);
    }

}
