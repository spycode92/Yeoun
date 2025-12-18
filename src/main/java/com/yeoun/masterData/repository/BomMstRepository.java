package com.yeoun.masterData.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.yeoun.masterData.entity.BomMst;
import com.yeoun.masterData.entity.BomMstId;
import com.yeoun.masterData.entity.MaterialMst;
import com.yeoun.masterData.entity.ProductMst;

@Repository
public interface BomMstRepository extends JpaRepository<BomMst, BomMstId>{
  
  	// 특정 품목의 bom 찾기
	List<BomMst> findByPrdId(String prdId);

	// prdId + matId 쌍으로 bom 찾기
	Optional<BomMst> findByPrdIdAndMatId(String prdId, String matId);
	
	// bom 완제품id 조회
	@Query(value = """
			SELECT PRD_ID AS value
					,PRD_NAME AS text
			FROM PRODUCT_MST
			WHERE USE_YN='Y'
				""", nativeQuery = true)
	List<Map<String, Object>> findBomPrdList();
	
	// bom 원재료id 조회
	@Query(value="""
			SELECT *
			FROM material_mst
			WHERE USE_YN = 'Y'
			""",nativeQuery= true)
	List<MaterialMst> findBomMatList();
	
	// bom 단위 드롭다운 조회
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
	List<Map<String, Object>> findByBomUnitList();

	// BOM 정보 그리드 조회
	@Query(value="""
				SELECT
				    b.*,
				    ec.emp_name AS created_by_name,
				    eu.emp_name AS updated_by_name
				FROM
				    bom_mst b
				LEFT JOIN
				    EMP ec
				ON
				    b.created_id = ec.emp_id
				LEFT JOIN
				    EMP eu
				ON
				    b.updated_id = eu.emp_id
				WHERE (:bomId IS NULL OR :bomId = '' OR b.BOM_ID LIKE '%' || :bomId || '%')
				AND (:matId IS NULL OR :matId = '' OR b.MAT_ID LIKE '%' || :matId || '%')
				ORDER BY use_yn desc,bom_id asc, bom_seq_no asc
				""", nativeQuery = true)
	List<Map<String, Object>> findBybomList(@Param("bomId") String bomId, @Param("matId") String matId);

	// BOM 상세 정보 조회
	@Query(value="""
				SELECT BOM_ID, USE_YN
				FROM (
				    SELECT 
				        BOM_ID, 
				        USE_YN,
				        -- N을 1순위, Y를 2순위로 번호를 매김
				        ROW_NUMBER() OVER(PARTITION BY BOM_ID ORDER BY USE_YN ASC) AS RN
				    FROM BOM_MST
				)
				WHERE RN = 1
				ORDER BY USE_YN DESC, BOM_ID ASC
				"""
			,nativeQuery = true
			)
	List<Object[]> findAllDetail();

	// BOM 상세 정보 조회 - 완제품
	@Query(value="""
				SELECT
					DISTINCT
					b.bom_id,
					b.prd_id,
					p.prd_name,
					p.prd_cat,
					p.item_name,
					p.prd_unit
				FROM
					bom_mst b
				LEFT OUTER JOIN
					product_mst p ON b.prd_id = p.prd_id
				WHERE bom_id=:bomId
				"""
			,nativeQuery = true)
	List<Object[]> findAllDetailPrd(@Param("bomId") String bomId);

	// BOM 상세 정보 조회 - 원재료
	@Query(value="""
				SELECT
					b.bom_id,
					b.prd_id,
					b.mat_id,
					m.mat_name,
					m.mat_type,
					m.mat_desc,
					b.mat_qty,
					b.mat_unit,
					b.bom_seq_no
				FROM
					bom_mst b
				LEFT OUTER JOIN
					product_mst p ON b.prd_id = p.prd_id
				LEFT OUTER JOIN
					material_mst m ON b.mat_id = m.mat_id
				WHERE b.bom_id=:bomId AND (m.mat_type = 'RAW')
				order by b.bom_seq_no asc
				"""
			,nativeQuery = true)
	List<Object[]> findAllDetailMat(@Param("bomId") String bomId);

	// BOM 상세 정보 조회 - 포장재
	@Query(value="""
				SELECT
					b.bom_id,
					b.prd_id,
					b.mat_id,
					m.mat_name,
					m.mat_type,
					m.mat_desc,
					b.mat_qty,
					b.mat_unit,
					b.bom_seq_no
				FROM
					bom_mst b
				LEFT OUTER JOIN
					product_mst p ON b.prd_id = p.prd_id
				LEFT OUTER JOIN
					material_mst m ON b.mat_id = m.mat_id
				WHERE bom_id=:bomId AND (m.mat_type IS NULL OR m.mat_type <> 'RAW')
				order by b.bom_seq_no asc
				"""
			,nativeQuery = true)
	List<Object[]> findAllDetailMatType(@Param("bomId") String bomId);

}
