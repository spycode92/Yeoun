package com.yeoun.masterData.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
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
	
	@CreatedDate
	@Column(nullable = false)
	private LocalDateTime createdDate;
	
	@Column
	private LocalDateTime updatedDate;

}
