package com.yeoun.inventory.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SupplierItemDTO {
	private Long itemId;        // 공급품목Id
	private String clientId;    // 거래처Id
	private String materialId;  // 자재Id                                                                                                                                                                                                                                                                                                                                                                           
	private String matName;     // 자재 이름
	private Integer unitPrice;  // 공급단가
	private Integer leadDays;   // 리드타임(일)
	private String unit;        // 공급단위
	private char supplyAvaliable; // 공급 가능 여부
	private Integer minOrderQty;  // 최소발주단위
	private Integer orderUnit;    // 발주단위
	private Integer orderAmount;  // 주문 수량
	private Integer supplyAmount; // 공급가액
}
