package com.yeoun.inbound.dto;

import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;

import com.yeoun.inbound.entity.InboundItem;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InboundItemDTO {
	private Long InboundItemId;
	private String lotNo;
	private String inboundId;
	private String itemId;
	private Long requestAmount;
	private Long inboundAmount;
	private Long disposeAmount;
	private LocalDateTime manufactureDate;
	private LocalDateTime expirationDate;
	private String itemType;
	private String locationId;
	
	@Builder
	public InboundItemDTO(Long inboundItemId, String lotNo, String inboundId, String itemId, Long requestAmount,
			Long inboundAmount, Long disposeAmount, LocalDateTime manufactureDate, LocalDateTime expirationDate,
			String itemType, String locationId) {
		this.lotNo = lotNo;
		this.inboundId = inboundId;
		this.itemId = itemId;
		this.requestAmount = requestAmount;
		this.inboundAmount = inboundAmount;
		this.disposeAmount = disposeAmount;
		this.manufactureDate = manufactureDate;
		this.expirationDate = expirationDate;
		this.itemType = itemType;
		this.locationId = locationId;
	}
	
	// ----------------------------------------
	// DTO <-> Entity 변환
	private static ModelMapper modelMapper = new ModelMapper();
	
	public InboundItem toEntity() {
		return modelMapper.map(this, InboundItem.class);
	}
	
	public static InboundItemDTO fromEntity(InboundItem inboundItem) {
		return modelMapper.map(inboundItem, InboundItemDTO.class);
	}
}
