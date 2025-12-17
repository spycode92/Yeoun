package com.yeoun.process.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yeoun.process.dto.LineStayRowDTO;
import com.yeoun.process.dto.ProductionDashboardKpiDTO;
import com.yeoun.process.dto.ProductionTrendResponseDTO;
import com.yeoun.process.dto.StayCellDTO;
import com.yeoun.process.service.ProductionDashboardService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/production")
@RequiredArgsConstructor
public class ProductionDashboardController {
	
	private final ProductionDashboardService productionDashboardService;
	
	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		
		// 1. 상단 KPI
		ProductionDashboardKpiDTO kpi = productionDashboardService.getKpis();
        model.addAttribute("kpi", kpi);
        
        // 2. 라인
        List<LineStayRowDTO> rows = productionDashboardService.getLineStayHeatmap();
        model.addAttribute("heatmapRows", rows);

        // 전체 셀 중 진행중 건수 합
        long heatmapTotal = rows.stream()
            .flatMap(r -> r.getSteps().stream())
            .mapToLong(StayCellDTO::getInProgressCnt)
            .sum();

        model.addAttribute("heatmapTotal", heatmapTotal);
        
        // 3. 차트
        ProductionTrendResponseDTO trend = productionDashboardService.getProductionTrend("day");
        model.addAttribute("trend", trend);
        
        // 4. 즉시 조치 리스트
        model.addAttribute("actions", productionDashboardService.getImmediateActions(10));
        
		return "/process/dashboard";
	}
	
	// 차트 range(월/주/일) 변경 시 호출하는 API
    @GetMapping("/dashboard/trend")
    @ResponseBody
    public ProductionTrendResponseDTO trend(@RequestParam(name = "range", defaultValue = "day") String range) {
        return productionDashboardService.getProductionTrend(range);
    }

}
