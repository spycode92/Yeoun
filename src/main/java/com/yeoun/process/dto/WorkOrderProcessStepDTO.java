package com.yeoun.process.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderProcessStepDTO {
	
	private String orderId;
	
	// 단계순서
	private Integer stepSeq;
	
	// 공정코드
	private String processId;
	
	// 공정명
	private String processName;
	
	// 공정 상태
	private String status;
	
	// 시작시간
	private LocalDateTime startTime;
	
	// 종료시간
	private LocalDateTime endTime;
	
	// 양품
	private Integer goodQty;
	
	// 불량
	private Integer defectQty;
	
	// 특이사항
	private String memo;
	
	// =================================
	// 화면용
	private boolean canStart;
	private boolean canFinish;

}
