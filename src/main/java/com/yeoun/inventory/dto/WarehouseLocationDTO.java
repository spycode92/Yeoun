package com.yeoun.inventory.dto;

import org.modelmapper.ModelMapper;

import com.yeoun.warehouse.entity.WarehouseLocation;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class WarehouseLocationDTO {
    private String locationId;  // 로케이션 고유ID
    
    private String zone;        // 존
    
    private String rack;        // 랙
    
    private String rackRow;     // 로우
    
    private String rackCol;     // 컬럼
    
    private String useYn;      // 사용 여부
    
    private Integer stockCount;  // 창고 사용하는 재고 카운트
    
	// ----------------------------------------------------------
	private static ModelMapper modelMapper = new ModelMapper();
	
	public WarehouseLocation toEntity() {
		WarehouseLocation warehouseLocation = modelMapper.map(this, WarehouseLocation.class);
		return warehouseLocation;
	}
	
	public static WarehouseLocationDTO fromEntity(WarehouseLocation warehouseLocation) {
		WarehouseLocationDTO locationDTO = modelMapper.map(warehouseLocation, WarehouseLocationDTO.class);
		return locationDTO;
	}
}
