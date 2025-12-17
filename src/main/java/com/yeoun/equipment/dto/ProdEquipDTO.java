package com.yeoun.equipment.dto;

import java.time.LocalDateTime;

import com.yeoun.equipment.entity.ProdEquip;
import groovy.transform.ToString;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.modelmapper.ModelMapper;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProdEquipDTO {
	private Long equipId;
	private String equipTypeId;
	private String equipName;
	private String lineId;
	private String status;
	private LocalDateTime createdDate;
	private LocalDateTime updatedDate;
	private String remark;

	private static ModelMapper modelMapper = new ModelMapper();
	public ProdEquip toEntity(){
		return modelMapper.map(this, ProdEquip.class);
	}
	public static ProdEquipDTO fromEntity(ProdEquip prodLine){
		return modelMapper.map(prodLine, ProdEquipDTO.class);
	}

}
