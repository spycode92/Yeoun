package com.yeoun.equipment.mapper;

import com.yeoun.equipment.dto.EquipDowntimeDTO;
import com.yeoun.equipment.dto.HistorySearchDTO;
import com.yeoun.process.dto.WorkOrderProcessDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EquipmentMapper {

    // 라인 및 공정별 작업진행현황
    List<WorkOrderProcessDTO> selectProcessByLineAndStep(
            @Param("line") String line,
            @Param("step") Integer step
    );
    
	
	// 설비 비가동 사유 정보 불러오기
	List<EquipDowntimeDTO> selectDowntimeHistories (HistorySearchDTO dto);

}
