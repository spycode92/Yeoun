package com.yeoun.sales.dto;

import java.time.LocalDate;

import com.yeoun.sales.enums.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderListDTO {
    private String orderId;
    private String clientName;
    private LocalDate orderDate;
    private LocalDate deliveryDate;
    private OrderStatus orderStatus; 
    private String managerName;
    private String memo;

}

