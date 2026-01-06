package com.yeoun.production.controller;

import com.yeoun.order.dto.ItemPlanAndOrderDTO;
import com.yeoun.order.dto.PlanAndOrderDashDTO;
import com.yeoun.order.service.OrderQueryService;
import com.yeoun.production.mapper.ProductionDashboardMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yeoun.production.dto.ProductionTrendResponseDTO;
import com.yeoun.production.service.ProductionDashboardService;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Controller
@RequestMapping("/production")
@RequiredArgsConstructor
public class ProductionDashboardController {

    private final OrderQueryService orderQueryService;
    private final ProductionDashboardMapper productionDashboardMapper;
	private final ProductionDashboardService productionDashboardService;
	
	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		
        //ProductionTrendResponseDTO trend = productionDashboardService.getProductionTrend("day");

        int planLength = orderQueryService.loadAllPlans().size();
        int todayPlan = productionDashboardMapper.countTodayProductionPlan();
        int todayPlanned = productionDashboardMapper.countTodayProductionOrder();

        //model.addAttribute("trend", trend);
        model.addAttribute("todayPlan", todayPlan);
        model.addAttribute("todayOrder", productionDashboardMapper.countTodayWorkOrder());
        model.addAttribute("todayPlanned", todayPlanned);
        model.addAttribute("todayDelayedPlan", todayPlan - todayPlanned);
        model.addAttribute("delayedPlan", planLength);
        model.addAttribute("delayedOrder", productionDashboardMapper.countDelayedWorkOrder());
		return "/production/dashboard";
	}

    // 차트 range(월/주/일) 변경 시 호출하는 API
    @GetMapping(
            value = "/orderChart/data",
            produces = "application/json"
    )
    @ResponseBody
    public List<PlanAndOrderDashDTO> orderChart(@RequestParam(name = "range", defaultValue = "day") String range) {
        return productionDashboardService.getOrderChart(range, null);
    }

    // 품목별 생산계획 차트 API
    @GetMapping(
            value = "/itemOrderChart/data",
            produces = "application/json"
    )
    @ResponseBody
    public List<ItemPlanAndOrderDTO> itemOrderChart() {
        return productionDashboardService.getItemOrderChart();
    }

    // 항목별 차트 range(월/주/일) 변경 시 호출하는 API
    @GetMapping(
            value = "/itemChart/data",
            produces = "application/json"
    )
    @ResponseBody
    public List<PlanAndOrderDashDTO> itemChart(
            @RequestParam(name = "itemId") String itemId,
            @RequestParam(name = "range", defaultValue = "day") String range) {
        return productionDashboardService.getOrderChart(range, itemId);
    }
	
//	// 차트 range(월/주/일) 변경 시 호출하는 API (기존)
//    @GetMapping("/dashboard/trend")
//    @ResponseBody
//    public ProductionTrendResponseDTO trend(@RequestParam(name = "range", defaultValue = "DAY") String range) {
//        return productionDashboardService.getProductionTrend(range);
//    }

}
