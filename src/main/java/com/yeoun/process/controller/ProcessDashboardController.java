package com.yeoun.process.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.yeoun.process.dto.LineStayRowDTO;
import com.yeoun.process.dto.ProductionDashboardKpiDTO;
import com.yeoun.process.dto.StayCellDTO;
import com.yeoun.process.service.ProcessDashboardService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/process")
@RequiredArgsConstructor
public class ProcessDashboardController {
	
	private final ProcessDashboardService processDashboardService;
	
	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		
		// 1. 상단 KPI
		ProductionDashboardKpiDTO kpi = processDashboardService.getKpis();
        model.addAttribute("kpi", kpi);
        
        // 2. 라인
        List<LineStayRowDTO> rows = processDashboardService.getLineStayHeatmap();
        model.addAttribute("heatmapRows", rows);

        // 전체 셀 중 진행중 건수 합
        long heatmapTotal = rows.stream()
            .flatMap(r -> r.getSteps().stream())
            .mapToLong(StayCellDTO::getInProgressCnt)
            .sum();

        model.addAttribute("heatmapTotal", heatmapTotal);
        
        // 4. 즉시 조치 리스트
        model.addAttribute("actions", processDashboardService.getImmediateActions(10));
        
		return "/process/dashboard";
	}
	
}
