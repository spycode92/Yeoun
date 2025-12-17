package com.yeoun.inventory.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeoun.masterData.entity.MaterialMst;
import com.yeoun.masterData.entity.ProductMst;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "INVENTORY")
@SequenceGenerator(
		name = "INVENTORY_SEQ_GENERATOR", // JPA 에서 사용하는 시퀀스 이름(DB의 시퀀스 이름이 아님!)
		sequenceName = "INVENTORY_SEQ", // 오라클에서 사용하는 시퀀스 이름
		initialValue = 1, 			// 초기값(오라클 시퀀스의 start with 1과 동일)
		allocationSize = 1          // 증가값(오라클 시퀀스의 increment by 값과 동일)
		)
@Getter
@Setter
@ToString
@EntityListeners(AuditingEntityListener.class)
public class Inventory {
	@Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "INVENTORY_SEQ_GENERATOR")
	@Column(name = "IV_ID", updatable = false)
	private Long ivId; 
	
	@Column(nullable = false)
	private String lotNo;
	
//	@Column(nullable = false)
//	private String locationId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "LOCATION_ID") 
	private WarehouseLocation warehouseLocation;
	
	@Column(nullable = false)
	private String itemId; // 원자재/부자재, 완제품의 기준정보 고유값
	
	@Column(nullable = false)
	private String itemType; // 상품의타입 (RAW, SUB, FG)
	
	// -------------------------------------------------------------
	// 조회용 객체 ItemId가 materialMst, productMst에 있는지 검색
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itemId", insertable = false, updatable = false)
    @NotFound(action = NotFoundAction.IGNORE) 
    @ToString.Exclude
    private MaterialMst materialMst;
	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itemId", insertable = false, updatable = false)
    @NotFound(action = NotFoundAction.IGNORE) 
    @ToString.Exclude
    private ProductMst productMst;
    
    // getItemName베서드를 통해 해당 재고의 이름 얻기
    public String getItemName() {
        if ("FG".equals(this.itemType)) {
            return (productMst != null) ? productMst.getPrdName() : "Unknown Product";
        } else {
            // RAW, SUB 인 경우
            return (materialMst != null) ? materialMst.getMatName() : "Unknown Material";
        }
    }
    // -------------------------------------------------------------
	
	@Column(nullable = false)
	private Long ivAmount; // 재고량
	
	@Column(nullable = true)
	private LocalDateTime expirationDate; // 유통기한
	
	@Column(nullable = false)
	private LocalDateTime manufactureDate; // 제조일
	
	@Column(nullable = false)
	private LocalDateTime ibDate; // 입고일

	@Column(nullable = false)
	private Long expectObAmount = 0l; // 출고예정수량
	
	@Column(nullable = false)
	private String ivStatus; // 재고상태 : 정상NORMAL/임박DISPOSAL_WAIT/유통기한지남EXPIRED
	
	public Inventory createMovedInventory(WarehouseLocation newLocation, Long moveQty) {
	    Inventory movedInventory = new Inventory();
	    movedInventory.setLotNo(this.lotNo);
	    movedInventory.setWarehouseLocation(newLocation);
	    movedInventory.setItemId(this.itemId);
	    movedInventory.setItemType(this.itemType);
	    movedInventory.setIvAmount(moveQty);
	    movedInventory.setExpirationDate(this.expirationDate);
	    movedInventory.setManufactureDate(this.manufactureDate);
	    movedInventory.setIbDate(this.ibDate);
	    movedInventory.setExpectObAmount(0L); // 이동 시 출고예정수량 0으로 초기화할 수도 있음
	    movedInventory.setIvStatus(this.ivStatus);
	    
	    return movedInventory;
	}
	
	// 재고 수량 증가
	public void increaseAmount(long qty) {
		this.ivAmount += qty;
	}
	
	
	
	
}
