package com.yeoun.masterData.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

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
@Table(name = "PRODUCT_MST")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class) 
public class ProductMst implements Serializable{
	
		@Id
		@Column(name="PRD_ID", length = 50)
		private String prdId; //제품id
		
		@Column(name="ITEM_NAME", length = 50)
		private String itemName;//품목명
		
		@Column(name="PRD_NAME", length = 100)
		private String prdName; //제품명	
		
		@Column(name="PRD_CAT", length = 50)
		private String prdCat; //제품유형
		
		@Column(name="MIN_QTY")
		private Long minQty; //최소수량
		
		@Column(name="PRD_UNIT", length = 20)
		private String prdUnit; //단위
		
		@Column(name="PRD_STATUS", length = 20)
		private String prdStatus; //상태

		@Column(name="EFFECTIVE_DATE")
		private Integer effectiveDate; //유효일자
		
		@Column(name = "UNIT_PRICE", precision = 18, scale = 2)
		private BigDecimal unitPrice;
		
		@Column(name="PRD_SPEC", length = 225)
		private String prdSpec; //제품상세설명
		
		@Column(name="CREATED_ID")
		private String createdId; //생성자 id
		
		@CreatedDate
		@Column(name="CREATED_DATE")
		private LocalDate createdDate; //생성일시
		
		@Column(name="UPDATED_ID")
		private String updatedId; //수정자 id
		
		@Column(name="UPDATED_DATE")
		private LocalDate updatedDate; //수정일시

		@Column(name="USE_YN", length = 1)
		private String useYn; //사용여부
		

	}