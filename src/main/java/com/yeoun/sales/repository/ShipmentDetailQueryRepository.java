package com.yeoun.sales.repository;

import com.yeoun.sales.dto.ShipmentDetailItemDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ShipmentDetailQueryRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * 단일 주문의 상세 품목 리스트 조회
     */
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
                
                /* 출하 가능 여부 */
                CASE 
                  WHEN NVL((
                      SELECT SUM(iv.IV_AMOUNT - iv.EXPECT_OB_AMOUNT)
                      FROM INVENTORY iv WHERE iv.ITEM_ID = oi.PRD_ID
                  ), 0) >= oi.ORDER_QTY 
                  THEN 1 ELSE 0
                END AS reservable

            FROM ORDER_ITEM oi
            JOIN PRODUCT_MST p ON oi.PRD_ID = p.PRD_ID
            WHERE oi.ORDER_ID = :orderId
        """;

        return em.createNativeQuery(sql, "ShipmentDetailItemMapping")
                 .setParameter("orderId", orderId)
                 .getResultList();
    }


    /**
     * 주문 헤더 조회
     */
    public Object[] findHeader(String orderId) {

        String sql = """
            SELECT 
                o.ORDER_ID,
                c.CLIENT_NAME,
                TO_CHAR(o.DELIVERY_DATE, 'YYYY-MM-DD') AS dueDate,
                
                CASE 
                    WHEN EXISTS (SELECT 1 FROM SHIPMENT s WHERE s.ORDER_ID = o.ORDER_ID AND s.SHIPMENT_STATUS='SHIPPED')
                        THEN 'SHIPPED'
                    WHEN EXISTS (SELECT 1 FROM SHIPMENT s WHERE s.ORDER_ID = o.ORDER_ID AND s.SHIPMENT_STATUS='RESERVED')
                        THEN 'RESERVED'
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
}
