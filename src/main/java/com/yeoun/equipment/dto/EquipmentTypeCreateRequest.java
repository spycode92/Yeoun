package com.yeoun.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentTypeCreateRequest {
	
	@NotBlank(message = "설비 ID는 필수입니다.")
	private String equipId;
	
	@NotBlank(message = "설비 한글명은 필수입니다.")
	private String koName;
	
	@NotBlank(message = "설비 풀네임은 필수입니다.")
	private String equipName;
	
	@Size(max = 500, message = "설명은 500자 이내입니다.")
	private String remark;

	private String useYn;
}
