package com.yeoun.masterData.service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import com.yeoun.masterData.entity.MaterialMst;
import com.yeoun.masterData.entity.ProductMst;
import com.yeoun.masterData.repository.MaterialMstRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional
public class MaterialMstService {
	private final MaterialMstRepository materialMstRepository;
	
	//1. 원재료 그리드 조회
	@Transactional(readOnly = true)
	public List<MaterialMst> findAll() {
		return materialMstRepository.findAll();
	}

	//2. 원재료 그리드 저장
	public String saveMaterialMst(String empId, Map<String, Object> param) {
		log.info("materialMstSaveList------------->{}",param);
		try {
			// createdRows
			Object createdObj = param.get("createdRows");
			if (createdObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<Map<String,Object>> created = (List<Map<String,Object>>) createdObj;
				for (Map<String,Object> row : created) {
					MaterialMst m = mapToMaterial(row);
					m.setCreatedId(empId);
					materialMstRepository.save(m);
				}
			}
			
			log.info("param.get(\"updatedRows\")------------------->{}",param.get("updatedRows"));
			// updatedRows
			Object updatedObj = param.get("updatedRows");
			if (updatedObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<Map<String,Object>> updated = (List<Map<String,Object>>) updatedObj;
				List<String> missingIds = new ArrayList<>();
				for (Map<String,Object> row : updated) {
					Object idObj = row.get("matId");
					String matId = (idObj == null) ? "" : String.valueOf(idObj).trim();
	
					MaterialMst target = null;
					if (!matId.isEmpty()) {
						Optional<MaterialMst> opt = materialMstRepository.findById(matId);
						if (opt.isPresent()) target = opt.get();
					}
	
					if (target != null) {
						// 기존 레코드 업데이트
						MaterialMst m = mapToMaterial(row);
						m.setCreatedId(row.get("createdId").toString());
						m.setCreatedDate(LocalDate.parse(row.get("createdDate").toString()));
						m.setUpdatedId(empId);
						m.setUpdatedDate(LocalDate.now());
						materialMstRepository.save(m);
					} else {
						// 존재하지 않는 prdId가 명시된 경우: PK 변경 시 의도치 않은 insert를 막기 위해 에러 처리
						if (!matId.isEmpty()) {
							missingIds.add(matId);
							continue;
						}
						// prdId가 비어있고 매칭되는 기존 레코드가 없으면 새로 저장 (신규 추가 케이스)
						MaterialMst m = mapToMaterial(row);
						m.setCreatedId(empId);
						materialMstRepository.save(m);
					}
				}
			 	
			}


			return "success";
		} catch (Exception e) {
			log.error("saveProductMst error", e);
			return "error: " + e.getMessage();
		}
	}

	// 유틸: Map 데이터를 ProductMst 엔티티로 변환
	private MaterialMst mapToMaterial(Map<String,Object> row) {
		MaterialMst m = new MaterialMst();
		if (row == null) return m;
		if (row.get("matId") != null) m.setMatId(String.valueOf(row.get("matId")));
		if (row.get("matName") != null) m.setMatName(String.valueOf(row.get("matName")));
		if (row.get("matType") != null) m.setMatType(String.valueOf(row.get("matType")));
		if (row.get("matUnit") != null) m.setMatUnit(String.valueOf(row.get("matUnit")));
		if (row.get("effectiveDate") != null) m.setEffectiveDate(String.valueOf(row.get("effectiveDate")));
		if (row.get("matDesc") != null) m.setMatDesc(String.valueOf(row.get("matDesc")));
		return m;
	}
	
	//3. 원재료 그리드 삭제
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public String deleteMaterialMst(String empId, List<String> param) {
		log.info("materialMstDeleteList------------->{}",param);
		try {
			for (String matId : param) {
				if (materialMstRepository.existsById(matId)) {
					materialMstRepository.deleteById(matId);
				}
			}
			return "success";
		} catch (Exception e) {
			log.error("deleteMaterialMst error", e);
			return "error: " + e.getMessage();
		}
	}
}