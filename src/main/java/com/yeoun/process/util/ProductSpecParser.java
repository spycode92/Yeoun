package com.yeoun.process.util;

/**
 * PRD_SPEC 문자열에서
 * - 용량(숫자), 단위(ml/g) 를 추출하는 유틸
 */
public class ProductSpecParser {

    /**
     * 용량 숫자 추출 (30, 50, 100, 5, 10 등)
     */
    public static int extractSize(String prdSpec) {
        if (prdSpec == null) return 0;

        String num = prdSpec.replaceAll("[^0-9]", "");
        if (num.isEmpty()) return 0;

        return Integer.parseInt(num);
    }

    /**
     * 단위 기반으로 제품 형태 결정
     * - ml -> LIQUID
     * - g  -> SOLID
     */
    public static String extractForm(String prdSpec, String itemName) {

        // 우선 ITEM_NAME
        if (itemName != null) {
            if ("LIQUID".equalsIgnoreCase(itemName)) return "LIQUID";
            if ("SOLID".equalsIgnoreCase(itemName)) return "SOLID";
        }

        // fallback: spec 문자열
        if (prdSpec != null) {
            if (prdSpec.contains("ml")) return "LIQUID";
            if (prdSpec.contains("g")) return "SOLID";
        }

        return "LIQUID"; // 기본값
    }
}
