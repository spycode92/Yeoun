package com.yeoun.qc.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

// QC 결과 상세 모달
@Getter
@Setter
public class QcResultViewDTO {
	
	private Long qcResultId;
    private String orderId;

    // 제품 정보 (WorkOrder에서 끌어옴)
    private String productCode;
    private String productName;
    private Integer planQty;

    // LOT / 검사 정보
    private String lotNo;
    private LocalDate inspectionDate;

    private String inspectorId;
    private String inspectorName;

    // 전체 판정
    private String overallResult;   // PASS / FAIL / PENDING
    private String failReason;

    private Integer inspectionQty;
    private Integer goodQty;
    private Integer defectQty;

    // 디테일 목록 (공용 DTO)
    private List<QcDetailRowDTO> details;

}
