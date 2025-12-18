package com.yeoun.lot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.yeoun.lot.entity.LotMaster;

@Repository
public interface LotMasterRepository extends JpaRepository<LotMaster, String> {

	// 최근 시퀀스 조회
	@Query(value = """
		    SELECT 
		        REGEXP_SUBSTR(lot_no, '[0-9]{3}$') AS seq
		    FROM LOT_MASTER
		    WHERE 
		        LOT_TYPE = :lotType
		    AND PRD_ID = :prdId
		    AND SUBSTR(lot_no, INSTR(lot_no, '-', 1, 3) + 1, 8) = :dateStr
		    AND REGEXP_SUBSTR(lot_no, '-([0-9]{2})-', 1, 1, NULL, 1) = :line
		    ORDER BY seq DESC
		    FETCH FIRST 1 ROWS ONLY
		""", nativeQuery = true)
	String findLastSeq(@Param("lotType") String lotType, 
			@Param("prdId") String prdId, 
			@Param("dateStr")String dateStr, 
			@Param("line") String line);

    // LOT 조회
	Optional<LotMaster> findByLotNo(String lotNo);
	
	// 완제품 LOT 목록 조회 (LOT_TYPE = 'WIP', 'FIN' 인 LOT만 대상)
	// - LOT 추적에서 왼쪽 목록에 출력될 ROOT
	
	@Query(value = """
		    SELECT
		      lm.lot_no AS lotNo,
		      COALESCE(p.prd_name, m.mat_name, lm.prd_id) AS displayName,
		      lm.current_status AS currentStatus,
		      lm.lot_type AS lotType
		    FROM lot_master lm
		    LEFT JOIN product_mst p ON p.prd_id = lm.prd_id
		    LEFT JOIN material_mst m ON m.mat_id = lm.prd_id
		    WHERE lm.lot_type IN ('WIP', 'FIN')
		    ORDER BY
		      CASE lm.current_status
		        WHEN 'IN_PROCESS' THEN 0
		        WHEN 'PROD_DONE' THEN 1
		        WHEN 'SCRAPPED' THEN 2
		        ELSE 3
		      END,
		      lm.created_date DESC
		    """, nativeQuery = true)
	List<Object[]> findFinishedLotsNative();
	
	@Query(value = """
		    SELECT
		      lm.lot_no AS lotNo,
		      COALESCE(p.prd_name, m.mat_name, lm.prd_id) AS displayName,
		      COALESCE(
		        (SELECT i.iv_status
		         FROM inventory i
		         WHERE i.iv_id = (
		           SELECT MAX(i2.iv_id)
		           FROM inventory i2
		           WHERE i2.lot_no = lm.lot_no
		         )
		        ),
		        'SOLD_OUT'
		      ) AS invStatus
		    FROM lot_master lm
		    LEFT JOIN product_mst p ON p.prd_id = lm.prd_id
		    LEFT JOIN material_mst m ON m.mat_id = lm.prd_id
		    WHERE lm.lot_type IN ('RAW','SUB','PKG')
		    ORDER BY lm.created_date DESC
		    """, nativeQuery = true)
	List<MaterialRootRow> findMaterialRootLots();


}
