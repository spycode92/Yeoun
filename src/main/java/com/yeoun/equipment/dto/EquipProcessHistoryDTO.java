package com.yeoun.equipment.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EquipProcessHistoryDTO {

    private String lineId;
    private String equipId;
    private String step;

    private String orderId;
    private String wopId;
    private String productName;
    private String status;
    private String startTime;
    private String endTime;
}
