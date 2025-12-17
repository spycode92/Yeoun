package com.yeoun.inventory.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yeoun.inventory.dto.InventorySafetyCheckDTO;
import com.yeoun.inventory.entity.Inventory;
import com.yeoun.inventory.entity.WarehouseLocation;

public interface InventoryRepository
	extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {

	Optional<Inventory> findByWarehouseLocationAndLotNo(WarehouseLocation location, String lotNo);
	
	// 생산계획시 필요한 제품(PRD_ID / ITEM_ID) 기준 전체 재고 조회
		@Query(value = """
		   SELECT
		    ITEM_ID AS prdId,
		    SUM(NVL(IV_AMOUNT, 0) - NVL(EXPECT_OB_AMOUNT, 0)) AS currentStock
		FROM INVENTORY
		GROUP BY ITEM_ID

		""", nativeQuery = true)
		List<Map<String, Object>> findCurrentStockGrouped();		


	List<Inventory> findByWarehouseLocation(WarehouseLocation location);


	// id로 재고 수량 조회
	@Query("""
		    SELECT COALESCE(SUM(i.ivAmount), 0)
		    FROM Inventory i
		    WHERE i.itemId = :id
		      AND i.ivStatus <> 'EXPIRED'
		""")
	Integer findAvailableStock(@Param("id") String id);

	// 현재재고수량, 안전재고수량 통계
	@Query(value = """
        SELECT
            ss.ITEM_ID                       AS itemId,
            ss.ITEM_NAME                     AS itemName,
            ss.ITEM_TYPE                     AS itemType,
            NVL(inv.IN_QTY, 0)               AS ivQty,
            NVL(inv.PLAN_OUT_QTY, 0)         AS planOutQty,
            NVL(inv.IN_QTY, 0)
              - NVL(inv.PLAN_OUT_QTY, 0)     AS expectIvQty,
            NVL(inv.LOCATIONS_CNT, 0)        AS locationsCnt,
            ss.SAFETY_STOCK_QTY              AS safetyStockQty,
            ss.SAFETY_STOCK_QTY_DAILY        AS safetyStockQtyDaily,
            ibit.EXPECT_IBAMOUNT             As expectIbAmount
        FROM SAFETY_STOCK ss
        LEFT JOIN (
            SELECT
                ITEM_ID,
                SUM(CASE 
                        WHEN IV_STATUS != 'EXPIRED' THEN IV_AMOUNT 
                        ELSE 0 
                    END) AS IN_QTY,
                SUM(CASE 
                        WHEN IV_STATUS != 'EXPIRED' THEN EXPECT_OB_AMOUNT 
                        ELSE 0 
                    END) AS PLAN_OUT_QTY,
                COUNT(DISTINCT LOCATION_ID) AS LOCATIONS_CNT
            FROM INVENTORY
            WHERE IV_STATUS != 'EXPIRED'
            GROUP BY ITEM_ID
        ) inv
            ON inv.ITEM_ID = ss.ITEM_ID
        LEFT JOIN(
			SELECT
				it.ITEM_ID,
				SUM(it.REQUEST_AMOUNT) as EXPECT_IBAMOUNT
			FROM
				INBOUND i
			LEFT JOIN
				INBOUND_ITEM it
				ON i.INBOUND_ID = it.INBOUND_ID
			WHERE
				INBOUND_STATUS = 'PENDING_ARRIVAL'
			GROUP BY ITEM_ID
		) ibit
			ON ss.ITEM_ID = ibit.ITEM_ID
        ORDER BY ss.ITEM_ID
        """,
        nativeQuery = true)
	List<InventorySafetyCheckDTO> getIvSummaryWithSafetyStock();
	
	
	@Modifying
	@Query("""
	    UPDATE Inventory i 
	    SET i.ivStatus = CASE
		    WHEN i.expirationDate IS NULL THEN i.ivStatus
	        WHEN i.expirationDate < :today THEN 'EXPIRED'
	        WHEN i.expirationDate BETWEEN :today AND :disposalWaitDate THEN 'DISPOSAL_WAIT'
	        ELSE 'NORMAL'
	    END
	    WHERE i.expirationDate IS NOT NULL
			AND i.ivStatus IN ('NORMAL', 'DISPOSAL_WAIT')
	""")
	void updateAllStatusByExpirationDate(@Param("today") LocalDateTime today, 
		    @Param("disposalWaitDate") LocalDateTime disposalWaitDate);

	// itemId로 재고 조회(선입선출)
	@Query("""
		    SELECT i
		    FROM Inventory i
		    WHERE i.itemId = :itemId
		      AND i.ivStatus <> :status
		    ORDER BY
			    i.expirationDate ASC,
			    i.ibDate ASC,
			    i.ivAmount DESC
		""")
	List<Inventory> findByItemIdAndIvStatusNot(@Param("itemId") String itemId, @Param("status") String status);

	// 재고삭제
	void delete(Inventory stock);

	// 재고 조회
	Optional<Inventory> findByIvId(Long ivId);

	
	//생산계획 작성시 필요한 재고 조회 쿼리
	@Query(value = """
		    SELECT 
		        COALESCE(SUM(i.IV_AMOUNT), 0) AS ivAmount,
		        COALESCE(SUM(i.EXPECT_OB_AMOUNT), 0) AS expectOut
		    FROM INVENTORY i
		    WHERE i.ITEM_ID = :matId
		    """,
		    nativeQuery = true)
		Map<String, Object> findMaterialStock(@Param("matId") String matId);

	Optional<Inventory> findTopByLotNoOrderByIvIdDesc(String inputLotNo);

	// lot번호와 창고위치로 재고 조회
	Optional<Inventory> findByLotNoAndWarehouseLocation_LocationId(String lotNo, String locationId);

	// 창고 위치 ID로 재고 존재 확인
	boolean existsByWarehouseLocation_LocationId(String locationId);


}
