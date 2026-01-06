package com.yeoun.process.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderProcessDetailDTO {
	
	// 모달 상단
	private WorkOrderProcessDTO wop;
	
	// 공정 단계 리스트
	private List<WorkOrderProcessStepDTO> steps;

}
