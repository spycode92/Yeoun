package com.yeoun.sales.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.masterData.entity.ProductMst;
import com.yeoun.sales.dto.OrderDetailDTO;
import com.yeoun.sales.dto.OrderItemDTO;
import com.yeoun.sales.dto.OrderListDTO;
import com.yeoun.sales.enums.OrderStatus;
import com.yeoun.sales.service.ClientService;
import com.yeoun.sales.service.OrdersService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/sales/orders")
public class OrdersController {

    private final OrdersService ordersService;
    private final ClientService clientService;


    /* ================================
       수주 목록 화면
    ================================= */
    @GetMapping
    public String listPage(
            @RequestParam(value="status", required = false) String status,
            Model model
    ) {
        model.addAttribute("status", status);
        return "sales/orders_list";
    }


    /* ================================
       AG-Grid 목록 조회 API
    ================================= */
    @GetMapping("/list")
    @ResponseBody
    public List<OrderListDTO> list(
            @RequestParam(value ="status", required = false) String status,
            @RequestParam(value ="startDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value ="endDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(value ="keyword",required = false) String keyword
    ) {
        return ordersService.search(status, startDate, endDate, keyword);
    }



    /* ================================
       신규 수주 등록 화면
    ================================= */
    @GetMapping("/new")
    public String createPage(
            Model model,
            @AuthenticationPrincipal LoginDTO login
    ) {
        List<ProductMst> products = ordersService.getProducts();
        
        
        // 🔥 디버깅: 로그 출력
        System.out.println("========== 제품 목록 ==========");
        System.out.println("제품 개수: " + products.size());
        products.forEach(p -> System.out.println(p.getPrdId() + " - " + p.getPrdName()));
        System.out.println("==============================");

        model.addAttribute("products", products);
        model.addAttribute("productList", products);
        model.addAttribute("login", login);

        return "sales/orders_create";
    }



    /* ================================
       거래처 자동완성 API
    ================================= */
    @GetMapping("/search-customer")
    @ResponseBody
    public List<Map<String, String>> searchCustomer(
            @RequestParam(value="keyword", required = false) String keyword
    ) {
        return ordersService.searchCustomer(keyword);
    }



    /* ================================
       신규 수주 저장
    ================================= */
    @PostMapping("/create")
    public String createOrder(
            @RequestParam(name = "clientId") String clientId,
            @RequestParam(name = "orderDate") String orderDate,
            @RequestParam(name = "deliveryDate") String deliveryDate,
            @RequestParam(name = "empId") String empId,

            @RequestParam(name = "managerName", required = false) String managerName,
            @RequestParam(name = "managerTel", required = false) String managerTel,
            @RequestParam(name = "managerEmail", required = false) String managerEmail,

            @RequestParam(name = "dPostcode", required = false) String postcode,
            @RequestParam(name = "dAddress1", required = false) String addr,
            @RequestParam(name = "dAddress2", required = false) String addrDetail,

            @RequestParam(name = "orderMemo", required = false) String orderMemo,

            HttpServletRequest request
    ) {

        ordersService.createOrder(
                clientId,
                orderDate,
                deliveryDate,
                empId,
                managerName,
                managerTel,
                managerEmail,
                postcode,
                addr,
                addrDetail,
                orderMemo,
                request
        );

        return "redirect:/sales/orders";
    }
    
    /* ================================
    생산계획 작성용 — 수주항목 조회 API
    (제품 정보까지 JOIN)
 ================================ */
	 @GetMapping("/order-items")
	 @ResponseBody
	 public List<Map<String, Object>> getOrderItemsForPlan(
	         @RequestParam(required = false) String group
	 ) {
	     return ordersService.getOrderItemsForPlan(group);
 }

	 @GetMapping("/confirmed-items")
	 @ResponseBody
	 public List<OrderItemDTO> getConfirmedOrderItems() {
	     return ordersService.getConfirmedOrderItems();
	 }

	
	 /* ================================
	   수주 상세 페이지
	================================ */
	@GetMapping("/{orderId}")
	public String orderDetailPage(
	        @PathVariable("orderId") String orderId,
	        Model model
	) {
	    OrderDetailDTO detail = ordersService.getOrderDetail(orderId);
	    model.addAttribute("order", detail);
	    return "sales/orders_detail"; // thymeleaf 페이지
	}

	
	 /* ================================
	   수주 상태값 변경
	================================ */
	@PostMapping("/{orderId}/confirm")
    @ResponseBody
    public void confirmOrder(@PathVariable("orderId") String orderId) {
        ordersService.changeStatus(orderId, OrderStatus.CONFIRMED);
    }

    @PostMapping("/{orderId}/cancel")
    @ResponseBody
    public void cancelOrder(@PathVariable("orderId") String orderId) {
        ordersService.changeStatus(orderId, OrderStatus.CANCEL);
    }

}
