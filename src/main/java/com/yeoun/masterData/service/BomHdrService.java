package com.yeoun.masterData.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
	//Bom 그룹 저장findBomHdrSave
	public String saveBomHdr(String empId, Map<String, Object> param) {
	    Object updatedObj = param.get("updatedRows");
	    
	    if (updatedObj instanceof List) {
	        @SuppressWarnings("unchecked")
	        List<Map<String, Object>> updated = (List<Map<String, Object>>) updatedObj;
	        
	        for (Map<String, Object> row : updated) {
	            // Null을 허용하지 않는 필드들에 대해 안전한 문자열 변환 함수 정의
	            // null인 경우 빈 문자열("") 혹은 기본값을 반환하도록 처리
	            
	            BomHdr b = BomHdr.builder()
	                    .bomHdrId(getString(row.get("bomHdrId")))
	                    .bomId(getString(row.get("bomId")))
	                    .bomHdrName(getString(row.get("bomHdrName")))
	                    .bomHdrType(getString(row.get("bomHdrType")))
	                    .useYn(getString(row.get("useYn")))
	                    .createdId(getString(row.get("createdId")))
	                    // 날짜 처리: null인 경우 현재 시간 혹은 기존 시간 유지 로직 필요
	                    .createdDate(parseDate(row.get("createdDate"))) 
	                    .updatedId(empId)
	                    .updatedDate(LocalDateTime.now())
	                    .build(); // 빌더 패턴 끝에 세미콜론 확인!
	            
	            bomHdrRepository.save(b);
	        }
	    }
	    return "Success: BOM 그룹수정이 완료되었습니다.";
	}

	// 안전한 문자열 변환 보조 메서드
	private String getString(Object obj) {
	    return (obj == null) ? "" : String.valueOf(obj);
	}

	// 안전한 날짜 파싱 보조 메서드
	private LocalDateTime parseDate(Object obj) {
	    if (obj == null || obj.toString().isEmpty()) {
	        return LocalDateTime.now(); // 데이터가 없으면 현재 시간으로 세팅
	    }
	    try {
	        // 만약 형식이 다르면 DateTimeFormatter를 사용해야 합니다.
	        return LocalDateTime.parse(obj.toString());
	    } catch (Exception e) {
	        return LocalDateTime.now();
	    }
	}
	
	

}
