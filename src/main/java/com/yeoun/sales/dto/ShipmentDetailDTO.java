package com.yeoun.sales.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentDetailDTO {

    /* =========================
       공통 (모든 상태)
    ========================= */
    private String orderId;
    private String clientName;
    private String dueDate;
    private String status;

    /* =========================
       출하완료(COMPLETED) 전용
    ========================= */
    private String shipmentId;
    private LocalDateTime outboundDate;
    private String processBy;
    private String trackingNumber;
    
    private List<ShipmentCompletedItemDTO> completedItems;

    /* =========================
       상세 품목
    ========================= */
    private List<ShipmentDetailItemDTO> items;
}
