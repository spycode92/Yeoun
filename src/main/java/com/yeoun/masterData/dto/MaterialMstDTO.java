package com.yeoun.masterData.dto;

import java.time.LocalDate;

import org.modelmapper.ModelMapper;

import com.yeoun.masterData.entity.MaterialMst;

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
public class MaterialMstDTO {

	@NotBlank(message = "원재료 ID는 필수 입력값입니다.")
	private String matId; //원재료id
	
	@NotBlank(message = "원재료 품목명은 필수 입력값입니다.")
	private String matName; //원재료품목명
	
	@NotBlank(message = "원재료 유형은 필수 입력값입니다.")
	private String matType; //원재료 유형
	
	@NotBlank(message = "단위는 필수 입력값입니다.")
	private String matUnit; //단위(용량)
	
	@NotBlank(message = "유효일자는 필수 입력값입니다.")
	private String effectiveDate; //유효일자
	
	@NotBlank(message = "상세설명은 필수 입력값입니다.")
	private String matDesc; //상세설명
	
	private String createId; //생성자 id
	
	private LocalDate createDate; //생성일시
	
	private String updateId; //수정자 id
	
	private LocalDate updateDate; //수정일시
	
	private static final ModelMapper modelMapper = new ModelMapper();
	
	public static MaterialMstDTO fromEntity(MaterialMst entity) {
		if(entity == null) return null;
		return modelMapper.map(entity, MaterialMstDTO.class);
	}
	
	public MaterialMst toEntity() {
		return modelMapper.map(this, MaterialMst.class);
	}

}
