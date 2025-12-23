package com.yeoun.production.repository;

import com.yeoun.production.dto.ProductionPlanListDTO;
import com.yeoun.production.entity.ProductionPlan;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductionPlanRepository extends JpaRepository<ProductionPlan, String> {

    // 오늘 날짜 기준 마지막 PLAN_ID 찾기
    @Query(value = """
        SELECT PLAN_ID
        FROM PRODUCTION_PLAN
        WHERE PLAN_ID LIKE :prefix || '%'
        ORDER BY PLAN_ID DESC
        FETCH FIRST 1 ROWS ONLY
        """, nativeQuery = true)
    String findLastPlanId(@Param("prefix") String prefix);
    
    List<ProductionPlan> findAllByOrderByCreatedAtDesc();
    
    
    /* 생산계획 리스트 조회 */
//    @Query(value = """
//    	   SELECT
//			    p.PLAN_ID            AS planId,
//			    TO_CHAR(p.CREATED_AT, 'YYYY-MM-DD HH24:MI:SS') AS createdAt,
//			    COALESCE(MIN(pr.PRD_NAME), '미정') AS itemName,
//			    COALESCE(p.PLAN_QTY, 0) AS totalQty,
//			    p.STATUS             AS status,
//			    p.PLAN_MEMO          AS memo
//			FROM PRODUCTION_PLAN p
//			LEFT JOIN PRODUCTION_PLAN_ITEM i 
//			  ON p.PLAN_ID = i.PLAN_ID
//			LEFT JOIN PRODUCT_MST pr
//			  ON i.PRD_ID = pr.PRD_ID
//			GROUP BY 
//			    p.PLAN_ID,
//			    p.CREATED_AT,
//			    p.PLAN_QTY,
//			    p.STATUS,
//			    p.PLAN_MEMO
//			ORDER BY p.CREATED_AT DESC
//
//    	""", nativeQuery = true)
//    	List<ProductionPlanListDTO> findPlanList();
    
    @Query(value = """
    	    SELECT
    	        p.PLAN_ID AS planId,
    	        TO_CHAR(p.CREATED_AT, 'YYYY-MM-DD HH24:MI:SS') AS createdAt,
    	        COALESCE(MIN(pr.PRD_NAME), '미정') AS itemName,
    	        COALESCE(p.PLAN_QTY, 0) AS totalQty,
    	        p.STATUS AS status,
    	        p.PLAN_MEMO AS memo,    	       
    	        e.EMP_NAME AS createdByName

    	    FROM PRODUCTION_PLAN p

    	    LEFT JOIN EMP e
    	      ON p.CREATED_BY = e.EMP_ID

    	    LEFT JOIN PRODUCTION_PLAN_ITEM i 
    	      ON p.PLAN_ID = i.PLAN_ID

    	    LEFT JOIN PRODUCT_MST pr
    	      ON i.PRD_ID = pr.PRD_ID

    	    GROUP BY 
    	        p.PLAN_ID,
    	        p.CREATED_AT,
    	        p.PLAN_QTY,
    	        p.STATUS,
    	        p.PLAN_MEMO,
    	        e.EMP_NAME

    	    ORDER BY p.CREATED_AT DESC
    	""", nativeQuery = true)
    	List<ProductionPlanListDTO> findPlanList();



    //*BOM 조회
    @Query(value = """
    	    SELECT 
    	        b.MAT_ID AS matId,
    	        b.MAT_QTY AS matQty
    	    FROM BOM_MST b
    	    WHERE b.PRD_ID = :prdId
    	    """,
    	    nativeQuery = true)
    	List<Map<String, Object>> findBomItems(@Param("prdId") String prdId);

}




