package com.yeoun.masterData.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.yeoun.masterData.entity.RouteHeader;
import com.yeoun.masterData.entity.RouteStep;

public interface RouteStepRepository extends JpaRepository<RouteStep, String> {
	
	// 해당 라우트의 공정단계를 순서대로
	List<RouteStep> findByRouteHeaderOrderByStepSeqAsc(RouteHeader routeHeader);
	
	Optional<RouteStep> findById(String routeStepId);

	// 라우트 공정단계 조회
	@Query(value ="""
			SELECT rs.*
				,ec.emp_name AS created_by_name
				,eu.emp_name AS updated_by_name
			FROM ROUTE_STEP rs
			LEFT JOIN emp ec
			ON rs.created_id = ec.emp_id
			LEFT JOIN emp eu
			ON rs.updated_id = eu.emp_id
			WHERE ROUTE_ID = :routeId
			ORDER BY STEP_SEQ ASC
			""", nativeQuery = true)
	List<Map<String, Object>> findRouteStepByRouteId(@Param("routeId") String routeId);


}
