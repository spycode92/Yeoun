package com.yeoun.outbound.dto;

import org.modelmapper.ModelMapper;

import com.yeoun.outbound.entity.OutboundItem;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OutboundItemDTO {
	private Long outboundItemId; // 입고대기품목 id
	private String outboundId; // 출고ID 
	private String itemId; // 원자재/부자재, 완제품의 기준정보 고유값
	private String lotNo;
	private Long outboundAmount;
	private String itemType;
	private Long ivId;
	private String locationId;
	
	@Builder
	public OutboundItemDTO(Long outboundItemId, String outboundId, String itemId, String lotNo, Long outboundAmount,
			String itemType, Long ivId, String locationId) {
		this.outboundId = outboundId;
		this.itemId = itemId;
		this.lotNo = lotNo;
		this.outboundAmount = outboundAmount;
		this.itemType = itemType;
		this.ivId = ivId;
		this.locationId = locationId;
	}
	
	// ----------------------------------------
	// DTO <-> Entity 변환
	private static ModelMapper modelMapper = new ModelMapper();
	
	public OutboundItem toEntity() {
		return modelMapper.map(this, OutboundItem.class);
	}
	
	public static OutboundItemDTO fromEntity(OutboundItem outboundItem) {
		return modelMapper.map(outboundItem, OutboundItemDTO.class);
	}
}
