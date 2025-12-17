package com.yeoun.masterData.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeoun.masterData.entity.RouteHeader;
import com.yeoun.masterData.entity.RouteStep;

public interface RouteStepRepository extends JpaRepository<RouteStep, String> {
	
	// 해당 라우트의 공정단계를 순서대로
	List<RouteStep> findByRouteHeaderOrderByStepSeqAsc(RouteHeader routeHeader);
	List<RouteStep> findByRouteHeader_RouteIdOrderByStepSeqAsc(String routeId);
	
	Optional<RouteStep> findById(String routeStepId);

}
