package com.yeoun.masterData.dto;

import java.time.LocalDate;


import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BomMstDTO {
	
	@NotBlank(message = "BOM ID는 필수 입력값입니다.")
	private String bomId; //BOMid
	
	@NotBlank(message = "제품 ID는 필수 입력값입니다.")
	private String prdId; //제품id
	
	@NotBlank(message = "원재료 ID는 필수 입력값입니다.")
	private String matId; //원재료id
	
	@NotBlank(message = "원재료사용량 필수 입력값입니다.")
	private String matQty; //원재료사용량

	@NotBlank(message = "사용단위는 필수 입력값입니다.")
	private String matUnit; //사용단위
	
	@NotBlank(message = "순서는 필수 입력값입니다.")
	private String bomSeqNo; //순서
	
	private String createId; //생성자 id
	
	private LocalDate createDate; //생성일시
	
	private String updateId; //수정자 id
	
	@Column(name="UPDATE_DATE")
	private LocalDate updateDate; //수정일시

}
