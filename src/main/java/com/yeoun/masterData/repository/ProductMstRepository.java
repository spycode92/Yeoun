package com.yeoun.masterData.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
  
import com.yeoun.masterData.entity.ProductMst;

@Repository
public interface ProductMstRepository extends JpaRepository<ProductMst, String> {

	//1. 완제품 조회
	//Optional<ProductMst> findByProductAll();
	//2. 완제품 수정
	//3. 완제품 삭제

	Optional<ProductMst> findByItemNameAndPrdName(String itemName, String prdName);

	// 제품ID로 조회
	Optional<ProductMst> findByPrdId(String prdId);
	
	//완제품 품목명(향수타입) 드롭다운
	@Query(value = """
			SELECT
				CODE_ID AS value,       -- DB에 저장할 실제 데이터 값 ('LIQUID', 'SOLID')
				CODE_NAME AS text       -- 사용자에게 보여줄 이름 ('고체향수', '액체향수')
			FROM
				COMMON_CODE
			WHERE
				PARENT_CODE_ID = 'PERFUME_TYPE'  -- '향수 타입 구분' 그룹에 속한 하위 코드만 선택
				AND USE_YN = 'Y'                 -- 사용 중인 코드만 선택
			ORDER BY
				CODE_SEQ
			""", nativeQuery = true)
	List<Map<String, Object>> findByPrdItemNameList();

	//완제품 제품유형 드롭다운
	@Query(value = """
			SELECT
				CODE_ID AS value,       -- DB에 저장할 실제 데이터 값 ('FINISHED_GOODS', 'SEMI_FINISHED_GOODS')
				CODE_NAME AS text       -- 사용자에게 보여줄 이름 ('완제품', '반제품')
			FROM
				COMMON_CODE
			WHERE
				PARENT_CODE_ID = 'PRODUCT_TYPE'  -- '제품 유형 구분' 그룹에 속한 하위 코드만 선택
				AND USE_YN = 'Y'                 -- 사용 중인 코드만 선택
			ORDER BY
				CODE_SEQ
			""", nativeQuery = true)
	List<Map<String, Object>> findByPrdTypeList();

	//완제품 단위 드롭다운
	@Query(value = """
			SELECT
				CODE_ID AS value,       -- DB에 저장할 실제 데이터 값 ('g', 'ml', 'EA')
				CODE_NAME AS text       -- 사용자에게 보여줄 이름 ('g', 'ml', 'EA')
			FROM
				COMMON_CODE
			WHERE
				PARENT_CODE_ID = 'UNIT_TYPE'  -- '단위 구분' 그룹에 속한 하위 코드만 선택
				AND USE_YN = 'Y'              -- 사용 중인 코드만 선택
			ORDER BY
				CODE_SEQ                     -- 미리 정한 순서대로 정렬
			""", nativeQuery = true)
	List<Map<String, Object>> findByPrdUnitList();

	//완제품 제품상태 드롭다운
	@Query(value = """
			SELECT
				CODE_ID AS value,       -- DB에 저장할 실제 데이터 값 ('ACTIVE', 'INACTIVE' 등)
				CODE_NAME AS text       -- 사용자에게 보여줄 이름 ('활성', '비활성' 등)
			FROM
				COMMON_CODE
			WHERE
				PARENT_CODE_ID = 'PRD_STATUS'  -- '제품 상태 구분' 그룹에 속한 하위 코드만 선택
				AND USE_YN = 'Y'               -- 사용 중인 코드만 선택
			ORDER BY
				CODE_SEQ
			""", nativeQuery = true)
	List<Map<String, Object>> findByPrdStatusList();

	//완제품 조회(제품ID, 제품명)
	@Query(value = """
				SELECT
				    p.*,
				    ec.emp_name AS created_by_name,
				    eu.emp_name AS updated_by_name
				FROM
				    PRODUCT_MST p
				LEFT JOIN
				    EMP ec
				ON
				    p.created_id = ec.emp_id
				LEFT JOIN
				    EMP eu
				ON
				    p.updated_id = eu.emp_id
				WHERE (:prdId IS NULL OR :prdId = '' OR p.PRD_ID LIKE '%' || :prdId || '%')
			AND (:prdName IS NULL OR :prdName = '' OR p.PRD_NAME LIKE '%' || :prdName || '%')
			ORDER BY
			    p.USE_YN DESC,p.prd_id ASC
			""", nativeQuery = true)
	List<Map<String, Object>> findByPrdIdList(@Param("prdId") String prdId, @Param("prdName") String prdName);

}
