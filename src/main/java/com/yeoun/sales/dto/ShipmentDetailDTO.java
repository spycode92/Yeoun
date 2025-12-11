package com.yeoun.sales.dto;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentDetailDTO {

    private String orderId;
    private String clientName;
    private String dueDate;
    private String status;
    private List<ShipmentDetailItemDTO> items;
}
