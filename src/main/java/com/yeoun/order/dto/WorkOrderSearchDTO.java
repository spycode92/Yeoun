package com.yeoun.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class WorkOrderSearchDTO {
    private String keyword;         // 품명/작업지시번호 검색
    private String status;          // 상태 검색
    private String startDateFrom;   // 시작일자 FROM
}
