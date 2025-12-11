package com.yeoun.sales.repository;

import com.yeoun.sales.dto.OrderItemDTO;
import com.yeoun.sales.entity.OrderItem;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	List<OrderItem> findByOrderId(String orderId);	
	//  제품ID로 주문 상세 찾기
    List<OrderItem> findByPrdId(String prdId);
	
	@Query(value = """
		    SELECT 
		        oi.ORDER_ITEM_ID AS orderItemId,
		        oi.ORDER_ID AS orderId,
		        oi.PRD_ID AS prdId,
		        pm.PRD_NAME AS prdName,
		        oi.ORDER_QTY AS orderQty,
		        o.DUE_DATE AS dueDate
		    FROM ORDER_ITEM oi
		    JOIN ORDERS o ON o.ORDER_ID = oi.ORDER_ID
		    JOIN PRODUCT_MST pm ON pm.PRD_ID = oi.PRD_ID
		    WHERE o.ORDER_STATUS = 'CONFIRMED'
		    ORDER BY o.DUE_DATE
		    """, nativeQuery = true)
		List<OrderItemDTO> findConfirmedOrderItems();	
	
	// 1) 확정된 수주를 제품별로 그룹화
	@Query("""
		    SELECT
		        p.prdId AS prdId,
		        p.prdName AS prdName,
		        SUM(oi.orderQty) AS totalOrderQty,
		        COUNT(oi) AS orderCount,
		        MIN(o.deliveryDate) AS earliestDeliveryDate
		    FROM OrderItem oi
		    JOIN oi.order o
		    JOIN oi.product p
		   WHERE oi.itemStatus = 'CONFIRMED'
		      AND (:group IS NULL OR p.itemName = :group)
		    GROUP BY p.prdId, p.prdName
		""")
		List<Map<String, Object>> findConfirmedGrouped(@Param("group") String group);


    // 2) 특정 제품에 대한 확정된 수주 상세 조회
	@Query(value = """
		    SELECT 
		        oi.ORDER_ITEM_ID          AS ORDER_ITEM_ID,
		        oi.ORDER_ID               AS ORDER_ID,
		        oi.PRD_ID                 AS PRD_ID,
		        pm.PRD_NAME               AS PRD_NAME,
		        oi.ORDER_QTY              AS ORDER_QTY,
		        TO_CHAR(o.DELIVERY_DATE, 'YYYY-MM-DD') AS dueDate,
		        c.CLIENT_NAME             AS CLIENT_NAME,
		        c.MANAGER_NAME            AS MANAGER_NAME,
		        c.MANAGER_TEL             AS MANAGER_TEL,
		        c.MANAGER_EMAIL           AS MANAGER_EMAIL
		    FROM ORDER_ITEM oi
		    JOIN ORDERS o       ON o.ORDER_ID = oi.ORDER_ID
		    JOIN CLIENT c       ON c.CLIENT_ID = o.CLIENT_ID
		    JOIN PRODUCT_MST pm ON pm.PRD_ID = oi.PRD_ID
		    WHERE o.ORDER_STATUS = 'CONFIRMED'
		      AND oi.ITEM_STATUS = 'CONFIRMED'   
		      AND oi.PRD_ID = :prdId
		    ORDER BY o.DELIVERY_DATE
		""", nativeQuery = true)
		List<Map<String,Object>> findItemsByProduct(@Param("prdId") String prdId);

	
    /*상태값 변경*/      
    @Modifying
    @Transactional
    @Query("""
        UPDATE OrderItem oi
        SET oi.itemStatus = 'PLANNED'
        WHERE oi.orderItemId = :orderItemId
    """)
    void updateStatusToPlanned(@Param("orderItemId") Long orderItemId);



}
