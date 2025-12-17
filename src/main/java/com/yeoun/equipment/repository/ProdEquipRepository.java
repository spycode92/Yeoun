package com.yeoun.equipment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeoun.equipment.entity.ProdEquip;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProdEquipRepository extends JpaRepository<ProdEquip, Long> {

    @Query("""
        SELECT e
        FROM ProdEquip e
        WHERE (:equipment IS NULL OR e.equipment.equipId = :equipment)
          AND (:line IS NULL OR e.line.lineId = :line)
          AND (:status IS NULL OR e.status = :status)
        ORDER BY e.createdDate DESC
    """)
    List<ProdEquip> loadProdEquip(
            @Param("equipment") String equipment,
            @Param("line") String line,
            @Param("status") String status
    );
    
    // PROD_EQUIP 기준으로 라인 + 설비코드들 조회
    @Query("""
    	    select pe
    	    from ProdEquip pe
    	    join fetch pe.equipment e
    	    where pe.line.lineId = :lineId
    	      and e.equipId in :equipCodes
    	""")
    	List<ProdEquip> findForLineAndCodes(@Param("lineId") String lineId,
    										@Param("equipCodes") List<String> equipCodes);
    
    
    // 공정 상세(LOT)에서 사용: 라인 + 공정ID로 설비 조회 (EQUIPMENT의 processId 기준)
    @Query("""
	    select pe
	    from ProdEquip pe
	    join fetch pe.equipment e
	    where pe.line.lineId = :lineId
	      and e.process.processId = :processId
	      and e.useYn = 'Y'
	""")
    List<ProdEquip> findForLineAndProcess(@Param("lineId") String lineId,
                                          @Param("processId") String processId);


}
