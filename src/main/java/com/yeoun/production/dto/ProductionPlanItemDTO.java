package com.yeoun.production.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductionPlanItemDTO {

    private String planItemId;   // PLAN_ITEM_ID
    private String prdId;        // 제품 ID
    private String prdName;        // 제품 ID
    private Integer planQty;     // 계획 수량
    private String bomStatus;    // BOM 상태
    private String statusLabel;      // 생산계획 상세 상태
}
