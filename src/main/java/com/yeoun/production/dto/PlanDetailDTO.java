package com.yeoun.production.dto;

import java.util.List;
import java.util.Map;

import com.yeoun.production.entity.ProductionPlan;
import com.yeoun.production.entity.ProductionPlanItem;
import com.yeoun.sales.dto.OrderItemDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PlanDetailDTO {

    private String planId;
    private String createdAt;
    private String itemName;   
    private Integer planQty;   
    private String status;
    private String memo;
    private String createdByName;


    private List<ProductionPlanItemDTO> planItems;
    private Map<String, List<OrderItemDTO>> orderItemMap;
}
