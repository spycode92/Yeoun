package com.yeoun.sales.repository;

import com.yeoun.sales.dto.ShipmentCompletedItemDTO;
import com.yeoun.sales.dto.ShipmentDetailItemDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ShipmentDetailQueryRepository {

    @PersistenceContext
    private EntityManager em;

    /* =========================================================
       1) 출하 전 상태 (WAITING / RESERVED / LACK)
    ========================================================= */

    public List<ShipmentDetailItemDTO> findItems(String orderId) {

        String sql = """
            SELECT
                p.PRD_NAME          AS prdName,
                oi.ORDER_QTY        AS orderQty,
                NVL((
                    SELECT SUM(iv.IV_AMOUNT - iv.EXPECT_OB_AMOUNT)
                    FROM INVENTORY iv
                    WHERE iv.ITEM_ID = oi.PRD_ID
                ), 0) AS stockQty,
                0 AS dummy
            FROM ORDER_ITEM oi
            JOIN PRODUCT_MST p ON oi.PRD_ID = p.PRD_ID
            WHERE oi.ORDER_ID = :orderId
        """;

        return em.createNativeQuery(sql, ShipmentDetailItemDTO.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }

    public Object[] findHeader(String orderId) {

        String sql = """
            SELECT
                o.ORDER_ID,
                c.CLIENT_NAME,
                TO_CHAR(o.DELIVERY_DATE, 'YYYY-MM-DD') AS dueDate,
                CASE
                    WHEN EXISTS (
                        SELECT 1 FROM SHIPMENT s
                        WHERE s.ORDER_ID = o.ORDER_ID
                          AND s.SHIPMENT_STATUS = 'SHIPPED'
                    ) THEN 'SHIPPED'
                    WHEN EXISTS (
                        SELECT 1 FROM SHIPMENT s
                        WHERE s.ORDER_ID = o.ORDER_ID
                          AND s.SHIPMENT_STATUS IN ('RESERVED','PENDING')
                    ) THEN 'RESERVED'
                    ELSE 'WAITING'
                END AS status
            FROM ORDERS o
            JOIN CLIENT c ON o.CLIENT_ID = c.CLIENT_ID
            WHERE o.ORDER_ID = :orderId
        """;

        return (Object[]) em.createNativeQuery(sql)
                .setParameter("orderId", orderId)
                .getSingleResult();
    }

    /* =========================================================
       2) 출하 완료 상태 (SHIPPED)
       - 부분출하 고려: 헤더는 여러 건 가능
    ========================================================= */

    /**
     * ✅ 출하완료 헤더들 (orderId → shipment/outbound 여러 건 가능)
     * - 최신 1건은 Service에서 선택
     */
    public List<Object[]> findCompletedHeadersByOrderId(String orderId) {

        String sql = """
            SELECT
                s.SHIPMENT_ID,
                o.OUTBOUND_DATE,
                e.EMP_NAME,
                s.TRACKING_NUMBER     
            FROM SHIPMENT s
            JOIN OUTBOUND o ON s.SHIPMENT_ID = o.SHIPMENT_ID
            JOIN EMP e ON o.PROCESS_BY = e.EMP_ID
            WHERE s.ORDER_ID = :orderId
              AND s.SHIPMENT_STATUS = 'SHIPPED'
            ORDER BY o.OUTBOUND_DATE DESC
        """;

        return em.createNativeQuery(sql)
                .setParameter("orderId", orderId)
                .getResultList();
    }


    /**
     * 출하완료 상세 품목 + LOT 이력 (orderId 기준 전체)
     */
    public List<ShipmentCompletedItemDTO> findCompletedItemsByOrderId(String orderId) {

        String sql = """
            SELECT
                p.PRD_NAME          AS prdName,
                oi.LOT_NO           AS lotNo,
                oi.OUTBOUND_AMOUNT  AS outboundAmount,
                o.OUTBOUND_DATE     AS outboundDate
            FROM OUTBOUND_ITEM oi
            JOIN OUTBOUND o ON oi.OUTBOUND_ID = o.OUTBOUND_ID
            JOIN SHIPMENT s ON o.SHIPMENT_ID = s.SHIPMENT_ID
            JOIN PRODUCT_MST p ON oi.ITEM_ID = p.PRD_ID
            WHERE s.ORDER_ID = :orderId
            ORDER BY o.OUTBOUND_DATE DESC, oi.OUTBOUND_ITEM_ID
        """;

        return em.createNativeQuery(sql, ShipmentCompletedItemDTO.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }
}
