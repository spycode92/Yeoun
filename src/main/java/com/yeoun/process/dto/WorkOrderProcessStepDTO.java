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
	
	// 계획수량
	private Integer planQty;

	// 양품
	private Integer goodQty;
	
	// 불량
	private Integer defectQty;
	
	// 특이사항
	private String memo;
	
	// 기준배합량(1~2단계는 배합량, 3단계 이후는 개수 기준)
	private Double standardQty;
	
	// =================================
    // 시간 계산/지연 표시용
    /**
     * 해당 공정의 "예상 소요시간(분)"
     * - 배치 공정: 고정 분
     * - 단위 공정: EU 기반 계산된 분
     */
    private Long expectedMinutes;

    /**
     * 지연 여부
     * - startTime이 있는 공정만 판정
     * - (현재시각 - startTime) > expectedMinutes 이면 true
     */
    private Boolean delayed;
	
	// =================================
	// 화면용
	private boolean canStart;
	private boolean canFinish;

}
