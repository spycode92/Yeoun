package com.yeoun.masterData.entity;

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
@Table(name = "MATERIAL_MST")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class) 
public class MaterialMst {

	@Id
	@Column(name="MAT_ID", length = 50)
	private String matId; //원재료id
	
	@Column(name="MAT_NAME", length = 100)
	private String matName; //원재료품목명
			
	@Column(name="MAT_TYPE", length = 50)
	private String matType; //원재료 유형
	
	@Column(name="MAT_UNIT", length = 20)
	private String matUnit; //단위
	
	@Column(name="EFFECTIVE_DATE")
	private Integer effectiveDate; //유효일자
	
	@Column(name="MAT_DESC", length = 255)
	private String matDesc; //상세설명
	
	@Column(name="CREATED_ID", length = 7)
	private String createdId; //생성자 id
	
	@CreatedDate
	@Column(name="CREATED_DATE")
	private LocalDate createdDate; //생성일시
	
	@Column(name="UPDATED_ID", length = 7)
	private String updatedId; //수정자 id
	
	@Column(name="UPDATED_DATE")
	private LocalDate updatedDate; //수정일시

	@Column(name="USE_YN", length = 1)
	private String useYn; //사용여부

}
