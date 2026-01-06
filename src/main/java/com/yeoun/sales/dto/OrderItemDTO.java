package com.yeoun.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderItemDTO {

    private Long orderItemId;
    private String orderId;

    private String prdId;
    private String prdName;

    private BigDecimal orderQty;     // 🔥 BigDecimal 권장
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    private String itemMemo;
    private String itemStatus;

    /* ===== 수주 마스터에서 내려오는 정보 ===== */
    private String clientName;
    private String managerName;
    private String managerTel;
    private String managerEmail;

    private LocalDate orderDate;
    private LocalDate deliveryDate;

    private String empName;
    
    public OrderItemDTO(
            Long orderItemId,
            String orderId,
            String prdId,
            String prdName,
            BigDecimal orderQty,
            String clientName,
            String managerName,
            String managerTel,
            String managerEmail,
            LocalDate orderDate,
            LocalDate deliveryDate,
            String empName
    ) {
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.prdId = prdId;
        this.prdName = prdName;
        this.orderQty = orderQty;
        this.clientName = clientName;
        this.managerName = managerName;
        this.managerTel = managerTel;
        this.managerEmail = managerEmail;
        this.orderDate = orderDate;
        this.deliveryDate = deliveryDate;
        this.empName = empName;
    }

}
