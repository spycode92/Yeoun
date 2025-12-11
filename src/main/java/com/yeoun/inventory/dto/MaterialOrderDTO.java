package com.yeoun.inventory.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.modelmapper.ModelMapper;

import com.yeoun.inventory.entity.MaterialOrder;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class MaterialOrderDTO {
	private String orderId; // 발주Id
	private String clientId; // 거래처Id
	private String status; // 상태
	private String dueDate; // 납기일
	private String empId; // 담당자Id
	private String totalAmount; // 총금액
	private LocalDateTime createdDate; // 생성일
	private List<MaterialOrderItemDTO> items;
	
	private String clientName; 
	private LocalDateTime expectArrivalDate;
	
	// ----------------------------------------
	// DTO <-> Entity 변환
	private static ModelMapper modelMapper = new ModelMapper();
	
	// 엔티티 타입으로 변환
	public MaterialOrder toEntity() {
		return MaterialOrder.builder()
				.orderId(orderId)
				.clientId(clientId)
				.empId(empId)
				.dueDate(dueDate)
				.totalAmount(totalAmount)
				.status("PENDING_ARRIVAL")
				.build();
	}
	
	// DTO 타입으로 변환
	public static MaterialOrderDTO fromEntity(MaterialOrder materialOrder) {
		return modelMapper.map(materialOrder, MaterialOrderDTO.class);
	}

	@Builder
	public MaterialOrderDTO(String orderId, String clientId, String status, String dueDate, String empId,
			String totalAmount, LocalDateTime createdDate, List<MaterialOrderItemDTO> items) {
		this.orderId = orderId;
		this.clientId = clientId;
		this.status = status;
		this.dueDate = dueDate;
		this.empId = empId;
		this.totalAmount = totalAmount;
		this.createdDate = createdDate;
		this.items = items;
	}
}
