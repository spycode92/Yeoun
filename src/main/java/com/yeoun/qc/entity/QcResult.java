package com.yeoun.qc.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "QC_RESULT")
@SequenceGenerator(
        name = "QC_RESULT_SEQ_GENERATOR",
        sequenceName = "QC_RESULT_SEQ",   
        initialValue = 1,
        allocationSize = 1
)
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class QcResult {
	
	// QC 결과 ID
	@Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "QC_RESULT_SEQ_GENERATOR")
	@Column(name = "QC_RESULT_ID")
	private Long qcResultId;
	
	// 작업지시번호
	@Column(name = "ORDER_ID", length = 16, nullable = false)
	private String orderId;
	
	// LOT 번호
	@Column(name = "LOT_NO", length = 50)
	private String lotNo;
	
	// 검사일자
	@Column(name = "INSPECTION_DATE")
	private LocalDate inspectionDate;
	 
	// 검사자
	@Column(name = "INSPECTOR_ID", length = 7)
	private String inspectorId;
	
	// 검사수량
	@Column(name = "INSPECTION_QTY")
	private Integer inspectionQty;
	
	// 양품수량
	@Column(name = "GOOD_QTY")
	private Integer goodQty;
	 
	// 불량수량
	@Column(name = "DEFECT_QTY")
    private Integer defectQty;
	
	// 전체결과
	@Column(name = "OVERALL_RESULT", length = 10, nullable = false)
    private String overallResult;
	
	// 불합격사유
	@Column(name = "FAIL_REASON", length = 400)
    private String failReason;
	
	// 비고
	@Column(name = "REMARK", length = 200)
    private String remark;
	
	// 등록자
	@Column(name = "CREATED_ID", length = 7, nullable = false, updatable = false)
    private String createdId;
	
	// 등록일시
	@CreatedDate
    @Column(name = "CREATED_DATE", nullable = false, updatable = false)
    private LocalDateTime createdDate;
	
	// 수정자
	@Column(name = "UPDATED_ID", length = 7)
    private String updatedId;
	
	// 수정일시
    @LastModifiedDate
    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

}
