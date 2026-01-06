package com.yeoun.inventory.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeoun.warehouse.entity.WarehouseLocation;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "INVENTORY_HISTORY")
@SequenceGenerator(
		name = "INVENTORY_HISTORY_SEQ_GENERATOR", // JPA 에서 사용하는 시퀀스 이름(DB의 시퀀스 이름이 아님!)
		sequenceName = "INVENTORY_HISTORY_SEQ", // 오라클에서 사용하는 시퀀스 이름
		initialValue = 1, 			// 초기값(오라클 시퀀스의 start with 1과 동일)
		allocationSize = 1          // 증가값(오라클 시퀀스의 increment by 값과 동일)
		)
@Getter
@Setter
@ToString
@EntityListeners(AuditingEntityListener.class)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryHistory {
	@Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "INVENTORY_HISTORY_SEQ_GENERATOR")
	@Column(name = "IV_HISTORY_ID", updatable = false)
	private Long ivHistoryId; 
	
	@Column(nullable = false)
	private String lotNo;
	
	@Column(nullable = false)
	private String itemName; // 재고상품이름
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PREV_LOCATION_ID") 
	private WarehouseLocation prevWarehouseLocation;
	
//	@Column(nullable = true)
//	private String prevLocationId; //이전위치
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CURRENT_LOCATION_ID") 
	private WarehouseLocation currentWarehouseLocation;
	
//	@Column(nullable = true)
//	private String currentLocationId; // 현재위치
	
	
	@Column(nullable = true) // 이동수량
	private Long moveAmount = 0l;
	
	@Column(nullable = false)
	private String empId; // 작업자
	
	@Column(nullable = false)
	private String workType; // 작업종류 ( 입고, 이동, 출고, 폐기, 증가, 감소 )
	
	@Column(nullable = true)
	private Long prevAmount = 0l; // 이전수량
	
	@Column(nullable = true)
	private Long currentAmount = 0l; // 현재수량
	
	@Column(nullable = true)
	private String reason; // 이유
	
	@CreatedDate
	private LocalDateTime createdDate; // 등록 일시
	
	public static InventoryHistory createFromMove(Inventory beforeInventory, 
	            Inventory afterInventory, 
	            Long moveQty, 
	            String empId) {
	return InventoryHistory.builder()
		.lotNo(beforeInventory.getLotNo())
		.itemName(beforeInventory.getItemName())
		.prevWarehouseLocation(beforeInventory.getWarehouseLocation())
		.currentWarehouseLocation(afterInventory.getWarehouseLocation())
		.moveAmount(moveQty)
		.empId(empId)
		.workType("MOVE")
		.prevAmount(0l)
		.currentAmount(0l)
		.reason("")
		.build();
	}
	
}
