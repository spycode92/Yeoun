package com.yeoun.production.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ProductionPlanListDTO {
    String getPlanId();
    String getCreatedAt();
    String getItemName();
    BigDecimal getTotalQty();
    String getStatus();
    String getMemo();
}
