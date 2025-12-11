package com.yeoun.order.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class WorkOrderDetailDTO {

    private String orderId;     // 작업지시 번호
    private String prdId;       // 제품코드
    private String prdName;     // 품명
    private String status;      // CREATE / IN_PROGRESS / RELEASED
    private Integer planQty;    // 계획 수량

    private String planDate;    // yyyy-MM-dd
    private String planTime;    // HH:mm ~ HH:mm

    private String lineName;    // 1번 라인
    private String routeId;     // 라우트 코드
    private String remark;		// 비고

    private List<WorkInfo> infos;     // 작업자 및 작업현황

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class WorkInfo {
        private String processId;
        private String processName;
        private String status;     // COMPLETED / IN_PROGRESS / PENDING
        private String workerId;
        private String workerName;
    }


}
