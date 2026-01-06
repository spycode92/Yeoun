package com.yeoun.production.repository;

import com.yeoun.production.entity.ProductionPlanItem;
import com.yeoun.production.enums.ProductionStatus;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductionPlanItemRepository extends JpaRepository<ProductionPlanItem, String> {

    @Query(value = """
        SELECT PLAN_ITEM_ID
        FROM PRODUCTION_PLAN_ITEM
        WHERE PLAN_ITEM_ID LIKE :prefix || '%'
        ORDER BY PLAN_ITEM_ID DESC
        FETCH FIRST 1 ROWS ONLY
        """, nativeQuery = true)
    String findLastPlanItemId(@Param("prefix") String prefix);
    
    
    
    List<ProductionPlanItem> findByPlanId(String planId);
    
    //제품이 모두 done상태만 예약하기
    
    @Query("""
    	    SELECT 
    	        CASE 
    	            WHEN COUNT(p) = SUM(CASE WHEN p.status = 'DONE' THEN 1 ELSE 0 END)
    	            THEN true
    	            ELSE false
    	        END
    	    FROM ProductionPlanItem p
    	    WHERE p.orderItemId IN (
    	        SELECT oi.orderItemId 
    	        FROM OrderItem oi 
    	        WHERE oi.orderId = :orderId
    	    )
    	""")
    	Boolean isAllDoneByOrderId(@Param("orderId") String orderId);

}
