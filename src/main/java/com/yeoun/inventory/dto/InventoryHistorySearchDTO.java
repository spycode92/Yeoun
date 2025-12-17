package com.yeoun.inventory.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;

import com.yeoun.inventory.entity.InventoryHistory;
import com.yeoun.inventory.entity.WarehouseLocation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class InventoryHistorySearchDTO {
    // 검색조건
    private LocalDate startDate;
    private LocalDate endDate;
    private String workType;
    private String searchKeyword;
}
