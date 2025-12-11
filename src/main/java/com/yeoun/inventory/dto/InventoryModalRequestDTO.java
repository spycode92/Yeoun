package com.yeoun.inventory.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class InventoryModalRequestDTO {
    // -------------------------------------------
	private Long ivId; 
    
	// -------------------------------------------
	// 수량 조절 리퀘스트
    private String adjustType;  // INC / DEC
    private Long adjustQty;
    private String reason;
    
    // -----------------------------------------
    // 재고이동 리퀘스트
    private Long moveLocationId;
    private Long moveAmount;

    // -----------------------------------------
    // 폐기 리퀘스트
    private Long disposeAmount;
}
