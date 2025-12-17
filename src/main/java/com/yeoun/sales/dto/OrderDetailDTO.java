package com.yeoun.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.yeoun.sales.enums.OrderStatus;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderDetailDTO {

    private String orderId;

    private String clientId;
    private String clientName;

    private LocalDate orderDate;
    private LocalDate deliveryDate;

    private String orderStatus;

    private String managerName;
    private String managerTel;
    private String managerEmail;

    private String postcode;
    private String addr;
    private String addrDetail;
    
    private String empId;     
    private String empName;  
       
    private String orderMemo;
    
    /* =========================
    수주 품목
 	========================= */

    private List<OrderItemDTO> items;
    
    private BigDecimal totalAmount;
    
    /* =========================
    ⭐ 출하 이력 
	 ========================= */
	 private List<OrderShipmentHistoryDTO> shipmentHistories;
	}
