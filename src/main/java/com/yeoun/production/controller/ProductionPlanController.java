package com.yeoun.production.controller;

import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.production.dto.PlanCreateRequestDTO;
import com.yeoun.production.dto.PlanDetailDTO;
import com.yeoun.production.dto.ProductionPlanListDTO;
import com.yeoun.production.entity.ProductionPlan;
import com.yeoun.production.entity.ProductionPlanItem;
import com.yeoun.production.service.ProductionPlanService;
import com.yeoun.sales.dto.OrderItemDTO;
import com.yeoun.sales.dto.OrderPlanSuggestDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/production")
public class ProductionPlanController {

    private final ProductionPlanService planService;


    /* ============================
       1) 생산계획 목록 페이지
       ============================ */
    @GetMapping("/plan")
    public String planPage() {
        return "production/plan_list";
    }


    /* ============================
       2) 생산계획 목록 데이터(JSON)
       ============================ */
    @GetMapping("/list")
    @ResponseBody
    public List<ProductionPlanListDTO> getPlanList() {
        return planService.getPlanList();
    }


    /* ============================
       3) 생산계획 작성 페이지
       ============================ */
    @GetMapping("/create")
    public String planCreatePage() {
        return "production/plan_create";
    }


    /* ============================
       4) 수동 생산계획 생성
       ============================ */
    @PostMapping("/create/submit")
    @ResponseBody
    public Map<String, Object> createPlan(
            @RequestBody PlanCreateRequestDTO request,
            @AuthenticationPrincipal LoginDTO login
    ) {

        String planId = planService.createPlan(
                request.getItems(),
                login.getEmpId(),
                request.getMemo()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("planId", planId);

        return response;
    }


    /* ============================
       5) 생산 추천 목록 조회(JSON)
       ============================ */
    @GetMapping("/suggest")
    @ResponseBody
    public List<OrderPlanSuggestDTO> getPlanSuggestions(
            @RequestParam(value = "group", required = false) String group
    ) {
        return planService.getPlanSuggestions(group);
    }


    /* ============================
       6) 자동 생산계획 생성
       ============================ */   
	@PostMapping("/plan/auto-create")
	@ResponseBody
	public Map<String, Object> autoCreatePlan(
	        @RequestBody Map<String, Object> req,
	        @AuthenticationPrincipal LoginDTO login
	) {
	
	    Map<String, Object> result = new HashMap<>();
	
	    try {
	
	        // 1) 요청 데이터 파싱
	        List<Map<String, Object>> requestList =
	                (List<Map<String, Object>>) req.get("requestList");
	
	        String memo = (String) req.get("memo");  // 🔥 메모 받기
	
	        // 2) 서비스 호출 (memo 포함)
	        String planIds = planService.createAutoPlan(
	                requestList,
	                login.getEmpId(),
	                memo
	        );
	
	        result.put("success", true);
	        result.put("planIds", planIds);
	
	    } catch (Exception e) {
	        result.put("success", false);
	        result.put("message", e.getMessage());
	    }
	
	    return result;
	}

    
    /* ============================
    7) 생산계획 상세 모달
    ============================ */
    @GetMapping("/plan/detail/{planId}")
    @ResponseBody
    public PlanDetailDTO getPlanDetail(@PathVariable("planId") String planId) {

        PlanDetailDTO dto = planService.getPlanDetailForModal(planId);

        // ================================
        // 🔍 디버깅용 로그 추가
        // ================================
        System.out.println("====== [PLAN_DETAIL_RESPONSE] ======");
        System.out.println("PlanId: " + dto.getPlanId());
        System.out.println("PlanItems: " + dto.getPlanItems());
        System.out.println("OrderItemMap: " + dto.getOrderItemMap());
        System.out.println("====================================");

        return dto;
    }

    /* ============================
    8) 추천 목록 → 제품별 수주 상세 조회
    ============================ */
	 @GetMapping("/order-items/{prdId}")
	 @ResponseBody
	 public List<OrderItemDTO> getOrderItemsByProduct(@PathVariable("prdId") String prdId) {
	     return planService.getOrderItemsByProduct(prdId);
 }


	 /* ============================
     9) 🔥 생산계획 취소
     ============================ */
  @PostMapping("/plan/{planId}/cancel")
  @ResponseBody
  public Map<String, Object> cancelProductionPlan(
          @PathVariable("planId") String planId,
          @AuthenticationPrincipal LoginDTO login
  ) {

      Map<String, Object> result = new HashMap<>();

      try {
          planService.cancelProductionPlan(planId, login.getEmpId());
          result.put("success", true);
          result.put("message", "생산계획이 취소되었습니다.");
      } catch (Exception e) {
          result.put("success", false);
          result.put("message", e.getMessage());
      }

      return result;
  }

}
