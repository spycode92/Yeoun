package com.yeoun.lot.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LotProcessDetailDTO {
	
    private String processId;      			// 공정 ID
    private String processName;    			// 공정명
    private String status;					// 공정 진행 상태
    private String deptName;				// 부서명
    private String workerId;				// 작업자 ID
    private String workerName;				// 작업자명
    private LocalDateTime startTime;		// 공정 시작일시
    private LocalDateTime endTime;			// 공정 종료일시
    private Long goodQty;					// 양품수량
    private Long defectQty;					// 불량수량
    private Double defectRate;				// 불량률
    private String lineId;					// 라인ID
    private Long qcResultId;				// QC 결과
    
    // 설비 정보
    private List<LotEquipInfoDTO> equipments;


}
