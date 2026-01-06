package com.yeoun.inventory.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yeoun.inventory.dto.InventoryHistoryGroupDTO;
import com.yeoun.inventory.entity.Inventory;
import com.yeoun.inventory.entity.InventoryHistory;

public interface InventoryHistoryRepository	extends JpaRepository<InventoryHistory, Long> {
	
	// 
    @Query(value = """
        SELECT
            TRUNC(h.created_date)        AS createdDate,
            h.work_type                  AS workType,
            SUM(h.current_amount)        AS sumCurrent,
            SUM(h.prev_amount)           AS sumPrev
        FROM INVENTORY_HISTORY h
        WHERE h.created_date BETWEEN :oneYearAgo AND :now
    		AND h.work_type IN('INBOUND', 'OUTBOUND', 'DISPOSE')
        GROUP BY TRUNC(h.created_date), h.work_type
        ORDER BY TRUNC(h.created_date)
        """,
        nativeQuery = true)
    List<Object[]> getIvHistoryGroupData(
        @Param("now") LocalDateTime now,
        @Param("oneYearAgo") LocalDateTime oneYearAgo
    );

}
