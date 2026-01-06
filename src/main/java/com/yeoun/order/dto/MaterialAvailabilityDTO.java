package com.yeoun.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialAvailabilityDTO {
	private String prdId;			// 제품Id
	private String prdName;			// 제품명
	private String matId;			// 자재Id
	private String matName;			// 자재명
	private String matType;			// 자재타입
	private Double requiredQty;		// 필요수량
	private Double stockQty;		// 재고량

}
