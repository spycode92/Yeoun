package com.yeoun.sales.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentDetailItemDTO {

    private String prdName;
    private BigDecimal orderQty;
    private BigDecimal stockQty;
    private boolean reservable;
}
