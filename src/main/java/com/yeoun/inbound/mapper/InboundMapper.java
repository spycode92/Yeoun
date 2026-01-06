package com.yeoun.inbound.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yeoun.inbound.dto.ReceiptDTO;

@Mapper
public interface InboundMapper {

	// 원재료 목록 데이터(날짜 지정과 검색 기능 포함)
	List<ReceiptDTO> findAllMaterialInbound(@Param("startDate") LocalDateTime startDate, 
			@Param("endDate") LocalDateTime endDate, @Param("searchType") String searchType, 
			@Param("keyword") String keyword);

	// 입고 상세 조회
	ReceiptDTO findInbound(String inboundId);
}
