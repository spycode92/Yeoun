package com.yeoun.approval.entity;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeoun.common.util.FileUtil;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="APPROVAL_DOC")
@SequenceGenerator(
    name = "APPROVAL_DOC_SEQ_GENERATOR",
    sequenceName = "APPROVAL_SEQ",
    allocationSize = 1
)
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class) 
public class ApprovalDoc implements FileUtil.FileUploadHelpper {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "APPROVAL_DOC_SEQ_GENERATOR")
	@Column(name="APPROVAL_ID")
	private Long approvalId; //결재문서id
	
	@Column(name="APPROVAL_TITLE")
	private String approvalTitle; //문서제목
	
	@Column(name="EMP_ID")
	private String empId; //사원번호
	
	@Column(name="CREATED_DATE")
	private LocalDate createdDate; //생성일자 
	
	@Column(name="FINISH_DATE")
	private LocalDate finishDate; //완료예정일자
	
	@Column(name="DOC_STATUS")
	private String docStatus; //문서상태
	
	@Column(name="FORM_TYPE")
	private String formType; //양식종류
	
	@Column(name="APPROVER")
	private String approver; //결재권한자
	
	@Column(name="START_DATE")
	private LocalDate startDate; //시작휴가날짜
	
	@Column(name="END_DATE")
	private LocalDate endDate; //종료휴가날짜

	@Column(name="TO_POS_CODE")
	private String toPosCode;   //직급코드
	
	@Column(name="TO_DEPT_ID")
	private String toDeptId; //발령부서

	@Column(name="LEAVE_TYPE")
	private String leaveType; //연차 유형
	
	@Column(name="EXPND_TYPE")
	private String expndType; //지출종류
	
	@Column(name="REASON",length = 4000)
	private String reason; //사유

	@Override
	public String getTargetTable() {
		return "APPROVAL_DOC";
	}

	@Override
	public Long getTargetTableId() {
		return this.approvalId;
	}

}
