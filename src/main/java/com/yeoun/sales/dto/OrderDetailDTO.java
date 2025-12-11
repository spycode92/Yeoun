package com.yeoun.sales.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailDTO {

    // ====== 수주 기본 정보 ======
    private String orderId;        // 수주번호
    private String clientId;       // 거래처ID
    private String clientName;     // 거래처명
    private LocalDate orderDate;   // 수주일
    private LocalDate deliveryDate; // 납기요청일
    private String orderStatus;    // 상태값 (REQUEST, CONFIRMED 등)

    // ====== 수주 품목 목록 ======
    private List<OrderItemDTO> items;
}
