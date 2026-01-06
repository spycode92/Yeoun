package com.yeoun.equipment.dto;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

@Getter
@Setter
public class EquipmentCreateRequest {

    private String equipId;

    @NotBlank(message = "설비 종류를 선택해주세요.")
    private String equipType;

    @NotBlank(message = "소속 라인을 선택해주세요.")
    private String lineId;

    @NotBlank(message = "설비 이름을 입력해주세요.")
    private String equipName;

    private String status;

}
