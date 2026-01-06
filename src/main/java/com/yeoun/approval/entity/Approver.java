package com.yeoun.approval.entity;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="APPROVER")
@Setter
@Getter
@EntityListeners(AuditingEntityListener.class)
@IdClass(ApproverId.class)
public class Approver {

	@Id
	@Column(name="EMP_ID")
	private String empId; //결재자 사원 id
	
	@Column(name="DELEGATE_STATUS")
	private String delegateStatus; //전결자 상태- 전결,대결,선결
	
	@Column(name="APPROVAL_STATUS")
	private boolean approvalStatus; //결재 상태 y/n
	
	@Id
	@Column(name="APPROVAL_ID")
	private Long approvalId; //결재서류 id
	
	@Column(name="ORDER_APPROVERS")
	private String orderApprovers; //결재 순서
	
	@Column(name="VIEWING")
	private String viewing;
	
	
}
