package com.yeoun.sales.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.yeoun.sales.dto.OrderShipmentHistoryDTO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class OrderQueryRepository {

    @PersistenceContext
    private EntityManager em;

    public List<OrderShipmentHistoryDTO> findShipmentHistoryByOrderId(String orderId) {

        String sql = """
            SELECT
                p.PRD_NAME,
                 CAST(oi.OUTBOUND_AMOUNT AS NUMBER(15,2)) AS OUTBOUND_AMOUNT,
                oi.LOT_NO,
                o.OUTBOUND_DATE,
                s.SHIPMENT_ID,
                s.SHIPMENT_STATUS
            FROM OUTBOUND_ITEM oi
            JOIN OUTBOUND o ON oi.OUTBOUND_ID = o.OUTBOUND_ID
            JOIN SHIPMENT s ON o.SHIPMENT_ID = s.SHIPMENT_ID
            JOIN PRODUCT_MST p ON oi.ITEM_ID = p.PRD_ID
            WHERE s.ORDER_ID = :orderId
            ORDER BY o.OUTBOUND_DATE
        """;

        return em.createNativeQuery(sql, OrderShipmentHistoryDTO.class)
                 .setParameter("orderId", orderId)
                 .getResultList();
    }
}
