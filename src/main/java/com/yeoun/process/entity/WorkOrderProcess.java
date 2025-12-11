package com.yeoun.process.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeoun.masterData.entity.ProcessMst;
import com.yeoun.masterData.entity.RouteStep;
import com.yeoun.order.entity.WorkOrder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "WORK_ORDER_PROCESS")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class WorkOrderProcess {
	
	// 작업공정ID
	@Id
	@Column(name = "WOP_ID")
	private String wopId;
	
	// 작업지시번호
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ORDER_ID")
	private WorkOrder workOrder;
	
	// 라우트단계ID
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ROUTE_STEP_ID")
    private RouteStep routeStep;
	
	// 공정ID
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PROCESS_ID")
    private ProcessMst process;
	
	// 공정순번
	@Column(name = "STEP_SEQ", nullable = false)
    private Integer stepSeq;
	
	// LOT 번호
	@Column(name = "LOT_NO", length = 50)
	private String lotNo;
	
	// 상태
	@Column(name = "STATUS", length = 20, nullable = false)
    private String status;
	
	// 시작일시
	@Column(name = "START_TIME")
    private LocalDateTime startTime;

	// 종료일시
	@Column(name = "END_TIME")
    private LocalDateTime endTime;
	
	// 양품수량
	@Column(name = "GOOD_QTY")
    private Integer goodQty;
	
	// 불량수량
	@Column(name = "DEFECT_QTY")
    private Integer defectQty;
	
	// 메모
	@Column(name = "MEMO", length = 400)
    private String memo;
	
	// 등록자
	@Column(name = "CREATED_ID", length = 7, nullable = false, updatable = false)
	private String createdId;
	 
	// 등록일시
	@CreatedDate
    @Column(name = "CREATED_DATE", updatable = false)
    private LocalDateTime createdDate;
	
	// 수정자
	@Column(name = "UPDATED_ID", length = 7)
    private String updatedId;
	
	// 수정일시
	@LastModifiedDate
    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;
	
}
