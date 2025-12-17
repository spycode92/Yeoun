package com.yeoun.inventory.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class WarehouseLocationCreateRequest {
    private String zone;
    private String rack;
    private int rowStart;
    private int rowEnd;
    private int colStart;
    private int colEnd;
}
