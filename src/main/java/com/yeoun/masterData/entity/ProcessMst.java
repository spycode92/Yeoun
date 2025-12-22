package com.yeoun.masterData.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PROCESS_MST")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ProcessMst implements Serializable{
	
	// 공정ID 
	@Id
	@Column(name = "PROCESS_ID", length = 20)
	private String processId;
	
	// 공정명 
	@Column(name = "PROCESS_NAME", length = 100, nullable = false)
	private String processName;
	
	// 공정유형 
	@Column(name = "PROCESS_TYPE", length = 20, nullable = false)
	private String processType;
	
	// 설명 
	@Column(name = "DESCRIPTION", length = 400)
	private String description;

	//STEP_NO
	@Column(name = "STEP_NO", nullable = false)
	private String stepNo;
	
	// 사용여부
	@Column(name = "USE_YN", length = 1, nullable = false)
	private String useYn;
	
	// 등록자
	@Column(name = "CREATED_ID", length = 7, nullable = false)
	private String createdId;
	
	// 등록일시
	@CreatedDate
	@Column(name = "CREATED_DATE")
	private LocalDateTime createdDate;
	
	// 수정자
	@Column(name = "UPDATED_ID", length = 7)
	private String updatedId;
	
	// 수정일시
	@Column(name = "UPDATED_DATE")
	private LocalDateTime updatedDate;

}
