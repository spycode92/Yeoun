package com.yeoun.inventory.dto;

import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.yeoun.inventory.entity.Inventory;
import com.yeoun.inventory.entity.WarehouseLocation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class InventoryDTO {
	private Long ivId; // 재고Id 
	
//	@NotBlank(message = "로트번호는 필수 입력값입니다.")
	private String lotNo;
	
//	@NotBlank(message = "로케이션ID는 필수 입력값입니다.")
	private String locationId;
	
//	@NotBlank(message = "상품ID는 필수 입력값입니다.")
	private String itemId; // 원자재/부자재, 완제품의 기준정보 고유값
	
//	@NotBlank(message = "재고량은 필수 입력값입니다.")
	private Long ivAmount; // 재고량
	
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime expirationDate; // 유통기한
	
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime manufactureDate; // 제조일
	
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime ibDate; // 입고일

	private Long expectObAmount = 0l; // 출고예정수량
	
//	@NotBlank(message = "재고상태는 필수 입력값입니다.")
	private String ivStatus = "NORMAL"; // 재고상태 : 정상NORMAL/임박DISPOSAL_WAIT/폐기DISPOSAL
	
	// --------------------------------------------------------------------------------
	private String prodName; // 재고 상품이름
	
	private String itemType; // 재고타입(원자재 : RAW, 부자재 : SUB, 완제품: FG)
	
	private String zone; // 존
	
	private String rack; // 랙
	
	private String rackRow;
	private String rackCol;
	
	private String status; // 상태
	
	// --------------------------------------------------------------------------------
	@Builder
	public InventoryDTO(Long ivId, String lotNo, String locationId, String itemId, Long ivAmount,
			LocalDateTime expirationDate, LocalDateTime manufactureDate, LocalDateTime ibDate, Long expectObAmount,
			String ivStatus, String itemType) {
		this.ivId = ivId;
		this.lotNo = lotNo;
		this.locationId = locationId;
		this.itemId = itemId;
		this.ivAmount = ivAmount;
		this.expirationDate = expirationDate;
		this.manufactureDate = manufactureDate;
		this.ibDate = ibDate;
		this.expectObAmount = expectObAmount;
		this.ivStatus = ivStatus;
		this.itemType = itemType;
	}
	
	// --------------------------------------------------------------------------------
	private static ModelMapper modelMapper = new ModelMapper();
	
	public Inventory toEntity() {
	    // 공통 단순 필드는 ModelMapper로 복사
	    Inventory inventory = modelMapper.map(this, Inventory.class);
	    
	    // 상품이름 설정
	    this.setProdName(inventory.getItemName());
	    
	    // 연관관계는 직접 세팅
	    WarehouseLocation location = new WarehouseLocation();
	    if(this.getLocationId() != null) {
	    	location.setLocationId(this.getLocationId());
	    	inventory.setWarehouseLocation(location);
	    }

	    // 기본값 보정
	    if (inventory.getExpectObAmount() == null) {
	        inventory.setExpectObAmount(0L);
	    }
	    if (inventory.getIvStatus() == null) {
	        inventory.setIvStatus("ok");
	    }
	    // 조회용 연관 필드(materialMst, productMst)는 굳이 여기서 세팅 안 함
	    return inventory;
	}
	
	public static InventoryDTO fromEntity(Inventory inventory) {
		InventoryDTO inventoryDTO = modelMapper.map(inventory, InventoryDTO.class);
		
		inventoryDTO.setProdName(inventory.getItemName());
		
		if(inventory.getWarehouseLocation() != null) {
			inventoryDTO.setZone(inventory.getWarehouseLocation().getZone());
			inventoryDTO.setRack(inventory.getWarehouseLocation().getRack());
			inventoryDTO.setRackRow(inventory.getWarehouseLocation().getRackRow());
			inventoryDTO.setRackCol(inventory.getWarehouseLocation().getRackCol());
		}
		
		return inventoryDTO;
	}
	
	
}
