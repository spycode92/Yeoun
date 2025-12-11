package com.yeoun.masterData.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.yeoun.masterData.entity.ProcessMst;
import com.yeoun.masterData.entity.ProductMst;
import com.yeoun.masterData.entity.RouteHeader;

@Repository
public interface RouteHeaderRepository extends JpaRepository<RouteHeader, String> {
	//제품별 공정 라우트
	//제품코드 관리드롭다운
	@Query(value ="""
			 SELECT * FROM product_mst
			""",nativeQuery = true)
	List<ProductMst> findAllPrd();
    

	// 제품+라우트로 조회 (빈값일 경우 전체조회) - useYn='Y' 필터 추가
	@Query("select r from RouteHeader r left join fetch r.product p "
		+ "where (:prdId is null or :prdId = '' or p.prdId = :prdId) "
		+ "and r.useYn = 'Y' "
		+ "and (:routeName is null or :routeName = '' or lower(r.routeName) like lower(concat('%', :routeName, '%')))"
	)
	List<RouteHeader> findByPrdIdAndRouteName(@Param("prdId") String prdId, @Param("routeName") String routeName);
}
