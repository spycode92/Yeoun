package com.yeoun.outbound.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
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
@Table(name = "OUTBOUND_ITEM")
@SequenceGenerator(
		name = "OUTBOUND_ITEM_GENERATOR", // JPA 에서 사용하는 시퀀스 이름(DB의 시퀀스 이름이 아님!)
		sequenceName = "OUTBOUND_ITEM_SEQ", // 오라클에서 사용하는 시퀀스 이름
		initialValue = 1, 			// 초기값(오라클 시퀀스의 start with 1과 동일)
		allocationSize = 1          // 증가값(오라클 시퀀스의 increment by 값과 동일)
		)
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
public class OutboundItem {
	@Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "OUTBOUND_ITEM_GENERATOR")
	@Column(name = "OUTBOUND_ITEM_ID", updatable = false)
	private Long outboundItemId; // 입고대기품목 id
	
	@ManyToOne
	@JoinColumn(name = "OUTBOUND_ID")
	private Outbound outbound; // 출고ID 
	
	@Column(nullable = false)
	private String itemId; // 원자재/부자재, 완제품의 기준정보 고유값

	@Column(nullable = false)
	private String lotNo; // 로트넘버
	
	@Column(nullable = false)
	private Long outboundAmount; // 출고수량

	@Column(nullable = false)
	private String itemType; // 아이템타입

	@Column(nullable = true)
	private Long ivId; // 재고Id
	
	@Column(nullable = true)
	private String locationId;

	@Builder
	public OutboundItem(Long outboundItemId, Outbound outbound, String outboundId, String itemId, String lotNo, Long outboundAmount,
			String itemType, Long ivId, String locationId) {
		this.outboundItemId = outboundItemId;
		this.itemId = itemId;
		this.lotNo = lotNo;
		this.outboundAmount = outboundAmount;
		this.itemType = itemType;
		this.ivId = ivId;
		this.outbound = outbound;
		this.locationId = locationId;
	}
	
	
	
}
