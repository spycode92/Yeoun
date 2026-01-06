package com.yeoun.lot.constant;

import com.yeoun.process.constant.ProcessStepStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// LOT 상태를 관리할 enum 정의
@Getter
@RequiredArgsConstructor
public enum LotStatus {
	
	NEW("생성"),
	IN_STOCK("창고 재고"),
	IN_PROCESS("공정 진행 중"),
	ISSUED("생산 투입"),
	SCRAPPED("폐기"),
	PROD_DONE("생산 완료"),
	SHIPPED("출하 완료");
	
	private final String label;
	
	public String getCode() {
		return this.name();
	}
	
	public static LotStatus fromCode(String code) {
        if (code == null) return null;
        try {
            return LotStatus.valueOf(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
