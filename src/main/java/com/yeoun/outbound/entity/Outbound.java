package com.yeoun.outbound.entity;

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
@Table(name = "OUTBOUND")
@Getter
@Setter
@ToString
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Outbound {
	@Id @Column(name = "OUTBOUND_ID", updatable = false)
	private String outboundId; 
	
	@Column(nullable = false)
	private String requestBy; // 요청자 empId
	
	@Column(nullable = true)
	private String processBy; // 출고처리자 empId
	
	@Column(nullable = true)
	private String workOrderId; // 작업지시서
	
	@Column(nullable = true)
	private String shipmentId; // 출하지시서
	
	@Column(nullable = false)
	private String status; // 상태
	
	@Column(nullable = false)
	private LocalDateTime expectOutboundDate; // 출고 예정일 
	
	@Column(nullable = true)
	private LocalDateTime outboundDate; // 출고일
	
	@CreatedDate
	private LocalDateTime createdDate; // 등록 일시
	
	@OneToMany(mappedBy = "outbound", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<OutboundItem> items = new ArrayList<>();

	@Builder
	public Outbound(String outboundId, String requestBy, String processBy, String workOrderId, String shipmentId,
			String status, LocalDateTime expectOutboundDate, LocalDateTime outboundDate, LocalDateTime createdDate,
			List<OutboundItem> items) {
		this.outboundId = outboundId;
		this.requestBy = requestBy;
		this.processBy = processBy;
		this.workOrderId = workOrderId;
		this.shipmentId = shipmentId;
		this.status = status;
		this.expectOutboundDate = expectOutboundDate;
		this.outboundDate = outboundDate;
		this.createdDate = createdDate;
	}
	
	// 품목 저장
	public void addItem(OutboundItem item) {
		this.items.add(item);
		item.setOutbound(this);
	}
	
	// 출고 담당자 등록
	public void registProcessBy(String empId) {
		this.processBy = empId;
	}
	
	// 출고일 등록
	public void registOutboundDate(LocalDateTime outboundDate) {
		this.outboundDate = outboundDate;
	}
	
	// 출고 상태 변경
	public void updateStatus(String status) {
		this.status = status;
	}
}
