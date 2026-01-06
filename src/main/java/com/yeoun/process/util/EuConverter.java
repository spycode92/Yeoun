package com.yeoun.process.util;

/**
 * Eu(Effort Unit) 환산 유틸
 * 
 * 목적
 * - 제품의 용량(ml / g)을 공정 계산용 표준 단위(EU)로 반환
 * 
 * 기준
 * - 액체 향수: 50ml = 1 EU
 * - 고체 향수: 10g = 1 EU
 * 
 * 사용 이유
 * - 용량이 다른 제품도 동일한 계산 로직으로 처리
 * - 공정별 시간 계산을 단순화
 */
public class EuConverter {
	
	private EuConverter() {}
	
	public static double toEU(String form, int size) {
		if (form == null) return 1.0;	// 안전 기본값
		
		// 액체 향수 기준
		if ("LIQUID".equalsIgnoreCase(form)) {
			return switch (size) {
				case 30 -> 0.6;
				case 50 -> 1.0;
				case 100 -> 2.0;
				default -> 1.0;
			};
		}
		
		// 고체 향수 기준
        if ("SOLID".equalsIgnoreCase(form)) {
            return switch (size) {
                case 5  -> 0.5;
                case 10 -> 1.0;
                default -> 1.0;
            };
        }

        return 1.0;
	}
}
