package com.yeoun.order.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionPlanViewDTO {
    private String planId;
    private String createdAt;
    private String itemName;
    private int totalQty;
    private int createdQty;
    private int remainingQty;
    private String status;
}
