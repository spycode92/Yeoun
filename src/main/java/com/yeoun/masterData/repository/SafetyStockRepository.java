package com.yeoun.masterData.repository;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.method.P;

import com.yeoun.masterData.entity.SafetyStock;

public interface SafetyStockRepository extends JpaRepository<SafetyStock, String> {
  
    //안전재고 품목종류 드롭다운
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
   
    //안전재고 단위 드롭다운
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

    //안전재고 정책방식 드롭다운
    @Query(value = """
      SELECT
        CODE_ID AS value,       -- DB에 저장할 실제 코드 값 ('FIXED', 'DYNAMIC')
        CODE_NAME AS text       -- 사용자에게 보여줄 이름 ('고정식', '동적식')
      FROM
        COMMON_CODE
      WHERE
        PARENT_CODE_ID = 'SAFETY_STOCK_POLICY'  -- '안전재고 정책방식' 그룹에 속한 하위 코드만 선택
        AND USE_YN = 'Y'              -- 사용 여부가 'Y'인 활성 코드만 선택
      ORDER BY
        CODE_SEQ
        """, nativeQuery = true)
    List<Map<String, Object>> findBySafetyStockPolicyList();

    //안전재고 상태 드롭다운
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

    //안전재고 그리드 조회
    @Query(value = """
        SELECT s
        FROM SafetyStock s
        WHERE (:itemId IS NULL OR :itemId = '' OR s.itemId LIKE %:itemId%)
          AND (:itemName IS NULL OR :itemName = '' OR s.itemName LIKE %:itemName%)
        ORDER BY s.itemId ASC
        """, nativeQuery = false)
    List<SafetyStock> findByItemlList(@Param("itemId") String itemId, @Param("itemName")  String itemName);
}
