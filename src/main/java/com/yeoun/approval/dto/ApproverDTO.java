package com.yeoun.approval.dto;

import org.modelmapper.ModelMapper;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeoun.approval.entity.Approver;

import jakarta.persistence.EntityListeners;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EntityListeners(AuditingEntityListener.class)
public class ApproverDTO {

    @NotBlank(message = "결재자는 필수 입력값입니다!")
    private String empId; //결재자 사원 id
	
	private String delegateStatus; //전결자 상태- 본인,전결,대결,선결
	
	private boolean approvalStatus; //결재 상태 y/n
	
    @NotBlank(message = "결재서류 id는 필수 입력값입니다!")
	private Long approvalId; //결재서류 id
	
    @NotBlank(message = "결재 순서는 필수 입력값입니다!")
	private String orderApprovers; //결재 순서
	
	private String viewing; //결재열람권한 y/n
	
	private static ModelMapper modelMapper = new ModelMapper();
	
	public Approver toEntity() {
		return modelMapper.map(this, Approver.class);
	}
	
	public static ApproverDTO fromEntity(Approver approver) {
		return modelMapper.map(approver, ApproverDTO.class);
	}


    
}
