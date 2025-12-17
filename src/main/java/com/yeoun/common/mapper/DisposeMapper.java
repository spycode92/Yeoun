package com.yeoun.common.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yeoun.common.dto.DisposeListDTO;
import com.yeoun.inbound.dto.ReceiptDTO;

@Mapper
public interface DisposeMapper {

	List<DisposeListDTO> searchDisposeList(
        @Param("startDateTime") LocalDateTime startDateTime, 
        @Param("endDateTime") LocalDateTime endDateTime, 
        @Param("workType") String workType,
        @Param("searchKeyword") String searchKeyword
    );


}
