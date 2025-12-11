package com.yeoun.masterData.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.yeoun.outbound.dto.OutboundOrderItemDTO;

@Mapper
public interface BomMstMapper {
	// prdId에 해당하는 BOM 리스트 조회
	List<OutboundOrderItemDTO> findByPrdIdList(String prdId);

}
