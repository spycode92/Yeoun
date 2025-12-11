package com.yeoun.production.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlanItemDTO {
    private String planItemId;
    private String prdId;
    private int planQty;
    private int orderQty;
    private String bomStatus;
    private String status;
}
