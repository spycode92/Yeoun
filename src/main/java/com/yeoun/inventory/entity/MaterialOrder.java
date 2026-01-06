package com.yeoun.inventory.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "MATERIAL_ORDER")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
public class MaterialOrder {
	@Id @Column(name = "ORDER_ID", updatable = false)
	private String orderId; // 로케이션 고유ID
	
	@Column(nullable = false)
	private String clientId; // 거래처ID
	@Column(nullable = false)
	private String status; // 상태
	@Column(nullable = false)
	private String empId; // 담당자
	@Column(nullable = false)
	private String dueDate; // 납기일
	@Column(nullable = false)
	private String totalAmount; // 발주한 총금액
	@Column(nullable = true)
	private String planId; // 생산계획 ID
	@CreatedDate
	private LocalDateTime createdDate; // 등록 일시
	
	// MaterialOrderItem와 연관관계 매핑
	@OneToMany(mappedBy = "materialOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<MaterialOrderItem> items = new ArrayList<>();
	
	// 품목 저장
	public void addItem(MaterialOrderItem item) {
		this.items.add(item);
		item.setMaterialOrder(this);
	}
	
	@Builder
	public MaterialOrder(String orderId, String clientId, String status,
						 String empId, String dueDate, String totalAmount) {
		this.orderId = orderId;
		this.clientId = clientId;
		this.status = status;
		this.empId = empId;
		this.dueDate = dueDate;
		this.totalAmount = totalAmount;
	}
	
	public void changeStatus(String status) {
		this.status = status;
	}
}
