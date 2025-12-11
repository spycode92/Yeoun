package com.yeoun.sales.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {

    private Long orderItemId;
    private String orderId;
    private String prdId;
    private String prdName;
    private Integer orderQty;

    private String clientName;   // 거래처명
    private String managerName;  // 담당자명 ⭐추가
    private String managerTel;   // 연락처 ⭐추가
    private String managerEmail; // 이메일 ⭐추가

    private LocalDate orderDate;     // 수주일자
    private LocalDate deliveryDate;  // 납기일
    
    private String empName;   // ⭐ 수주 담당자명 (내부 사용자)

}

