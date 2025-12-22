package com.yeoun.order.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class PlanAndOrderDashDTO {
    private String period;
    private String periodLabel;
    private Integer planCnt;
    private Integer orderCnt;
}
