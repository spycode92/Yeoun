package com.yeoun.masterData.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "BOM_MST")
@Getter
@Setter
@IdClass(BomMstId.class)
public class BomMst {
	
	@Column(name="BOM_ID", length = 20, nullable = false)
	private String bomId; //BOMid
	
	@Id
	@Column(name="PRD_ID", length = 50, nullable = false)
	private String prdId; //제품id
	
	@Id
	@Column(name="MAT_ID", length = 50, nullable = false)
	private String matId; //원재료id
	
	@Column(name="MAT_QTY", nullable = false, precision = 10, scale = 3)
	private BigDecimal matQty; //원재료사용량

	@Column(name="MAT_UNIT", length = 20)
	private String matUnit; //사용단위
	
	@Column(name="BOM_SEQ_NO")
	private Long bomSeqNo; //순서
	
	@Column(name="CREATED_ID", length = 7)
	private String createdId; //생성자 id
	
	@CreatedDate
	@Column(name="CREATED_DATE")
	private LocalDate createdDate; //생성일시
	
	@Column(name="UPDATED_ID", length = 7)
	private String updatedId; //수정자 id
	
	@Column(name="UPDATED_DATE")
	private LocalDate updatedDate; //수정일시


}
