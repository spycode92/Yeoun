package com.yeoun.pay.enums;

public enum ItemGroup {

    ALLOWANCE("수당"),
    INCENTIVE("인센티브"),
    BONUS("보너스"),
    DEDUCTION("공제"),
    TAX("세금");

    private final String kor;

    ItemGroup(String kor) {
        this.kor = kor;
    }

    public String getKor() {
        return kor;
    }
}
