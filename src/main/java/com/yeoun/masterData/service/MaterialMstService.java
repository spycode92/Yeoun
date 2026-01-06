package com.yeoun.masterData.service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import com.yeoun.masterData.entity.MaterialMst;
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
	// 전체조회와 특정 matId 조회(부분검색)를 모두 처리
	@Transactional(readOnly = true)
	public List<Map<String, Object>> findByMatIdList(String matId, String matName) {
		// repository 쿼리에서 null/빈값은 전체조회로 처리하도록 되어 있음
		return materialMstRepository.findByMatIdList(matId, matName);
	}
	
	//원재료유형 드롭다운
	@Transactional(readOnly = true)
	public List<Map<String, Object>> findByMatTypeList() {
		// repository 쿼리에서 null/빈값은 전체조회로 처리하도록 되어 있음
		return materialMstRepository.findByMatTypeList();
	}
	
	
	//원재료 단위 드롭다운
	@Transactional(readOnly = true)
	public List<Map<String, Object>> findByMatUnitList() {
		// repository 쿼리에서 null/빈값은 전체조회로 처리하도록 되어 있음
		return materialMstRepository.findByMatUnitList();
	}

	//2. 원재료 그리드 저장
	@Transactional
	public String saveMaterialMst(String empId, Map<String, Object> param) {
		log.info("materialMstSaveList------------->{}",param);
		try {
			// createdRows
			Object createdObj = param.get("createdRows");
			if (createdObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<Map<String,Object>> created = (List<Map<String,Object>>) createdObj;
				for (Map<String,Object> row : created) {
					Object idObj = row.get("matId");
					String matId = (idObj == null) ? "" : String.valueOf(idObj).trim();

					if (!matId.isEmpty()) {
						// 예외를 던지지 않고 중복을 감지하여 호출자에게 에러 문자열로 알립니다.
						materialMstRepository.findById(matId)
							    .ifPresent(existingProduct -> {
							        throw new IllegalStateException("중복되는 원재료id가 이미 존재합니다.");
							    });//중복중
					}
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
						Object createdIdObj = row.get("createdId");
						if (createdIdObj != null) m.setCreatedId(createdIdObj.toString());
						Object createdDateObj = row.get("createdDate");
						if (createdDateObj != null) {
							try { m.setCreatedDate(LocalDate.parse(createdDateObj.toString())); } catch(Exception ignore) {}
						}
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
		if (row.get("effectiveDate") != null) {
			Object eff = row.get("effectiveDate");
			try {
				if (eff instanceof Number) {
					m.setEffectiveDate(((Number) eff).intValue());
				} else {
					String s = eff.toString().trim();
					if (!s.isEmpty()) m.setEffectiveDate(Integer.valueOf(s));
				}
			} catch (Exception ignore) {
				// invalid number format: leave as null
			}
		}
		if (row.get("matDesc") != null) m.setMatDesc(String.valueOf(row.get("matDesc")));
		// useYn 기본값을 'Y'로 설정 (DB의 NOT NULL 제약 대비)
		if (row.get("useYn") != null) {
			m.setUseYn(String.valueOf(row.get("useYn")));
		} else {
			m.setUseYn("Y");
		}
		return m;
	}
	
	//3. 원재료 그리드 삭제
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public String deleteMaterialMst(String empId, List<String> param) {
		log.info("materialMstDeleteList------------->{}",param);
		try {
			int updated = 0;
			if (param != null) {
				for (String key : param) {
					if (key == null) continue;
					String matId = key.trim();
					if (matId.isEmpty()) continue;
					Optional<MaterialMst> opt = materialMstRepository.findById(matId);
					if (opt.isPresent()) {
						MaterialMst m = opt.get();
						m.setUseYn("N");
						m.setUpdatedDate(LocalDate.now());
						m.setUpdatedId(empId);
						materialMstRepository.save(m);
						updated++;
					}
				}
			}
			log.info("materialMstDeleteList updated count: {}", updated);
			return "success";
		} catch (Exception e) {
			log.error("deleteMaterialMst error", e);
			return "error: " + e.getMessage();
		}
	}
}