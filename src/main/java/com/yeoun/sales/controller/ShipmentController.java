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
@RequestMapping("/sales/shipment")
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final ShipmentDetailService shipmentDetailService;

    /* =========================================================
       1️⃣ 출하관리 화면
    ========================================================= */
    @GetMapping
    public String shipmentPage() {
        return "sales/shipment_list";
    }

    /* =========================================================
       2️⃣ 출하 목록 조회 (AJAX)
    ========================================================= */
    @PostMapping("/list")
    @ResponseBody
    public List<ShipmentListDTO> list(@RequestBody Map<String, Object> param) {

        String startDate = (String) param.get("startDate");
        String endDate   = (String) param.get("endDate");
        String keyword   = (String) param.get("keyword");

        Object statusObj = param.get("status");
        List<String> statusList = null;

        if (statusObj instanceof String) {
            statusList = List.of((String) statusObj);
        } else if (statusObj instanceof List<?>) {
            statusList = ((List<?>) statusObj).stream()
                    .map(String::valueOf)
                    .toList();
        }

        return shipmentService.search(startDate, endDate, keyword, statusList);
    }

    /* =========================================================
       3️⃣ 출하 예약
    ========================================================= */
    @PostMapping("/reserve")
    @ResponseBody
    public Map<String, Object> reserve(
            @RequestParam("orderId") String orderId,
            @AuthenticationPrincipal LoginDTO login
    ) {

        String empId = login.getEmpId();
        String shipmentId = shipmentService.reserveShipment(orderId, empId);

        return Map.of(
                "success", true,
                "shipmentId", shipmentId
        );
    }

    /* =========================================================
       4️⃣ 출하 상세 조회
       - orderId 기준
       - Service에서 상태(SHIPPED) 판단 후
         ▷ 출하완료 : LOT / 출하일
         ▷ 그 외   : 기존 상세
    ========================================================= */
    @GetMapping("/detail")
    @ResponseBody
    public ShipmentDetailDTO getShipmentDetail(
            @RequestParam("orderId") String orderId
    ) {
        return shipmentDetailService.getDetail(orderId);
    }

    /* =========================================================
       5️⃣ 출하 예약 취소
    ========================================================= */
    @PostMapping("/cancel")
    @ResponseBody
    public Map<String, Object> cancel(@RequestParam("orderId") String orderId) {
        shipmentService.cancelShipment(orderId);
        return Map.of("success", true);
    }
}
