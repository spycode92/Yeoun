package com.yeoun.common.dto;

import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;

import com.yeoun.common.entity.Dispose;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class DisposeDTO {
	private Long disposeId; // 폐기ID
	private String lotNo; //로트번호
	private String itemId; // 원자재/부자재, 완제품의 기준정보 고유값
	private String workType; // 작업종류 ( 입고, 재고, 생산 )
	private String empId; // 작업자
	private Long disposeAmount; // 폐기수량
	private String disposeReason; // 폐기이유
	private LocalDateTime createdDate; // 등록 일시
	
	@Builder
	public DisposeDTO(Long disposeId, String lotNo, String itemId, String workType, String empId, Long disposeAmount,
			String disposeReason, LocalDateTime createdDate) {
		this.disposeId = disposeId;
		this.lotNo = lotNo;
		this.itemId = itemId;
		this.workType = workType;
		this.empId = empId;
		this.disposeAmount = disposeAmount;
		this.disposeReason = disposeReason;
		this.createdDate = createdDate;
	}
	
	// ------------------------------------------------------------------------
	private static ModelMapper modelMapper = new ModelMapper();
	
	// DTO -> Entity 변환
	public Dispose toEntity() {
		return modelMapper.map(this, Dispose.class);
	}
	
	// Entity -> DTO 변환
	public static DisposeDTO fromEntity(Dispose dispose) {
		return modelMapper.map(dispose, DisposeDTO.class);
	}
}
