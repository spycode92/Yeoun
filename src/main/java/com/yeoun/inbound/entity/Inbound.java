package com.yeoun.inbound.entity;

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
import lombok.ToString;

@Entity
@Table(name = "INBOUND")
@Getter
@Setter
@ToString
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
public class Inbound {
	@Id @Column(name = "INBOUND_ID", updatable = false)
	private String inboundId; 
	
	@Column(nullable = false)
	private LocalDateTime expectArrivalDate; // 예상도착일
	
	@Column(nullable = false)
	private String inboundStatus; // 입고상태
	
	@Column(nullable = true)
	private String materialId; // 발주 고유번호
	
	@Column(nullable = true)
	private String prodId; // 작업지시서 고유번호
	
	@Column(nullable = true)
	private String empId; // 담당자
	
	@CreatedDate
	private LocalDateTime createdDate; // 등록 일시
	
	// InboundItem 연관관계 매핑
	@OneToMany(mappedBy = "inbound", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<InboundItem> items = new ArrayList<>();
	
	// 품목 저장
	public void addItem(InboundItem item) {
		this.items.add(item);
		item.setInbound(this);
	}

	@Builder
	public Inbound(String inboundId, LocalDateTime expectArrivalDate, String inboundStatus, String materialId,
			String prodId, LocalDateTime createdDate) {
		this.inboundId = inboundId;
		this.expectArrivalDate = expectArrivalDate;
		this.inboundStatus = inboundStatus;
		this.materialId = materialId;
		this.prodId = prodId;
		this.createdDate = createdDate;
	}
	
	// 상태 변경
	public void changeStatus(String status) {
		this.inboundStatus = status;
	}
	
	public void registEmpId(String empId) {
		this.empId = empId;
	}
}
