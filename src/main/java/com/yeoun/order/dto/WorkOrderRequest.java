package com.yeoun.order.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class WorkOrderRequest {

    @NotNull(message = "생산 계획은 필수 선택값입니다.")
    private String planId;

    @NotNull(message = "공정 ID는 필수 입력값입니다.")
    private String routeId;

    @NotNull(message = "품명은 필수 입력값입니다.")
    private String prdId;

    @NotNull(message = "라인 지정은 필수입니다.")
    private String lineId;

    @Positive(message = "계획 수량 입력은 필수입니다.")
    private Integer planQty;

    @Future(message = "예정 시작일을 미래로 설정해주세요.")
    private LocalDateTime planStartDate;

    @Future(message = "예정 종료일을 미래로 설정해주세요.")
    private LocalDateTime planEndDate;

    private String remark;

    // 라인작업자 정보
    @NotNull(message = "블렌딩 작업자 선택은 필수입니다.")
    private String prcBld;

    @NotNull(message = "여과기 작업자 선택은 필수입니다.")
    private String prcFlt;

    @NotNull(message = "충전 작업자 선택은 필수입니다.")
    private String prcFil;

    @NotNull(message = "캡핑 작업자 선택은 필수입니다.")
    private String prcCap;
    
    @NotNull(message = "품질관리자 선택은 필수입니다.")
    private String prcQc;

    @NotNull(message = "라벨링 작업자 선택은 필수입니다.")
    private String prcLbl;

    @AssertTrue(message = "종료일은 시작일보다 이후여야 합니다.")
    public boolean isValidDateRange() {
        if (planStartDate == null || planEndDate == null) return true;
        return planEndDate.isAfter(planStartDate);
    }

}













