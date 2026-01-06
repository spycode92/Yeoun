package com.yeoun.approval.dto;


import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ApprovalDocGridDTO {

    @JsonProperty("row_no")  //강제로 변환해줌  
    private Long rowNo;     

    @JsonProperty("approval_id")    
    private String approvalId; 

    @JsonProperty("approval_title") 
    private String approvalTitle; 

    @JsonProperty("form_type")     
    private String formType; 

    @JsonProperty("doc_status")   
    private String docStatus; 

    @JsonProperty("reason")   
    private String reason;

    @JsonProperty("emp_id")        
    private String empId; 

    @JsonProperty("emp_name")       
    private String empName; 

    @JsonProperty("dept_id")       
    private String deptId; 

    @JsonProperty("dept_name")    
    private String deptName;

    @JsonProperty("pos_code")       
    private String posCode; 

    @JsonProperty("pos_name")       
    private String posName; 
   
    @JsonProperty("approver")       
    private String approver; 

    @JsonProperty("approver_name")  
    private String approverName;
   
    @JsonProperty("created_date")  
    private LocalDate createdDate;

    @JsonProperty("finish_date") 
    private LocalDate finishDate;    

    @JsonProperty("start_date")     
    private LocalDate startDate; 

    @JsonProperty("end_date")  
    private LocalDate endDate;

    @JsonProperty("leave_type")  
    private String leaveType; 

    @JsonProperty("to_pos_code")
    private String toPosCode; 

    @JsonProperty("to_dept_id")
    private String toDeptId; 
    
    @JsonProperty("expnd_type")
    private String expndType;
}