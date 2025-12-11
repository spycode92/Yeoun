package com.yeoun.order.controller;

import com.yeoun.order.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import com.yeoun.order.service.OrderService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/order")
@Log4j2
public class OrderController {
	
	private final OrderService orderService;

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

        orderService.createWorkOrder(req, auth.getName());
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
    // 작업지시 확정
    @PatchMapping("/status/{id}")
    public ResponseEntity<?> released (@PathVariable("id") String id, @RequestParam("status") String status){
    	orderService.modifyOrderStatus(id, status);
    	return ResponseEntity.ok("updated");
    }
    
    // =====================================================
    // 작업자스케줄 페이지
    @GetMapping("/schedule")
    public String schedule (){
    	orderService.selectAllWorkers();
        return "/order/schedule";
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
