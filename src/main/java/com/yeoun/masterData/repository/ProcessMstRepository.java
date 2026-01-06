package com.yeoun.masterData.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.yeoun.masterData.entity.ProcessMst;

public interface ProcessMstRepository extends JpaRepository<ProcessMst, String> {

	// 공정 기준정보 조회
	Optional<ProcessMst> findByProcessId(String processId);

	// 공정코드 드롭다운 조회
	@Query(value = """
			SELECT DISTINCT process_id
			FROM PROCESS_MST WHERE use_yn = 'Y'
			""", nativeQuery = true)
	List<String> processIdList();

	// 공정코드 그리드 조회
	@Query(value="""
			SELECT
				    p.*,
				    ec.emp_name AS created_by_name,
				    eu.emp_name AS updated_by_name
				FROM
				    PROCESS_MST p
				LEFT JOIN
				    EMP ec
				ON
				    p.created_id = ec.emp_id
				LEFT JOIN
				    EMP eu
				ON
				    p.updated_id = eu.emp_id
			WHERE ( ?1 IS NULL OR ?1 = '' OR PROCESS_ID LIKE '%' || ?1 || '%')
			  AND ( ?2 IS NULL OR ?2 = '' OR PROCESS_NAME LIKE '%' || ?2 || '%')
			ORDER BY USE_YN DESC,STEP_NO ASC
			""", nativeQuery = true)
	List<Map<String, Object>> findByprocesslList(String processId, String processName);
	
}
