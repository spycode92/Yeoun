package com.yeoun.production.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanCreateItemDTO {
	 private Long orderItemId;
	    private String orderId;
	    private String prdId;
	    private String prdName;
	    private BigDecimal orderQty; // 기존 수주 수량 (정보용)
	    private LocalDate dueDate;
	    
	    private int qty; //⭐ 실제 생산계획에 사용할 수량 (프론트에서 오는 값)
    
}
