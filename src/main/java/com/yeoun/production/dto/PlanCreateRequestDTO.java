package com.yeoun.production.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanCreateRequestDTO {
    private List<PlanCreateItemDTO> items;   
    private String memo;
}
