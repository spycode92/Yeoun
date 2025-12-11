package com.yeoun.sales.controller;

import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.sales.dto.ShipmentDetailDTO;
import com.yeoun.sales.dto.ShipmentListDTO;
import com.yeoun.sales.service.ShipmentDetailService;
import com.yeoun.sales.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/sales/shipment")   // ✅ 출하 관련 URL은 전부 /sales/shipments 아래로
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final ShipmentDetailService shipmentDetailService;

    /** 1) 출하관리 화면 (GET)  */
    @GetMapping
    public String shipmentPage() {
        // 메뉴에서 th:href="@{/sales/shipments}" 로 연결
        return "sales/shipment_list";
    }

    /** 2) 출하 목록 조회 (GET, AJAX) */
    @PostMapping("/list")
    @ResponseBody
    public List<ShipmentListDTO> list(@RequestBody Map<String, Object> param) {

        String startDate = (String) param.get("startDate");
        String endDate   = (String) param.get("endDate");
        String keyword   = (String) param.get("keyword");
        String status    = (String) param.get("status");

        return shipmentService.search(startDate, endDate, keyword, status);
    }


    /** 3) 출하 예약 (POST) */
    @PostMapping("/reserve")
    @ResponseBody
    public Map<String, Object> reserve(
            @RequestParam("orderId") String orderId,
            @AuthenticationPrincipal LoginDTO login
    ) {

        String empId = login.getEmpId();

        String shipmentId = shipmentService.reserveShipment(orderId, empId);

        return Map.of("success", true, "shipmentId", shipmentId);
    }

    
    @GetMapping("/detail")
    @ResponseBody
    public ShipmentDetailDTO getShipmentDetail(@RequestParam("orderId") String orderId) {
        return shipmentDetailService.getDetail(orderId);
    }



}
