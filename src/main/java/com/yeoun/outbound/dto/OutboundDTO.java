package com.yeoun.outbound.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.modelmapper.ModelMapper;

import com.yeoun.outbound.entity.Outbound;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OutboundDTO {
	private String outboundId; 
	private String requestBy; // 요청자 empId
	private String processBy; // 출고처리자 empId
	private String workOrderId; // 작업지시서
	private String shipmentId; // 출하지시서
	private String status; // 상태
	private LocalDateTime expectOutboundDate; // 출고 예정일
	private LocalDateTime outboundDate; // 출고일
	List<OutboundItemDTO> items; // 출고 품목
	
	@Builder
	public OutboundDTO(String outboundId, String requestBy, String processBy, String workOrderId, String shipmentId,
			String status, LocalDateTime expectOutboundDate, LocalDateTime outboundDate, List<OutboundItemDTO> items) {
		this.outboundId = outboundId;
		this.requestBy = requestBy;
		this.processBy = processBy;
		this.workOrderId = workOrderId;
		this.shipmentId = shipmentId;
		this.status = status;
		this.expectOutboundDate = expectOutboundDate;
		this.outboundDate = outboundDate;
		this.items = items;
	}
	
	// ----------------------------------------
	// DTO <-> Entity 변환
	private static ModelMapper modelMapper = new ModelMapper();
	
	public Outbound toEntity() {
		return Outbound.builder()
				.outboundId(outboundId)
				.requestBy(requestBy)
				.processBy(processBy)
				.workOrderId(workOrderId)
				.shipmentId(shipmentId)
				.status(status)
				.expectOutboundDate(expectOutboundDate)
				.outboundDate(outboundDate)
				.build();
	}
	
	public static OutboundDTO fromEntity(Outbound outbound) {
		return modelMapper.map(outbound, OutboundDTO.class);
	}
	
	
	
}
