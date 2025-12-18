package com.yeoun.masterData.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeoun.masterData.entity.BomHdr;
import com.yeoun.masterData.entity.BomMst;
import com.yeoun.masterData.entity.MaterialMst;
import com.yeoun.masterData.mapper.BomMstMapper;
import com.yeoun.masterData.repository.BomHdrRepository;
import com.yeoun.masterData.repository.BomMstRepository;
import com.yeoun.outbound.dto.OutboundOrderItemDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional
public class BomHdrService {
	private final BomHdrRepository bomHdrRepository;
	
	//findBomHdrTypeList
	@Transactional(readOnly = true)
	public List<Map<String,Object>> findBomHdrTypeList() {
		return bomHdrRepository.findBomHdrTypeList();
	}
	
	
	//BOM 그룸 전체 조회
	@Transactional(readOnly = true)
	public List<BomHdr> findBomHdrList(String bomHdrId, String bomHdrType ) {
		return bomHdrRepository.searchBomHeaders(bomHdrId,bomHdrType);
	}
	

}
