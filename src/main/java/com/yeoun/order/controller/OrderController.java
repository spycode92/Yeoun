package com.yeoun.order.controller;

import com.yeoun.order.dto.*;
import com.yeoun.order.service.OrderCommandService;
import com.yeoun.order.service.OrderValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.yeoun.order.dto.WorkOrderDTO;
import com.yeoun.order.dto.WorkOrderListDTO;
import com.yeoun.order.service.OrderQueryService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/order")
@Log4j2
public class OrderController {
	
	private final OrderQueryService orderService;
    private final OrderCommandService orderCommandService;
    private final OrderValidationService orderValidationService;

    // ======================================================
    // 작업지시 목록
    @GetMapping("/list")
    public String list (WorkOrderListDTO dto, Model model){
        List<ProductionPlanViewDTO> plans = orderService.loadAllPlans();
        model.addAttribute("workOrderRequest", new WorkOrderRequest());
        model.addAttribute("plans", plans);		// 생산계획 조회
    	model.addAttribute("prods", orderService.loadAllProducts());	// 품목 조회
    	model.addAttribute("lines", orderService.loadAllLines());		// 라인 조회
        model.addAttribute("workers", 
        						orderService.loadAllWorkers());	// 작업자 조회(작업자)
        model.addAttribute("plansLength", plans.size());
    	return "/order/list";
    }

    // =====================================================
    // 작업지시 목록 조회
    @GetMapping("/list/data")
    @ResponseBody
    public List<WorkOrderListDTO> listData (WorkOrderSearchDTO dto){
        return orderService.loadAllOrders(dto);
    }

    // =====================================================
    // 새 작업지시 등록
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createWorkOrder (
            @Valid @RequestBody WorkOrderRequest req,
            BindingResult bindingResult,
            Authentication auth){

        log.info("dto.... ::::::: here create...." + req);

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }

        orderCommandService.createWorkOrder(req, auth.getName());
        return ResponseEntity.ok().build();
    }

    // =====================================================
    // 작업지시 상세조회
    @GetMapping("/detail/{id}")
    @ResponseBody
    public WorkOrderDetailDTO getWorkOrderDetail (@PathVariable("id") String id){
        return orderService.getDetailWorkOrder(id);
    }

    // =====================================================
    // 작업지시 수정
    @PatchMapping("/modify/{id}")
    public ResponseEntity<?> modifyOrder (@PathVariable("id")String id,
                                          @RequestBody Map<String, String> map){
    	orderCommandService.updateOrder(id, map);
    	return ResponseEntity.ok("updated");
    }
    
    // =====================================================
    // 작업지시 확정 및 취소
    @PatchMapping("/status/{id}")
    public ResponseEntity<?> released (@PathVariable("id") String id, 
    								   @RequestParam("status") String status){
    	orderCommandService.modifyOrderStatus(id, status);
    	return ResponseEntity.ok("updated");
    }
    
    // =====================================================
    // 작업자스케줄 페이지
    @GetMapping("/schedule")
    public String schedule (){
        return "/order/schedule";
    }
    
    // =====================================================
    // 작업자스케줄 로드
    @GetMapping("/schedule/data")
    @ResponseBody
    public List<WorkScheduleDTO> scheduleData() {
    	return orderService.loadAllSchedules();
    }
    
    // =====================================================
    // 작업자목록 로드
    @GetMapping("/workers/data")
    @ResponseBody
    public List<WorkerListDTO> workerData() {
    	return orderService.loadAllWorkers();
    }

    // =====================================================
    // 작업지시 유효성 검증
    @PostMapping("/validate")
    @ResponseBody
    public WorkOrderValidationResult validateWorkOrder (
            @RequestBody WorkOrderValidateRequest req
    ) {
        return orderValidationService.validateAll(req);
    }

    // ========================================
    // 지정한 날짜에 해당하는 작업지시 목록 조회
    @GetMapping("/orderList/data")
    @ResponseBody
    public ResponseEntity<List<WorkOrderDTO>> workList() {
		List<WorkOrderDTO> orderDTOList = orderService.findAllWorkList();
		return ResponseEntity.ok(orderDTOList);
    }
}
