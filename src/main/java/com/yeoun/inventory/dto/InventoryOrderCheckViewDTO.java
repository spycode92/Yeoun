package com.yeoun.inventory.dto;

import org.modelmapper.ModelMapper;

import com.yeoun.inventory.entity.InventoryOrderCheckView;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class InventoryOrderCheckViewDTO {
	private String itemId; // mat_id, prod_id
	private String itemName; // 원자재, 상품 명
	private String itemUnit; // 단위
	
	private Long expectIvQty; // 예상 재고 수량( 재고수량 - 출고예정수량)
	private Long productPlanQty; // 완료되지 않은 생산계획 상품의 BOM기반 원,부자재 수량
	private Long outboundPlanQty; // 생산계획수량 중 작업지시가 일어나서 출고된 원,부자재 수량
	private Long safetyQty; // 안전재고 수량
	private Long expectIbQty; // 입고예정수량(입고완료 처리 되지 않은 입고상품수량)
	
	
	// ------------------------------------------------------------------------
	private static ModelMapper modelMapper = new ModelMapper();
	
    public static InventoryOrderCheckViewDTO fromEntity(InventoryOrderCheckView entity) {
        return modelMapper.map(entity, InventoryOrderCheckViewDTO.class);
    }
}
