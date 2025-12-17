package com.yeoun.sales.repository;

import com.yeoun.sales.dto.ShipmentListDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ShipmentQueryRepository {

    @PersistenceContext
    private EntityManager em;

    public List<ShipmentListDTO> search(
            String startDate,
            String endDate,
            String keyword,
            List<String> statusList
    ) {

        StringBuilder sql = new StringBuilder("""
            SELECT
                oi.ORDER_ID              AS orderId,
                c.CLIENT_NAME            AS clientName,
                p.PRD_NAME               AS prdName,
                oi.ORDER_QTY             AS orderQty,

                NVL((
                    SELECT SUM(iv.IV_AMOUNT - iv.EXPECT_OB_AMOUNT)
                    FROM INVENTORY iv
                    WHERE iv.ITEM_ID = oi.PRD_ID
                ), 0) AS stockQty,

                o.DELIVERY_DATE          AS dueDate,

               
                CASE 
                    WHEN EXISTS (
                        SELECT 1 FROM SHIPMENT s
                        WHERE s.ORDER_ID = oi.ORDER_ID
                          AND s.SHIPMENT_STATUS = 'SHIPPED'
                    ) THEN 'SHIPPED'
                    
                     WHEN EXISTS (
                        SELECT 1 FROM SHIPMENT s
                        WHERE s.ORDER_ID = oi.ORDER_ID
                          AND s.SHIPMENT_STATUS = 'PENDING'
                    ) THEN 'PENDING'

                    WHEN EXISTS (
                        SELECT 1 FROM SHIPMENT s
                        WHERE s.ORDER_ID = oi.ORDER_ID
                          AND s.SHIPMENT_STATUS = 'RESERVED'
                    ) THEN 'RESERVED'

                    WHEN NVL((
                        SELECT SUM(iv.IV_AMOUNT - iv.EXPECT_OB_AMOUNT)
                        FROM INVENTORY iv WHERE iv.ITEM_ID = oi.PRD_ID
                    ), 0) < oi.ORDER_QTY
                    THEN 'LACK'

                    ELSE 'WAITING'
                END AS status,

               
                CASE
                    WHEN 
                        NOT EXISTS (SELECT 1 FROM SHIPMENT s WHERE s.ORDER_ID = oi.ORDER_ID)
                        AND NVL((
                            SELECT SUM(iv.IV_AMOUNT - iv.EXPECT_OB_AMOUNT)
                            FROM INVENTORY iv WHERE iv.ITEM_ID = oi.PRD_ID
                        ), 0) >= oi.ORDER_QTY
                    THEN 1 ELSE 0
                END AS reservable,

                
                CASE
                    WHEN EXISTS (
                        SELECT 1
                        FROM ORDER_ITEM oi2
                        WHERE oi2.ORDER_ID = oi.ORDER_ID
                        AND (
                           
                            NVL((
                                SELECT SUM(iv2.IV_AMOUNT - iv2.EXPECT_OB_AMOUNT)
                                FROM INVENTORY iv2 WHERE iv2.ITEM_ID = oi2.PRD_ID
                            ), 0) < oi2.ORDER_QTY
                            
                             OR EXISTS (
				                SELECT 1
				                FROM SHIPMENT s2
				                WHERE s2.ORDER_ID = oi2.ORDER_ID
				                AND s2.SHIPMENT_STATUS IN ('RESERVED', 'PENDING', 'SHIPPED')
				          )				                
                        )
                    ) THEN 0  
                    ELSE 1     
                END AS reservable_group

            FROM ORDER_ITEM oi
            JOIN ORDERS o       ON oi.ORDER_ID = o.ORDER_ID
            JOIN PRODUCT_MST p  ON oi.PRD_ID = p.PRD_ID
            JOIN CLIENT c       ON o.CLIENT_ID = c.CLIENT_ID
            WHERE 1=1
        """);

        if (startDate != null && !startDate.isEmpty()) {
            sql.append(" AND o.DELIVERY_DATE >= TO_DATE(:startDate, 'YYYY-MM-DD') ");
        }

        if (endDate != null && !endDate.isEmpty()) {
            sql.append(" AND o.DELIVERY_DATE <= TO_DATE(:endDate, 'YYYY-MM-DD') ");
        }

        if (keyword != null && !keyword.isEmpty()) {
            sql.append("""
                AND (
                    oi.ORDER_ID LIKE '%' || :keyword || '%'
                    OR c.CLIENT_NAME LIKE '%' || :keyword || '%'
                    OR p.PRD_NAME LIKE '%' || :keyword || '%'
                )
            """);
        }

        if (statusList != null && !statusList.isEmpty() && !statusList.contains("ALL")) {
            sql.append("""
                AND (
                    CASE 
                        WHEN EXISTS (
                            SELECT 1 FROM SHIPMENT s 
                            WHERE s.ORDER_ID = oi.ORDER_ID
                              AND s.SHIPMENT_STATUS = 'SHIPPED'
                        ) THEN 'SHIPPED'

                        WHEN EXISTS (
                            SELECT 1 FROM SHIPMENT s 
                            WHERE s.ORDER_ID = oi.ORDER_ID
                              AND s.SHIPMENT_STATUS IN ('RESERVED', 'PENDING','SHIPPED')
                        ) THEN 'RESERVED'

                        WHEN NVL((
                            SELECT SUM(iv.IV_AMOUNT - iv.EXPECT_OB_AMOUNT)
                            FROM INVENTORY iv WHERE iv.ITEM_ID = oi.PRD_ID
                        ), 0) < oi.ORDER_QTY
                        THEN 'LACK'

                        ELSE 'WAITING'
                    END
                ) IN (:statusList)
            """);
        }


        sql.append(" ORDER BY o.DELIVERY_DATE ");

        var query = em.createNativeQuery(sql.toString(), ShipmentListDTO.class);

        if (startDate != null && !startDate.isEmpty()) query.setParameter("startDate", startDate);
        if (endDate != null && !endDate.isEmpty()) query.setParameter("endDate", endDate);
        if (keyword != null && !keyword.isEmpty()) query.setParameter("keyword", keyword);
        if (statusList != null && !statusList.isEmpty() && !statusList.contains("ALL")) {
            query.setParameter("statusList", statusList);
        }


        return query.getResultList();
    }
    
    
}
