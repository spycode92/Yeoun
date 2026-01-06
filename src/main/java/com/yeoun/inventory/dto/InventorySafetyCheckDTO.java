package com.yeoun.inventory.dto;

import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.yeoun.inventory.entity.Inventory;
import com.yeoun.warehouse.entity.WarehouseLocation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class InventorySafetyCheckDTO {
    private String itemId;
    private String itemName;
    private String itemType;

    private Long ivQty;
    private Long planOutQty;
    private Long expectIvQty;
    private Long locationsCnt;
    private Long safetyStockQty;
    private Long safetyStockQtyDaily;
    private Long expectIbAmount;

    // ★ 네이티브 쿼리 매핑용 생성자 (순서/타입 정확히 일치)
    public InventorySafetyCheckDTO(String itemId,
                                   String itemName,
                                   String itemType,
                                   Number ivQty,
                                   Number planOutQty,
                                   Number expectIvQty,
                                   Number locationsCnt,
                                   Number safetyStockQty,
                                   Number safetyStockQtyDaily,
                                   Number expectIbAmount) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemType = itemType;
        this.ivQty = ivQty == null ? 0L : ivQty.longValue();
        this.planOutQty = planOutQty == null ? 0L : planOutQty.longValue();
        this.expectIvQty = expectIvQty == null ? 0L : expectIvQty.longValue();
        this.locationsCnt = locationsCnt == null ? 0L : locationsCnt.longValue();
        this.safetyStockQty = safetyStockQty == null ? 0L : safetyStockQty.longValue();
        this.safetyStockQtyDaily = safetyStockQtyDaily == null ? 0L : safetyStockQtyDaily.longValue();
        this.expectIbAmount = expectIbAmount == null ? 0L : expectIbAmount.longValue();
    }
	
}
