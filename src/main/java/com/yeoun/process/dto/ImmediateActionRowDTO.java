package com.yeoun.process.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 대시보드 "즉시 조치 작업 리스트" 한 행 DTO
// - 지연 / QC대기 / 장기진행 항목을 하나의 리스트
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImmediateActionRowDTO {
	
	// 우선순위 (1:지연, 2:QC대기, 3:장기진행)
    private int priority;

    // 타입 코드 (DELAY / QC_PENDING / LONG)
    private String actionType;

    // 작업지시번호
    private String orderId;

    // 제품명 / 제품코드(또는 품목코드)
    private String itemName;
    private String itemCode;

    // 현재 공정명 (지연은 없을 수 있어 "-" 로 처리)
    private String processName;

    // 화면용 상태 라벨 ("지연" / "QC대기" / "진행")
    private String statusLabel;

    // 작업지시 예정 종료시간(지연 판단 근거 / 화면 표시용)
    private LocalDateTime planEndTime;

    // 경과/대기/지연 시간(분)
    private long elapsedMin;

    // 공정 단계(상세 이동 시 사용 가능)
    private Integer stepSeq;

}
