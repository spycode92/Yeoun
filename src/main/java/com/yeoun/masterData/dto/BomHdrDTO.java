package com.yeoun.masterData.dto;

import java.time.LocalDateTime;

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
public class BomHdrDTO {

	@NotBlank(message = "BOM HDRID는 필수 입력값입니다.")
    private String bomHdrId; //BOM Header ID

	@NotBlank(message = "BOM ID는 필수 입력값입니다.")
	private String bomId; //BOMid

	@NotBlank(message = "BOM 명은 필수 입력값입니다.")
	private String bomHdrName; //BOM Header Name
	
    @NotBlank(message = "BOM 타입은 필수 입력값입니다.")
    private String bomHdrType; //BOM Header Type

    @NotBlank(message = "BOM 사용여부는 필수 입력값입니다.")
    private String useYn; //사용여부

    private LocalDateTime bomHdrDate; 
    
}
