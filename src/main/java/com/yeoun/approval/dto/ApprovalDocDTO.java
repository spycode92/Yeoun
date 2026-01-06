package com.yeoun.approval.dto;

import java.time.LocalDate;

import org.modelmapper.ModelMapper;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeoun.approval.entity.ApprovalDoc;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EntityListeners(AuditingEntityListener.class)
public class ApprovalDocDTO {
	
	private Long approvalId; //결재문서id
	
	@NotBlank(message = "문서제목은 필수 입력값입니다!")
	private String approvalTitle; //문서제목
	
	@NotBlank(message= "사원번호는 필수 입력값입니다!")
	private String empId; //사원번호
	
	@NotBlank(message="생성일자는 필수 입력값입니다!")
	private LocalDate createdDate; //생성일자 
	
	@NotBlank(message="완료예정일자는 필수 입력값입니다!")
	private LocalDate finishDate; //완료예정일자
	
	private String docStatus; //문서상태
	
	@NotBlank(message="양식종류는 필수 입력값입니다!")
	private String formType; //양식종류
	
	@NotBlank(message="결재권한자는 필수 입력값입니다!")
	private String approver; //결재권한자
	
	private LocalDate startDate; //시작휴가날짜 - 휴가(연차신청서)
	
	private LocalDate endDate; //종료휴가날짜 - 휴가(연차신청서)
	
	private String toDeptId; //발령부서 - 인사 발령신청서
	
	private String leaveType; //연차 유형
	
	private String toPosCode;   //직급코드
	
	private String expndType; //지출종류 - 지출결의서
	
	private String reason; //사유 - 자유양식결재서
	
	// ----------------------------------------------------------
	// fromEntity, toEntity 설정
	private static ModelMapper modelMapper = new ModelMapper();
	
	public static ApprovalDocDTO fromEntity(ApprovalDoc approvalDoc) {
		return modelMapper.map(approvalDoc, ApprovalDocDTO.class);
	}
	
	public ApprovalDoc toEntity(ApprovalDocDTO approvalDocDTO) {
		return modelMapper.map(this, ApprovalDoc.class);
	}

}
