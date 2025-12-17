package com.yeoun.masterData.repository;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.yeoun.masterData.entity.ProductMst;
import com.yeoun.masterData.entity.RouteHeader;

@Repository
public interface RouteHeaderRepository extends JpaRepository<RouteHeader, String> {
	//제품별 공정 라우트
	//제품코드 관리드롭다운
	@Query(value ="""
			 SELECT * FROM product_mst
			 WHERE use_yn = 'Y'
			""",nativeQuery = true)
	List<ProductMst> findAllPrd();
    

	// 제품+라우트로 조회 (빈값일 경우 전체조회) - useYn='Y' 필터 추가
	@Query(value = """
		    SELECT
		        rh.*,  -- ROUTE_HEADER 테이블의 모든 컬럼
		        p.* -- PRODUCT_MST 테이블의 모든 컬럼
		    FROM
		        ROUTE_HEADER rh
		    LEFT JOIN
		        PRODUCT_MST p ON rh.prd_id = p.prd_id
		    WHERE 
		        (:prdId IS NULL OR :prdId = '' OR p.prd_id = :prdId)
		    AND 
		        rh.use_yn = 'Y'
		    AND 
		        (:routeName IS NULL OR :routeName = '' OR LOWER(rh.route_name) LIKE LOWER('%' || :routeName || '%'))
		    """, nativeQuery = true)
	List<Map<String, Object>> findByPrdIdAndRouteName(@Param("prdId") String prdId, @Param("routeName") String routeName);
}
