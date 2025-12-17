package com.yeoun.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class WorkerListDTO {
	
	private String empId;
	private String empName;
	private String deptName;
	private String posName;
	private String status;

}
