package com.yeoun.inventory.dto;

import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;

import com.yeoun.inventory.entity.InventoryHistory;
import com.yeoun.warehouse.entity.WarehouseLocation;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class InventoryHistoryDTO {
    private Long ivHistoryId;        // 이력 ID
    private String lotNo;            // LOT 번호
    private String itemName;           // 재고이름
    

    private String prevLocationId;   // 이전 위치
    private String prevLocationName;
    private String currentLocationId;// 현재 위치
    private String currentLocationName;

    private String empId;            // 작업자
    private String empName;          // 작업자 이름
    private String workType;         // 작업 종류 (입고, 이동, 출고, 폐기, 증가, 감소)
    
    private Long moveAmount;         // 이동 수량
    private Long prevAmount;         // 이전 수량
    private Long currentAmount;      // 현재 수량

    private String reason;           // 사유

    private LocalDateTime createdDate; // 등록 일시
    
    // ---------------------------------------------------------------------------
    @Builder
	public InventoryHistoryDTO(Long ivHistoryId, String lotNo, String prevLocationId, String itemName,
			String prevLocationName, String currentLocationId, String currentLocationName, String empId,
			String workType, Long moveAmount, Long prevAmount, Long currentAmount, String reason,
			LocalDateTime createdDate) {
		this.ivHistoryId = ivHistoryId;
		this.lotNo = lotNo;
		this.itemName = itemName;
		this.prevLocationId = prevLocationId;
		this.prevLocationName = prevLocationName;
		this.currentLocationId = currentLocationId;
		this.currentLocationName = currentLocationName;
		this.empId = empId;
		this.workType = workType;
		this.moveAmount = moveAmount;
		this.prevAmount = prevAmount;
		this.currentAmount = currentAmount;
		this.reason = reason;
		this.createdDate = createdDate;
	}
    
    
    // ---------------------------------------------------------------------------
    private static ModelMapper modelMapper = new ModelMapper();
    // Entity -> DTO 변환
    public static InventoryHistoryDTO fromEntity(InventoryHistory history) {
        InventoryHistoryDTO dto = modelMapper.map(history, InventoryHistoryDTO.class);
        if(history.getPrevWarehouseLocation() != null) {
        	dto.setPrevLocationId(history.getPrevWarehouseLocation().getLocationId());
        	dto.setPrevLocationName(history.getPrevWarehouseLocation().getLocationName());
        }
        if(history.getCurrentWarehouseLocation() != null) {
        	dto.setCurrentLocationId(history.getCurrentWarehouseLocation().getLocationId());
        	dto.setCurrentLocationName(history.getCurrentWarehouseLocation().getLocationName());
        }
        
        return dto;
    }
    
    // DTO -> Entity 변환
    public InventoryHistory toEntity() {
    	InventoryHistory inventoryHistory = modelMapper.map(this, InventoryHistory.class);
    	
    	if (this.getPrevLocationId() != null) {
    		WarehouseLocation warehouseLocation= new WarehouseLocation();
    		warehouseLocation.setLocationId(this.getPrevLocationId());
    		inventoryHistory.setPrevWarehouseLocation(warehouseLocation);
    	}
    	
    	if (this.getCurrentLocationId() != null) {
    		WarehouseLocation warehouseLocation= new WarehouseLocation();
    		warehouseLocation.setLocationId(this.getCurrentLocationId());
    		inventoryHistory.setCurrentWarehouseLocation(warehouseLocation);
    	}
    	
    	return inventoryHistory;
    }    
    
}
