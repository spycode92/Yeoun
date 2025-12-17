package com.yeoun.process.constant;

import java.util.Map;

/**
 * 공정별 표준 소요시간 기준 정의
 *
 * - 공정 예상시간 계산 기준
 * - 지연 여부 판단 기준
 * - DB 컬럼 없이 코드 상수로 관리
 */
public final class ProcessTimeStandard {

    // 배치(묶음) 공정 고정 시간 (분)
    public static final Map<String, Integer> BATCH_MINUTES = Map.of(
        "PRC-BLD", 30,  // 블렌딩
        "PRC-FLT", 15,  // 여과
        "PRC-QC",  20   // QC 검사
    );

    // 단위(개별) 공정 EU당 처리 시간 (초)
    public static final Map<String, Integer> UNIT_SECONDS_PER_EU = Map.of(
        "PRC-FIL", 8,   // 충전
        "PRC-CAP", 6,   // 캡/펌프
        "PRC-LBL", 10   // 라벨링
    );

    // 유틸 클래스 -> 인스턴스 생성 방지
    private ProcessTimeStandard() {}
}
