package com.yeoun.sales.service;

import com.yeoun.sales.dto.ShipmentCompletedItemDTO;
import com.yeoun.sales.dto.ShipmentDetailDTO;
import com.yeoun.sales.dto.ShipmentDetailItemDTO;
import com.yeoun.sales.repository.ShipmentDetailQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipmentDetailService {

    private final ShipmentDetailQueryRepository detailRepo;

    /**
     * 출하 상세 조회
     *
     * - SHIPPED : 출하 기준 (LOT / 출하일 / 처리자)
     * - 그 외   : 주문 기준 (기존 상세 유지)
     */
    public ShipmentDetailDTO getDetail(String orderId) {

        /* =========================================================
           1️⃣ 주문 기준 헤더 조회 (공통)
        ========================================================= */
        Object[] h = detailRepo.findHeader(orderId);

        String status = (String) h[3];

        ShipmentDetailDTO dto = ShipmentDetailDTO.builder()
                .orderId((String) h[0])
                .clientName((String) h[1])
                .dueDate((String) h[2])
                .status(status)
                .build();

        /* =========================================================
           2️⃣ 상태 기준 분기
        ========================================================= */

     // ✅ 출하완료
        if ("SHIPPED".equals(status)) {

            List<Object[]> headers =
                    detailRepo.findCompletedHeadersByOrderId(orderId);

            if (!headers.isEmpty()) {

                Object[] ch = headers.get(0); // 최신 출하 1건

                // 0: SHIPMENT_ID
                dto.setShipmentId((String) ch[0]);

                // 1: OUTBOUND_DATE
                Timestamp ts = (Timestamp) ch[1];
                dto.setOutboundDate(ts.toLocalDateTime());

                // 2: EMP_NAME
                dto.setProcessBy((String) ch[2]);
                
                // 3: TrackingNumber
                dto.setTrackingNumber((String) ch[3]);
            }            

            /* -----------------------------------------
               (2) LOT 기반 출하 품목 조회
            ----------------------------------------- */
            List<ShipmentCompletedItemDTO> completedItems =
                    detailRepo.findCompletedItemsByOrderId(orderId);

            dto.setCompletedItems(completedItems); // ⭐️ 분리

        } else {

            /* -----------------------------------------
               출하 전 상태 (WAITING / RESERVED / LACK)
            ----------------------------------------- */
            List<ShipmentDetailItemDTO> items =
                    detailRepo.findItems(orderId);

            dto.setItems(items);
        }

        return dto;
    }
}
