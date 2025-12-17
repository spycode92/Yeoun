package com.yeoun.masterData.repository;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.yeoun.masterData.entity.QcItem;

@Repository
public interface QcItemRepository extends JpaRepository<QcItem, String> {

    //품질 항목 기준 qcId 목록 조회
    @Query(value = """
        SELECT DISTINCT QC_ITEM_ID 
        FROM QC_ITEM ORDER BY QC_ITEM_ID ASC
        """, nativeQuery = true)
    List<String> qcIdList();
    //품질항목조회 (네이티브 SQL 사용)
    @Query(value = """
     
		SELECT
			    q.*,
			    ec.emp_name AS created_by_name,
			    eu.emp_name AS updated_by_name
			FROM
			    QC_ITEM q
			LEFT JOIN
			    EMP ec
			ON
			    q.created_id = ec.emp_id
			LEFT JOIN
			    EMP eu
			ON
			    q.updated_id = eu.emp_id
        WHERE (?1 IS NULL OR ?1 = '' OR QC_ITEM_ID LIKE '%' || ?1 || '%')
        AND (USE_YN = 'Y')
        ORDER BY SORT_ORDER ASC, QC_ITEM_ID ASC
        """, nativeQuery = true)
    List<Map<String, Object>> findByQcItemList(String qcItemId);
	// 대상구분 + 사용여부 기준으로 항목 조회
    List<QcItem> findByTargetTypeAndUseYnOrderBySortOrderAsc(String targetType, String useYn);
    
}
