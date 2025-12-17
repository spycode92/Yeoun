package com.yeoun.sales.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class ShipmentDetailItemDTO {

    private String prdName;
    private BigDecimal orderQty;
    private BigDecimal stockQty;
    private boolean reservable;

    public ShipmentDetailItemDTO(
            String prdName,
            Number orderQty,
            Number stockQty,
            Object reservable
    ) {
        this.prdName = prdName;
        this.orderQty = orderQty == null ? BigDecimal.ZERO : BigDecimal.valueOf(orderQty.doubleValue());
        this.stockQty = stockQty == null ? BigDecimal.ZERO : BigDecimal.valueOf(stockQty.doubleValue());

        if (reservable instanceof Number n) {
            this.reservable = n.intValue() == 1;
        }
    }
}
