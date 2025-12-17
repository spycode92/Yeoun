package com.yeoun.sales.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;      


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderShipmentHistoryDTO {

    private String prdName;
    private BigDecimal outboundAmount;
    private String lotNo;
    private Timestamp outboundDate;
    private String shipmentId;
    private String shipmentStatus;
}
