package com.yeoun.qc.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeoun.common.util.FileUtil.FileUploadHelpper;
import com.yeoun.qc.service.QcResultService.QcFileKeyUtil;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "QC_RESULT_DETAIL")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class QcResultDetail implements FileUploadHelpper {
	
	// QC 결과 상세ID
	@Id
    @Column(name = "QC_RESULT_DTL_ID", length = 20)
    private String qcResultDtlId;
	
	// QC 결과ID
	@Column(name = "QC_RESULT_ID", length = 20, nullable = false)
    private String qcResultId;
	
	// QC 항목ID
	@Column(name = "QC_ITEM_ID", length = 20, nullable = false)
    private String qcItemId;
	
	// 측정값
	@Column(name = "MEASURE_VALUE", length = 20)
    private String measureValue;
	
	// 판정
	@Column(name = "RESULT", length = 10)
    private String result;
	
	// 비고
	@Column(name = "REMARK", length = 200)
    private String remark;
	
	// 등록자
	@Column(name = "CREATED_USER", length = 7, nullable = false, updatable = false)
    private String createdUser;
	
	// 등록일시
	@CreatedDate
    @Column(name = "CREATED_DATE", nullable = false, updatable = false)
    private LocalDateTime createdDate;
	
	// 수정자
	@Column(name = "UPDATED_USER", length = 7)
    private String updatedUser;
	
	// 수정일시
	@LastModifiedDate
    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;
	
	// ---------------------------------------------------------------
	@Override
	public String getTargetTable() {
	    return "QC_RESULT_DETAIL";
	}

	@Override
	public Long getTargetTableId() {
	    return QcFileKeyUtil.toFileRefId(this.qcResultDtlId);
	}

	

}
