package com.yeoun.inventory.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yeoun.inventory.dto.InventoryHistoryDTO;
import com.yeoun.inventory.entity.InventoryHistory;

@Mapper
public interface InventoryHistoryMapper {

	List<InventoryHistoryDTO> searchInventoryHistory(
        @Param("startDateTime") LocalDateTime startDateTime, 
        @Param("endDateTime") LocalDateTime endDateTime, 
        @Param("workType") String workType,
        @Param("searchKeyword") String searchKeyword
	);

}
