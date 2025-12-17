package com.yeoun.equipment.dto;

import com.yeoun.equipment.entity.ProdLine;
import lombok.*;
import org.modelmapper.ModelMapper;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProdLineDTO {
    private String lineId;
    private String lineName;
    private String status;
    private String remark;
    private String useYn;

    private static ModelMapper modelMapper = new ModelMapper();
    public ProdLine toEntity(){
        return modelMapper.map(this, ProdLine.class);
    }
    public static ProdLineDTO fromEntity(ProdLine prodLine){
        return modelMapper.map(prodLine, ProdLineDTO.class);
    }
}
