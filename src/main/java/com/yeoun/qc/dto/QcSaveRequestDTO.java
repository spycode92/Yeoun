package com.yeoun.qc.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

// QC 결과 저장 요청 DTO
@Data
public class QcSaveRequestDTO {
	
	private LocalDate inspectionDate; 
    private String inspectorId;
	
	// QC 헤더 영역에서 입력하는 값들
    private Integer goodQty;     // QC 통과 수량(EA)
    private Integer defectQty;   // QC 불량 수량(EA)
    private String failReason;   // 불량 사유(전량 FAIL 등)
    private String remark;       // 비고(검사 메모 등)
    private String overallResult;

    // QC 항목 상세 행들 (지금 쓰고 있는 DTO 재사용)
    private List<QcDetailRowDTO> detailRows;

}
