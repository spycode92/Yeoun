package com.yeoun.notice.dto;

import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;

import com.yeoun.emp.entity.Dept;
import com.yeoun.emp.entity.Emp;
import com.yeoun.main.dto.ScheduleDTO;
import com.yeoun.main.entity.Schedule;
import com.yeoun.notice.entity.Notice;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
@Getter
@Setter
@ToString
public class NoticeDTO {
	private Long noticeId;
	@NotNull(message = "제목은 필수 입력 사항입니다.")
	private String noticeTitle;
	@NotNull(message = "내용은 필수 입력 사항입니다.")
	private String noticeContent;
	
	private String noticeYN;
	private String createdUser;
	private String updatedUser;
	private LocalDateTime createdDate;
	private LocalDateTime updatedDate;
	
	private String empId;
	private String empName;
	private String deptId;
	private String deptName;
	private String updatedEmpId;
	private String updatedEmpName;
	private String updatedDeptId;
	private String updatedDeptName;
	
	
	private static ModelMapper modelMapper = new ModelMapper();
	
	public Notice toEntity() {
		Notice notice = modelMapper.map(this,  Notice.class);
		
		if( this.getCreatedUser() != null) {
			Emp emp = new Emp();
			emp.setEmpId(this.getCreatedUser());
			
			if(this.getDeptId() != null) {
				Dept dept = new Dept();
				dept.setDeptId(this.getDeptId());
				emp.setDept(dept);
			}
			notice.setEmp(emp);
		}
		if( this.getUpdatedUser() != null) {
			Emp emp = new Emp();
			emp.setEmpId(this.getUpdatedUser());
			
			if(this.getDeptId() != null) {
				Dept dept = new Dept();
				dept.setDeptId(this.getDeptId());
				emp.setDept(dept);
			}
			notice.setUpdateEmp(emp);
		}
		return notice;
	}
	
	public static NoticeDTO fromEntity(Notice notice) {
		NoticeDTO noticeDTO = modelMapper.map(notice, NoticeDTO.class);
		noticeDTO.createdUser = notice.getEmp().getEmpId();
		noticeDTO.empId = notice.getEmp().getEmpId();
		noticeDTO.empName = notice.getEmp().getEmpName();
		noticeDTO.deptId = notice.getEmp().getDept().getDeptId();
		noticeDTO.deptName = notice.getEmp().getDept().getDeptName();

		if(notice.getUpdateEmp() != null) {
			noticeDTO.updatedUser = notice.getUpdateEmp().getEmpId();
			noticeDTO.updatedEmpId = notice.getUpdateEmp().getEmpId();
			noticeDTO.updatedEmpName = notice.getUpdateEmp().getEmpName();
			noticeDTO.updatedDeptId = notice.getUpdateEmp().getDept().getDeptId();
			noticeDTO.updatedDeptName = notice.getUpdateEmp().getDept().getDeptName();
		}
		
		return noticeDTO;
	}

	
}
