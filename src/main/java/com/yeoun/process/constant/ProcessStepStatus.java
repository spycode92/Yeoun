package com.yeoun.process.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 공정 단계 상태를 관리할 enum 정의
@Getter
@RequiredArgsConstructor
public enum ProcessStepStatus {
	
	READY("대기"),
	IN_PROGRESS("진행 중"),
	DONE("완료"),
	QC_PENDING("QC 대기"),
	SKIPPED("중단");
	
	private final String label;
	
	public String getCode() {
		return this.name();
	}
	
	public static ProcessStepStatus fromCode(String code) {
        if (code == null) return null;
        try {
            return ProcessStepStatus.valueOf(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
