package com.yeoun.inbound.entity;

import java.time.LocalDateTime;

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
@Table(name = "INBOUND_ITEM")
@SequenceGenerator(
		name = "INBOUND_ITEM_GENERATOR", // JPA 에서 사용하는 시퀀스 이름(DB의 시퀀스 이름이 아님!)
		sequenceName = "INBOUND_ITEM_SEQ", // 오라클에서 사용하는 시퀀스 이름
		initialValue = 1, 			// 초기값(오라클 시퀀스의 start with 1과 동일)
		allocationSize = 1          // 증가값(오라클 시퀀스의 increment by 값과 동일)
		)
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
public class InboundItem {
	@Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "INBOUND_ITEM_GENERATOR")
	@Column(name = "INBOUND_ITEM_ID", updatable = false)
	private Long InboundItemId; // 입고대기품목 id
	
	@Column(nullable = true)
	private String lotNo; // 로트넘버
	
	@ManyToOne
	@JoinColumn(name = "INBOUND_ID")
	private Inbound inbound;
	
	@Column(nullable = false)
	private String itemId; // 원자재/부자재, 완제품의 기준정보 고유값
	
	@Column(nullable = false)
	private Long requestAmount; // 발주수량
	
	@Column(nullable = true)
	private Long inboundAmount; // 입고수량
	
	@Column(nullable = true)
	private Long disposeAmount; // 폐기수량
	
	@Column(nullable = false)
	private LocalDateTime manufactureDate; // 제조일
	
	@Column(nullable = true)
	private LocalDateTime expirationDate; // 유통기한
	
	@Column(nullable = false)
	private String itemType; // 아이템타입

	@Column(nullable = true)
	private String locationId; //창고위치

	@Builder
	public InboundItem(Long inboundItemId, String lotNo, Inbound inbound, String itemId, Long requestAmount,
			Long inboundAmount, Long disposeAmount, LocalDateTime manufactureDate, LocalDateTime expirationDate,
			String itemType, String locationId) {
		this.lotNo = lotNo;
		this.inbound = inbound;
		this.itemId = itemId;
		this.requestAmount = requestAmount;
		this.inboundAmount = inboundAmount;
		this.disposeAmount = disposeAmount;
		this.manufactureDate = manufactureDate;
		this.expirationDate = expirationDate;
		this.itemType = itemType;
		this.locationId = locationId;
	}
	
	// 입고대기에서 완료로 변경시 변경될 내용들
	public void updateInfo(String lotNo, Long inboundAmount, Long disposeAmount, String locationId) {
		this.lotNo = lotNo;
		this.inboundAmount = inboundAmount;
		this.disposeAmount = disposeAmount;
		this.locationId = locationId;
	}
}
