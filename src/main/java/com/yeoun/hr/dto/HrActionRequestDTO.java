package com.yeoun.hr.dto;

import java.time.LocalDate;

import com.yeoun.emp.entity.Emp;
import com.yeoun.hr.entity.HrAction;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HrActionRequestDTO {
	
	// 대상 사원 ID 
	private String empId;         

	// 발령 구분(코드값)
    private String actionType;    
    
 	// 발령 일자
    private LocalDate effectiveDate;  
    
	// 휴직 종료 예정일
	private LocalDate leaveEndDate;

    // 발령 부서
    private String toDeptId;      
    
    // 발령 직급
    private String toPosCode;    

    // 발령 사유
    private String actionReason; 
    
    // 결재자 사번
//    private String approverEmpId;

    public HrAction toEntity() {
        HrAction action = new HrAction();
        action.setActionType(this.actionType);
        action.setEffectiveDate(this.effectiveDate);
        action.setLeaveEndDate(this.leaveEndDate);
        action.setActionReason(this.actionReason);
        action.setStatus("대기");       
        return action;
    }

}
