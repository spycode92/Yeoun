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
	
	// 완제품 LOT(FIN) 목록 조회 (LOT_TYPE = 'FIN' 인 LOT만 대상)
	// - LOT 추적에서 왼쪽 목록에 출력될 ROOT
	List<LotMaster> findByLotTypeOrderByCreatedDateDesc(String lotType);

}
