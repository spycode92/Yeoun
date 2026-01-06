package com.yeoun.masterData.dto;

import java.time.LocalDate;



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
public class QcItemDTO {

	@NotBlank(message = "QC 항목 ID는 필수 입력값입니다.")
	private String qcItemId; // QC 항목 id
	
	@NotBlank(message = "항목명은 필수 입력값입니다.")
	private String itemName; //항목명

	@NotBlank(message = "대상구분은 필수 입력값입니다.")
	private String targetType; //대상구분
	
	@NotBlank(message = "단위는 필수 입력값입니다.")
	private String unit; //단위
	
	private String stdText; //기준값텍스트
	
	@NotBlank(message = "허용하한값은 필수 입력값입니다.")
	private String minValue; //허용하한값
	
	@NotBlank(message = "허용상한값은 필수 입력값입니다.")
	private String maxValue; //허용상한값
	
	private String useYn; //사용여부
	
	@NotBlank(message = "정렬순서는 필수 입력값입니다.")
	private String sortOrder; //정렬순서
	
	private String createId; //생성자 id
	
	private LocalDate createDate; //생성일시
	
	private String updateId; //수정자 id
	
	private LocalDate updateDate; //수정일시
	
}
