package com.yeoun.equipment.dto;

import com.yeoun.equipment.entity.Equipment;
import lombok.*;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class EquipmentDTO {
    private String equipId;
    private String koName;
    private String equipName;
    private String remark;
    private String useYn;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    private static ModelMapper modelMapper = new ModelMapper();
    public Equipment toEntity(){
        return modelMapper.map(this, Equipment.class);
    }

    public static EquipmentDTO fromEntity(Equipment entity){
        return modelMapper.map(entity, EquipmentDTO.class);
    }

}

