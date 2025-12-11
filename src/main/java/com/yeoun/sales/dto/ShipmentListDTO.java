package com.yeoun.sales.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ShipmentListDTO {

    private String orderId;
    private String clientName;
    private String prdName;
    private BigDecimal orderQty;
    private BigDecimal stockQty;
    private LocalDate dueDate;
    private String status;
    private boolean reservable;         // 품목 단위 예약 가능 여부
    private boolean reservableGroup;    // 주문 단위 예약 가능 여부

    public ShipmentListDTO(
            String orderId,
            String clientName,
            String prdName,
            BigDecimal orderQty,
            BigDecimal stockQty,
            Object dueDate,
            String status,
            Object reservable,
            Object reservableGroup
    ) {

        this.orderId = orderId;
        this.clientName = clientName;
        this.prdName = prdName;
        this.orderQty = orderQty;
        this.stockQty = stockQty;

        if (dueDate instanceof java.sql.Timestamp ts) {
            this.dueDate = ts.toLocalDateTime().toLocalDate();
        } else if (dueDate instanceof java.sql.Date d) {
            this.dueDate = d.toLocalDate();
        }

        this.status = status;

        // 품목 단위 예약 가능 여부
        if (reservable instanceof Number n) {
            this.reservable = n.intValue() == 1;
        }

        // ⭐ 주문 단위 예약 가능 여부
        if (reservableGroup instanceof Number n) {
            this.reservableGroup = n.intValue() == 1;
        }
    }
}
