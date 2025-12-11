package com.yeoun.order.dto;

import java.util.List;

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
public class PlanWithMaterialsDTO {
	private ProductionPlanViewDTO plan;
	private List<MaterialAvailabilityDTO> materials;

}
