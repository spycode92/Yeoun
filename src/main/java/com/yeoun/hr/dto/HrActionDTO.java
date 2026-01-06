package com.yeoun.hr.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HrActionDTO {
	
	// 발령ID
	private Long actionId;	
	
	// 사원ID
	private String empId; 	
	
	// 사원 이름
	private String empName; 
	
	// 발령유형 (예: PROMOTION)
	private String actionType; 
	// 발령유형이름 (예: 승진)
	private String actionTypeName;
	
	// 효력일
	private LocalDate effectiveDate; 
	
	// 휴직 종료 예정일
	private LocalDate leaveEndDate;
	
	// 이전 부서명
	private String fromDeptName;
	
	// 이후 부서명
	private String toDeptName;
	
	// 이전 직급명
	private String fromPosName;
	
	// 이후 직금명
	private String toPosName;
	
	// 상태 
	private String status;
	
	// 등록일시
	private LocalDateTime createdDate;
	
	

}
