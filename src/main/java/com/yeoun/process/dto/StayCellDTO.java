package com.yeoun.process.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 대시보드 라인 공정별 체류
@Getter 
@Setter
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class StayCellDTO {
	
	private String lineId;
    private String lineName;

    private Integer stepSeq;
    private String stepName;

    private long stayMin;          // IN_PROGRESS/QC_PENDING 체류(분)
    private long inProgressCnt;    // IN_PROGRESS 건수
    private long qcPendingCnt;	   // QC_PENDING
    
    private long readyCnt;         // READY 건수(표시용)
    private boolean hasReady;      // READY 존재 여부

    private long startableCnt;     // "시작대기" 건수 (READY 중 이전단계 DONE이면)

    private String level;          // OK/WARN/DELAY

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
}
