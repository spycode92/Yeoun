package com.yeoun.inventory.dto;

import org.modelmapper.ModelMapper;

import com.yeoun.inventory.entity.MaterialOrderItem;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class MaterialOrderItemDTO {
	private Long orderItemId; //발주품목Id
	private String orderId; // 발주Id
	private Long itemId; // 발주상품Id
	private Long orderAmount; // 발주량
	private Long unitPrice; // 단가
	private Long supplyAmount; // 공급가액
	private Long VAT; // 부가세
	private Long totalPrice; //총금액
	
	private String matName;     // 자재 이름
	
	@Builder
	public MaterialOrderItemDTO(String orderId, Long itemId, Long orderAmount, Long unitPrice,
			Long supplyAmount, Long VAT, Long totalPrice) {
		this.orderId = orderId;
		this.itemId = itemId;
		this.orderAmount = orderAmount;
		this.unitPrice = unitPrice;
		this.supplyAmount = supplyAmount;
		this.VAT = VAT;
		this.totalPrice = totalPrice;
	}
	
	// ----------------------------------------
	// DTO <-> Entity 변환
	private static ModelMapper modelMapper = new ModelMapper();
	
	public MaterialOrderItem toEntity() {
		return modelMapper.map(this, MaterialOrderItem.class);
	}
	
	public static MaterialOrderItemDTO fromEntity(MaterialOrderItem materialOrderItem) {
		return modelMapper.map(materialOrderItem, MaterialOrderItemDTO.class);
	}
}
