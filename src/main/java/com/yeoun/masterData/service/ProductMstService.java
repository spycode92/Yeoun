package com.yeoun.masterData.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.yeoun.masterData.entity.ProductMst;
import com.yeoun.masterData.repository.ProductMstRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional
public class ProductMstService {
	
	private final ProductMstRepository productMstRepository;

	//완제품 품목명(향수타입) 드롭다운
	@Transactional(readOnly = true)
	public List<Map<String, Object>> findByPrdItemNameList() {
		return productMstRepository.findByPrdItemNameList();
	}
	//완제품 제품유형 드롭다운
	@Transactional(readOnly = true)
	public List<Map<String, Object>> findByPrdTypeList() {
		return productMstRepository.findByPrdTypeList();
	}
	//완제품 단위 드롭다운
	@Transactional(readOnly = true)
	public List<Map<String, Object>> findByPrdUnitList() {
		return productMstRepository.findByPrdUnitList();
	}
	//완제품 제품상태 드롭다운
	@Transactional(readOnly = true)
	public List<Map<String, Object>> findByPrdStatusList() {
		return productMstRepository.findByPrdStatusList();
	}
 
	//1. 완제품 그리드 조회
	@Transactional(readOnly = true)
	public List<ProductMst> findAll() {
		return productMstRepository.findAll();
	}
	// 전체조회와 특정 prdId/prdName 조회(부분검색)를 모두 처리
	@Transactional(readOnly = true)
	public List<Map<String, Object>> findByPrdIdList(String prdId, String prdName) {
		// repository 쿼리에서 null/빈값은 전체조회로 처리하도록 되어 있음
		return productMstRepository.findByPrdIdList(prdId, prdName);
	}

	//2. 완제품 그리드 저장
	// 프론트엔드에서 보낼 것으로 예상되는 구조:
	// { createdRows: [{prdId:..., itemName:...}, ...], updatedRows: [...], deletedRows: [...] }
	@Transactional
	public String saveProductMst(String empId, Map<String,Object> param) {
		try {
			// createdRows
			Object createdObj = param.get("createdRows");
			if (createdObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<Map<String,Object>> created = (List<Map<String,Object>>) createdObj;
				for (Map<String,Object> row : created) {
					Object idObj = row.get("prdId");
					String prdId = (idObj == null) ? "" : String.valueOf(idObj).trim();
	
					if (!prdId.isEmpty()) {
						productMstRepository.findById(prdId)
							    .ifPresent(existingProduct -> {
							        throw new IllegalStateException("중복되는 품번이 이미 존재합니다.");
							    });//중복중
					}
					
					ProductMst p = mapToProduct(row);
					p.setCreatedId(empId);
					p.setCreatedDate(LocalDate.now());
					if (p.getUseYn() == null) p.setUseYn("Y");
					productMstRepository.save(p);
				}
			}

			// updatedRows
			Object updatedObj = param.get("updatedRows");
			if (updatedObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<Map<String,Object>> updated = (List<Map<String,Object>>) updatedObj;
				java.util.List<String> missingIds = new ArrayList<>();
				for (Map<String,Object> row : updated) {
					Object idObj = row.get("prdId");
					String prdId = (idObj == null) ? "" : String.valueOf(idObj).trim();
	
					ProductMst target = null;
					if (!prdId.isEmpty()) {
						Optional<ProductMst> opt = productMstRepository.findById(prdId);
						if (opt.isPresent()) target = opt.get();
					}
	
					if (target != null) {
						// 기존 레코드 업데이트
						ProductMst p = mapToProduct(row);
						// preserve created metadata from existing record unless explicitly provided
						if (row.get("createdId") == null) {
							p.setCreatedId(target.getCreatedId());
						}
						if (row.get("createdDate") == null) {
							p.setCreatedDate(target.getCreatedDate());
						} else {
							try { p.setCreatedDate(LocalDate.parse(row.get("createdDate").toString())); } catch(Exception ignore) {}
						}
						// preserve USE_YN when not provided in update
						if (p.getUseYn() == null) p.setUseYn(target.getUseYn() == null ? "Y" : target.getUseYn());
						p.setUpdatedId(empId);
						p.setUpdatedDate(LocalDate.now());
						productMstRepository.save(p);
					} else {
						// 존재하지 않는 prdId가 명시된 경우: PK 변경 시 의도치 않은 insert를 막기 위해 에러 처리
						if (!prdId.isEmpty()) {
							missingIds.add(prdId);
							continue;
						}
						// prdId가 비어있고 매칭되는 기존 레코드가 없으면 새로 저장 (신규 추가 케이스)
						ProductMst p = mapToProduct(row);
						p.setCreatedId(empId);
						if (p.getUseYn() == null) p.setUseYn("Y");
						productMstRepository.save(p);
					}
			}
			 	
			}

			return "success";
		} catch(Exception e) {
			return "error: " + e.getMessage();
		}
	}


	// 유틸: Map 데이터를 ProductMst 엔티티로 변환
	private ProductMst mapToProduct(Map<String,Object> row) {
		ProductMst p = new ProductMst();
		if (row == null) return p;
		if (row.get("prdId") != null) p.setPrdId(String.valueOf(row.get("prdId")));
		if (row.get("itemName") != null) p.setItemName(String.valueOf(row.get("itemName")));
		if (row.get("prdName") != null) p.setPrdName(String.valueOf(row.get("prdName")));
		if (row.get("prdCat") != null) p.setPrdCat(String.valueOf(row.get("prdCat")));
		if (row.get("prdUnit") != null) p.setPrdUnit(String.valueOf(row.get("prdUnit")));
		if (row.get("prdStatus") != null) p.setPrdStatus(String.valueOf(row.get("prdStatus")));
		if (row.get("prdSpec") != null) p.setPrdSpec(String.valueOf(row.get("prdSpec")));
		if (row.get("unitPrice") != null) {
			try { p.setUnitPrice(new java.math.BigDecimal(String.valueOf(row.get("unitPrice")))); } catch(Exception e) {}
		}
		if (row.get("effectiveDate") != null) {
			try { p.setEffectiveDate(Integer.valueOf(String.valueOf(row.get("effectiveDate")))); } catch(Exception e) {}
		}
		// useYn 처리: 전달된 값이 있으면 사용
		if (row.get("useYn") != null) {
			p.setUseYn(String.valueOf(row.get("useYn")).trim());
		}
		return p;
	}
	//3. 완제품 그리드 삭제
	/**
	 * 주어진 키 목록에 해당하는 제품을 삭제합니다.
	 * 입력으로 Long, Integer, String 등 다양한 형태의 키를 허용합니다.
	 * 반환값은 처리 결과 메시지이며, 성공 시 삭제된 건수를 포함합니다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Map<String, Object> deleteProduct(Map<String, Object> param) {
		log.info("deleteProduct------------->{}",param);
		Map<String, Object> result = new HashMap<>();
		try {
			Object keysObj = param.get("rowKeys");
			int updated = 0;
			if (keysObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<String> rowKeys = (List<String>) keysObj;
				for (String key : rowKeys) {
					if (key == null) continue;
					String prdId = key.trim();
					if (prdId.isEmpty()) continue;
					Optional<ProductMst> opt = productMstRepository.findById(prdId);
					if (opt.isPresent()) {
						ProductMst p = opt.get();
						p.setUseYn("N");
						p.setUpdatedDate(java.time.LocalDate.now());
						productMstRepository.save(p);
						updated++;
					}
				}
			}
			result.put("status", "success");
			result.put("updatedCount", updated);
			return result;
		} catch (DataIntegrityViolationException dive) {
			log.error("deleteProduct data integrity error", dive);
			result.put("status", "error");
			result.put("message", dive.getMessage());
			return result;
		} catch (Exception e) {
			log.error("deleteProduct error", e);
			result.put("status", "error");
			result.put("message", e.getMessage());
			return result;
		}
	}

}
