package com.yeoun.pay.enums;

public enum CalcMethod {

    FIXED("고정금액"),
    RATE("비율"),
    FORMULA("수식"),
    EXTERNAL("외부연동");

    private final String kor;

    CalcMethod(String kor) {
        this.kor = kor;
    }

    public String getKor() {
        return kor;
    }
}
