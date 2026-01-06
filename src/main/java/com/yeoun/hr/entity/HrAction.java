package com.yeoun.hr.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeoun.emp.entity.Dept;
import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.entity.Position;
import com.yeoun.approval.entity.ApprovalDoc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "HR_ACTION")
@SequenceGenerator(
		name = "HR_ACTION_SEQ_GENERATOR",
		sequenceName = "HR_ACTION_SEQ",
		initialValue = 1,
		allocationSize = 1
		)
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class HrAction {
	
	// 발령ID
	@Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "HR_ACTION_SEQ_GENERATOR")
	@Column(name = "ACTION_ID", nullable = false)
	private Long actionId;
	
	// 대상사원 (사원ID) 
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMP_ID", nullable = false)
    private Emp emp;
	
	// 발령유형
	@Column(name = "ACTION_TYPE", length = 20, nullable = false)
	private String actionType;
	
	// 발령 효력일자 (휴직 시작일 / 복직일 / 퇴직일 등 공통 사용)
	@Column(name = "EFFECTIVE_DATE")
	private LocalDate effectiveDate;
	
	// 휴직 종료 예정일
	@Column(name = "LEAVE_END_DATE")
	private LocalDate leaveEndDate;
	
	// 이전부서ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FROM_DEPT_ID")
    private Dept fromDept;
	
	// 이후부서ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TO_DEPT_ID")
    private Dept toDept;
	
	// 이전직급
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FROM_POS_CODE")
    private Position fromPosition;
	
	// 이후직급
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TO_POS_CODE")
    private Position toPosition;
	
	// 발령사유
	@Column(name = "REASON", length = 4000)
	private String actionReason;
	
	// 승인상태
	@Column(name = "STATUS", length = 10, nullable = false)
    private String status = "대기";   // 자바에서 기본값 세팅
	
	// 등록자 (CREATED_USER = 사원)
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CREATED_USER", nullable = false)
    private Emp createdUser;
	
	// 등록일시
	@CreatedDate
	@Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;
	
	// 결재문서ID
	@Column(name="APPROVAL_ID")
	private Long approvalId;
	 
	// 발령 적용일자 (최종 승인 후 실제 발령이 반영된 날짜)
	@Column(name = "APPLIED_DATE")
	private LocalDate appliedDate; 

	// 발령 적용 여부
	/* N : 아직 발령이 시스템이 적용되지 않음
	 *     즉 결재 중이거나, 결재는 완료되었지만 효력일이 미래인 상태
	 * Y : 발령이 실제로 EMP 테이블에 반영 완료
	 *     즉 스케줄러가 돌면서 해당 발령을 처리한 상태
	 */
	@Column(name = "APPLIED_YN", nullable = false)
	private String appliedYn = "N";
	

}
