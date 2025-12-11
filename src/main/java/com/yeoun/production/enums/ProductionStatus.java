package com.yeoun.production.enums;

public enum ProductionStatus {

    PLANNING("검토대기"),        // 생산팀 검토 전
    MATERIAL_PENDING("자재확보중"), // 재고 부족 또는 발주 필요
    IN_PROGRESS("생산중"),       // 실제 생산 중
    DONE("생산완료");            // 생산 완료

    private final String label;

    ProductionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
