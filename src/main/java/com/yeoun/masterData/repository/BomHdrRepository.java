package com.yeoun.masterData.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.yeoun.masterData.entity.BomHdr;
import com.yeoun.masterData.entity.BomMst;
import com.yeoun.masterData.entity.BomMstId;
import com.yeoun.masterData.entity.MaterialMst;
import com.yeoun.masterData.entity.ProductMst;

@Repository
public interface BomHdrRepository extends JpaRepository<BomHdr, String>{
  

	List<BomHdr> findAllByOrderByUseYnDescBomHdrIdAsc();
	
	//BOM 그룹 findBomHdrType 드롭다운
	@Query(value = """
		   SELECT 
			    CODE_ID    AS value,
			    CODE_NAME  AS text
			FROM COMMON_CODE
			WHERE PARENT_CODE_ID = 'BOM_HDR_TYPE'
			  AND USE_YN = 'Y'
			ORDER BY CODE_SEQ ASC
		""",nativeQuery = true)
	List<Map<String,Object>> findBomHdrTypeList();
	
	//BOM 그룹 그리드 조회
	@Query(value = """
		    SELECT DISTINCT b 
		    FROM BomHdr b 
		    LEFT JOIN FETCH b.bomMstList 
		    WHERE (:bomHdrId IS NULL OR b.bomHdrId LIKE %:bomHdrId%)
		      AND (:bomHdrType IS NULL OR b.bomHdrType LIKE %:bomHdrType%)
		    ORDER BY b.useYn DESC, b.bomHdrId ASC
		""",nativeQuery = false)
	List<BomHdr> searchBomHeaders(@Param("bomHdrId") String bomHdrId, @Param("bomHdrType") String bomHdrType);
	
}
