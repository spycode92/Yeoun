package com.yeoun.order.entity;

import java.time.LocalDateTime;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeoun.emp.entity.Emp;
import com.yeoun.equipment.entity.ProdLine;
import com.yeoun.masterData.entity.ProductMst;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "WORK_ORDER")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrder {

	// 작업 지시번호 ID
	// WO-YYYYMMDD-0000 (시퀀스)
	@Id @Column(nullable = false, length = 16)
	private String orderId;
	
	// 생산계획 ID
	@Column			// ================================> 생산계획 엔티티랑 연결
	private String planId;
	
	// 제품 ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PRD_ID", nullable = false)
	private ProductMst product;
    
    // 계획수량
    @Column(nullable = false)
    private Integer planQty;
    
    // 예정시작일시
    @Column(nullable = false)
    private LocalDateTime planStartDate;
    
    // 실제시작일시
    @Column
    private LocalDateTime actStartDate;
    
    // 예정완료일시
    @Column(nullable = false)
    private LocalDateTime planEndDate;
    
    // 실제완료일시
    @Column
    private LocalDateTime actEndDate;
    
	// 공정 ID
	@Column				// ================================> 라우트 엔티티랑 연결
	private String routeId;
	
	//private String processId;
    
    // 수행 라인
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LINE_ID", nullable = false)
    private ProdLine line;
    
    // 수행 상태
    @Column(nullable = false)
    private String status;
    
    // 작성자 ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CREATED_ID", nullable = false)
    private Emp createdEmp;
    
    // 작성일자
    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdDate;
    
    // 수정일자
    @Column
    private LocalDateTime updatedDate;
    
    // 출고여부
    @Column(nullable = false, columnDefinition = "CHAR(1) DEFAULT 'N'")
    @Builder.Default
    private String outboundYn = "N";
    
    // 비고(특이사항 및 메모)
    @Column
    private String remark;
	
    // ======================================
    // 출고 상태 업데이트
    public void updateOutboundYn(String outboundYn) {
    	this.outboundYn = outboundYn;
    }
	
}
