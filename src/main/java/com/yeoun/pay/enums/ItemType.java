package com.yeoun.pay.enums;

public enum ItemType {
    EARNING("지급"),
    DEDUCTION("공제");

    private final String kor;
    ItemType(String kor) { this.kor = kor; }
    public String getKor() { return kor; }
}
