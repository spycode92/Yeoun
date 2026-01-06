package com.yeoun.inventory.entity;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "MATERIAL_ORDER_ITEM")
@SequenceGenerator(
		name = "MATERIAL_ORDER_ITEM_SEQ_GENERATOR", // JPA 에서 사용하는 시퀀스 이름(DB의 시퀀스 이름이 아님!)
		sequenceName = "MATERIAL_ORDER_ITEM_SEQ", // 오라클에서 사용하는 시퀀스 이름
		initialValue = 1, 			// 초기값(오라클 시퀀스의 start with 1과 동일)
		allocationSize = 1          // 증가값(오라클 시퀀스의 increment by 값과 동일)
		)
@Getter
@Setter
@ToString
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
public class MaterialOrderItem {
	@Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "MATERIAL_ORDER_ITEM_SEQ_GENERATOR") 
	@Column(name = "ORDER_ITEM_ID", updatable = false)
	private Long orderItemId; // 발주품목
	
	@ManyToOne
	@JoinColumn(name = "ORDER_ID")
	private MaterialOrder materialOrder; // 발주 ID
	
	@Column(nullable = false)
	private Long itemId; // 발주상품ID
	
	@Column(nullable = false)
	private Long orderAmount; // 발주량
	
	@Column(nullable = false)
	private Long unitPrice; // 단가
	
	@Column(nullable = false)
	private Long supplyAmount; // 공급가액
	
	@Column(nullable = false)
	private Long VAT; // 부가세
	
	@Column(nullable = false)
	private Long totalPrice; // 총금액 
	
	@Builder
	public MaterialOrderItem(Long itemId, Long orderAmount, Long unitPrice,
							Long supplyAmount, Long vat, Long totalPrice) {
		this.itemId = itemId;
		this.orderAmount = orderAmount;
		this.unitPrice = unitPrice;
		this.supplyAmount = supplyAmount;
		this.VAT = vat;
		this.totalPrice = totalPrice;
	}
	
}
