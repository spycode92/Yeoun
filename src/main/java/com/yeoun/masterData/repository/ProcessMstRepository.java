package com.yeoun.masterData.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yeoun.masterData.entity.ProcessMst;
import com.yeoun.masterData.entity.ProductMst;
import com.yeoun.masterData.entity.RouteHeader;

public interface ProcessMstRepository extends JpaRepository<ProcessMst, String> {

	// 공정 기준정보 조회
	Optional<ProcessMst> findByProcessId(String processId);

	// 공정코드 그리드 조회
	@Query(value="""
			select * from process_mst
				where use_yn = 'Y'
			""",nativeQuery = true)
	List<ProcessMst> findByprocessCode();
	
}
