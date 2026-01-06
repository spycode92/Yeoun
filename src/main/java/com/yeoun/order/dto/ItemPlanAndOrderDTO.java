package com.yeoun.order.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ItemPlanAndOrderDTO {
    private String itemId;
    private String itemName;
    private int planCnt;
    private int orderCnt;
}
