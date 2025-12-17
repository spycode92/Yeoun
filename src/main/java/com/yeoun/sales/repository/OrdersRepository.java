package com.yeoun.sales.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.yeoun.sales.dto.OrderListDTO;
import com.yeoun.sales.entity.Orders;
import com.yeoun.sales.enums.OrderStatus;

import jakarta.transaction.Transactional;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, String> {

    /* ============================================================
       1) 수주 목록 조회 
    ============================================================ */
	@Query("""
		    SELECT DISTINCT new com.yeoun.sales.dto.OrderListDTO(
		        o.orderId,
		        c.clientName,
		        o.orderDate,
		        o.deliveryDate,
		        o.orderStatus,
		        e.empName,
		        o.orderMemo
		    )
		    FROM Orders o
		    LEFT JOIN o.client c
		    LEFT JOIN Emp e 
		           ON e.empId = o.empId
		    LEFT JOIN OrderItem oi
		           ON oi.orderId = o.orderId
		    LEFT JOIN ProductMst p
		           ON p.prdId = oi.prdId
		    WHERE (:status IS NULL OR o.orderStatus = :status)
		      AND (:startDate IS NULL OR o.orderDate >= :startDate)
		      AND (:endDate IS NULL OR o.deliveryDate <= :endDate)
		      AND (
		            :keyword IS NULL OR
		            c.clientName LIKE CONCAT('%', :keyword, '%')
		         OR o.orderId LIKE CONCAT('%', :keyword, '%')
		         OR e.empName LIKE CONCAT('%', :keyword, '%')
		         OR p.prdName LIKE CONCAT('%', :keyword, '%')
		      )
		    ORDER BY o.orderDate DESC
		""")
		List<OrderListDTO> searchOrders(
		    @Param("status") OrderStatus status,
		    @Param("startDate") LocalDate startDate,
		    @Param("endDate") LocalDate endDate,
		    @Param("keyword") String keyword
		);


    /* ============================================================
       2) 주문번호 생성
    ============================================================ */
    @Query(value = """
        SELECT ORDER_ID
        FROM ORDERS
        WHERE ORDER_ID LIKE 'ORD' || :today || '%'
        ORDER BY ORDER_ID DESC
        FETCH FIRST 1 ROWS ONLY
    """, nativeQuery = true)
    String findLastOrderId(@Param("today") String today);


    /* ============================================================
       3) ⭐ 생산계획 대상 조회 
       - 입금 완료된 주문
       - 아직 계획 안 된 제품만
    ============================================================ */
    @Query(value = """
        SELECT 
            oi.ORDER_ITEM_ID AS orderItemId,
            oi.ORDER_ID      AS orderId,
            pr.PRD_ID        AS prdId,
            pr.PRD_NAME      AS prdName,
            pr.ITEM_NAME     AS itemName,
            oi.ORDER_QTY     AS orderQty,
            o.DELIVERY_DATE  AS deliveryDate
        FROM ORDER_ITEM oi
        JOIN PRODUCT_MST pr ON pr.PRD_ID = oi.PRD_ID
        JOIN ORDERS o       ON o.ORDER_ID = oi.ORDER_ID
        WHERE (:group IS NULL OR :group = '' OR pr.ITEM_NAME = :group)
          AND o.ORDER_STATUS = 'CONFIRMED'
          AND oi.ITEM_STATUS = 'CONFIRMED'
        ORDER BY o.DELIVERY_DATE
    """, nativeQuery = true)
    List<Map<String, Object>> findOrderItemsForPlan(@Param("group") String group);


    /* ============================================================
       4) 단건 조회 
    ============================================================ */
    Optional<Orders> findByOrderId(String orderId);


    /* ============================================================
       5) 주문 상태 확정 (입금확인용)
    ============================================================ */
    @Modifying
    @Transactional
    @Query("""
        UPDATE Orders o
        SET o.orderStatus = 'CONFIRMED'
        WHERE o.orderId = :orderId
    """)
    int updateOrderStatusToConfirmed(@Param("orderId") String orderId);
}
