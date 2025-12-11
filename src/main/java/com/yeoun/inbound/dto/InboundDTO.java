package com.yeoun.inbound.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.modelmapper.ModelMapper;

import com.yeoun.inbound.entity.Inbound;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
public class InboundDTO {
	private String inboundId;
	private LocalDateTime expectArrivalDate; // 예상도착일
	private String inboundStatus; // 입고상태
	private String materialId; // 발주 고유번호
	private String prodId; // 작업지시서 고유번호
	private List<InboundItemDTO> items;
	
	public InboundDTO(String inboundId, LocalDateTime expectArrivalDate, String inboundStatus, String materialId,
			String prodId, List<InboundItemDTO> items) {
		this.inboundId = inboundId;
		this.expectArrivalDate = expectArrivalDate;
		this.inboundStatus = inboundStatus;
		this.materialId = materialId;
		this.prodId = prodId;
		this.items = items;
	}
	
	// ----------------------------------------
	// DTO <-> Entity 변환
	private static ModelMapper modelMapper = new ModelMapper();
	
	public Inbound toEntity() {
		return Inbound.builder()
				.inboundId(inboundId)
				.expectArrivalDate(expectArrivalDate)
				.inboundStatus(inboundStatus)
				.materialId(materialId)
				.prodId(prodId)
				.build();
	}
	
	public static InboundDTO fromEntity(Inbound inbound) {
		return modelMapper.map(inbound, InboundDTO.class);
	}
}
