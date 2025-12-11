package com.yeoun.emp.dto;

import java.time.LocalDate;

import com.yeoun.emp.entity.Emp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EmpListDTO {
	
	private LocalDate hireDate;
    private String empId;
    private String empName;
    private String deptName; // 현재 부서명
    private String posName;  // 현재 직급명
    private Integer rankOrder;  // 직급 서열
    private String mobile;
    private String email;
    
    private String status;     
    private String statusName; 
    
    // Entity → DTO 변환
    public static EmpListDTO fromEntity(Emp emp) {
    	
    	String status = emp.getStatus(); // ACTIVE / LEAVE / RETIRE
    	
    	String statusName = switch (status) {
	        case "ACTIVE" -> "재직";
	        case "LEAVE"  -> "휴직";
	        case "RETIRE" -> "퇴직";
	        default       -> "알수없음";
    	};

        return new EmpListDTO(
            emp.getHireDate(),
            emp.getEmpId(),
            emp.getEmpName(),
            emp.getDept().getDeptName(),
            emp.getPosition().getPosName(),
            emp.getPosition().getRankOrder(),
            emp.getMobile(),
            emp.getEmail(),
            status,
            statusName
        );
    }
    
    // searchActiveEmpList 에서 사용
    public EmpListDTO(LocalDate hireDate,
                      String empId,
                      String empName,
                      String deptName,
                      String posName,
                      Integer rankOrder,
                      String mobile,
                      String email) {

        this.hireDate  = hireDate;
        this.empId     = empId;
        this.empName   = empName;
        this.deptName  = deptName;
        this.posName   = posName;
        this.rankOrder = rankOrder;
        this.mobile    = mobile;
        this.email     = email;

        // 상태는 이 생성자에서는 안 씀 (필요한 화면에서만 fromEntity로 사용)
        this.status     = null;
        this.statusName = null;
    }

    

}
