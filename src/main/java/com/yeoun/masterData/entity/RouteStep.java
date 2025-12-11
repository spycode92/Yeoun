package com.yeoun.masterData.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;

@Entity
@Table(name = "ROUTE_STEP")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class RouteStep implements Serializable{
	
	// 라우트 단계 ID
	@Id
    @Column(name = "ROUTE_STEP_ID", length = 20)
    private String routeStepId;
	
	// 라우트 ID
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROUTE_ID", nullable = false)
    private RouteHeader routeHeader;
	
	// 공정 순서
	@Column(name = "STEP_SEQ", nullable = false)
    private Integer stepSeq;
	
	// 공정 ID (실행할 공정)
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROCESS_ID", nullable = false)
    private ProcessMst process;
	
	// QC 포인트 여부
	@Column(name = "QC_POINT_YN", length = 1, nullable = false)
    private String qcPointYn;
	
	// 비고
	@Column(name = "REMARK", length = 200)
    private String remark;

	// 최초 등록자
	@Column(name = "CREATED_ID", length = 7, nullable = false)
	private String createdId;
	
	// 최초 등록일
	@CreatedDate
	@Column(name = "CREATED_DATE")
	private LocalDateTime createdDate;
	
	// 수정자
	@Column(name = "UPDATED_ID", length = 7)
	private String updatedId;
	
	// 수정일
	@Column(name = "UPDATED_DATE")
	private LocalDateTime updatedDate;
}
