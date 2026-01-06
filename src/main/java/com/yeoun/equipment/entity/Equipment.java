package com.yeoun.equipment.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeoun.masterData.entity.ProcessMst;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "EQUIPMENT")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Equipment {
	
	@Id @Column(nullable = false)
	private String equipId;
	
	@Column(nullable = false)
	private String koName;
	
	@Column(nullable = false)
	private String equipName;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PROCESS_ID")
	private ProcessMst process;

	@Column(nullable = false,  columnDefinition = "CHAR(1) DEFAULT 'Y'")
	private String useYn = "Y";
	
	@Column(length = 500)
	private String remark;
	
	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdDate;
	
	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime updatedDate;

}
