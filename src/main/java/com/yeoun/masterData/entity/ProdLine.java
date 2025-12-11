package com.yeoun.masterData.entity;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "PROD_LINE")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class ProdLine {
	
	@Id @Column(nullable = false, name = "LINE_ID")
	private String lineId;
	
	@Column(nullable = false)
	private String lineName;
	
	@Column(nullable = false)
	private String status;

}
