package com.yeoun.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LineCreateRequest {

    public String lineId;

    @NotBlank(message = "라인 이름은 필수입니다.")
    public String lineName;

    @Size(max = 500, message = "설명은 500자 이내입니다.")
    public String remark;

    public String useYn;

}
