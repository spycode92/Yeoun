package com.yeoun.sales.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ShipmentCompletedItemDTO {

    private String prdName;
    private String lotNo;
    private BigDecimal outboundAmount;
    private LocalDateTime outboundDate;

    // ✅ NativeQuery 전용 생성자 (중요)
    public ShipmentCompletedItemDTO(
            String prdName,
            String lotNo,
            Number outboundAmount,      // 🔥 BigDecimal 금지
            java.sql.Timestamp outboundDate // 🔥 LocalDateTime 금지
    ) {
        this.prdName = prdName;
        this.lotNo = lotNo;

        // Number → BigDecimal 안전 변환
        this.outboundAmount = outboundAmount == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(outboundAmount.doubleValue());

        // Timestamp → LocalDateTime 변환
        this.outboundDate = outboundDate == null
                ? null
                : outboundDate.toLocalDateTime();
    }
}
