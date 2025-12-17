package com.yeoun.masterData.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yeoun.masterData.entity.MaterialMst;

public interface MaterialMstRepository extends JpaRepository<MaterialMst, String> {

	// 원자재 조회
	Optional<MaterialMst> findByMatId(String materialOrder);

	List<MaterialMst> findByMatType(String matType);
	
	List<MaterialMst> findByMatTypeAndUseYn(String matType, String useYn);

	// 원자재 품목명(원자재유형) 드롭다운
	@Query(value = """
			SELECT
				CODE_ID AS value,       -- DB에 저장할 실제 데이터 값 ('RAW', 'SUB' 등)
				CODE_NAME AS text       -- 사용자에게 보여줄 이름 ('원재료', '부자재' 등)
			FROM
				COMMON_CODE
			WHERE
				PARENT_CODE_ID = 'MAT_TYPE'  -- '원재료 유형 구분' 그룹에 속한 하위 코드만 선택
				AND USE_YN = 'Y'             -- 사용 중인 코드만 선택
			ORDER BY
				CODE_SEQ 
			""", nativeQuery = true)
	List<Map<String, Object>> findByMatTypeList();

	// 원자재 단위 드롭다운
	@Query(value = """
		SELECT
			CODE_ID AS value,       -- DB에 저장할 실제 코드 값 ('g', 'ml', 'EA', 'UNIT_BOX')
			CODE_NAME AS text       -- 사용자에게 보여줄 이름 ('g', 'ml', 'EA', '박스')
		FROM
			COMMON_CODE
		WHERE
			PARENT_CODE_ID = 'UNIT_TYPE'  -- '단위 구분' 그룹에 속한 하위 코드만 선택
			AND USE_YN = 'Y'              -- 사용 여부가 'Y'인 활성 코드만 선택
		ORDER BY
			CODE_SEQ
			""", nativeQuery = true)
	List<Map<String, Object>> findByMatUnitList();
	

	// 원자재 조회(검색)
	@Query(value = """
			SELECT
				    m.*,
				    ec.emp_name AS created_by_name,
				    eu.emp_name AS updated_by_name
				FROM
				    MATERIAL_MST m
				LEFT JOIN
				    EMP ec
				ON
				    m.created_id = ec.emp_id
				LEFT JOIN
				    EMP eu
				ON
				    m.updated_id = eu.emp_id
			WHERE (:matId IS NULL OR :matId = '' OR m.MAT_ID LIKE '%' || :matId || '%')
			AND (:matName IS NULL OR :matName = '' OR m.MAT_NAME LIKE '%' || :matName || '%')
			ORDER BY
			    m.USE_YN DESC, m.mat_id ASC
			""", nativeQuery = true)
	List<Map<String, Object>> findByMatIdList(@Param("matId") String matId, @Param("matName") String matName);

}
