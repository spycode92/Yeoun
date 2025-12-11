package com.yeoun.masterData.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
	//1. 완제품 그리드 조회
	@Transactional(readOnly = true)
	public List<ProductMst> findAll() {
		log.info("productMstRepository.findAll() 조회된개수 - {}",productMstRepository.findAll());
		return productMstRepository.findAll();
	}

	//2. 완제품 그리드 저장
	// 프론트엔드에서 보낼 것으로 예상되는 구조:
	// { createdRows: [{prdId:..., itemName:...}, ...], updatedRows: [...], deletedRows: [...] }
	public String saveProductMst(String empId, Map<String,Object> param) {
		log.info("productMstSaveList------------->{}",param);
		try {
			// createdRows
			Object createdObj = param.get("createdRows");
			if (createdObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<Map<String,Object>> created = (List<Map<String,Object>>) createdObj;
				for (Map<String,Object> row : created) {
					ProductMst p = mapToProduct(row);
					p.setCreatedId(empId);
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
						p.setCreatedId(row.get("createdId").toString());
						p.setCreatedDate(LocalDate.parse(row.get("createdDate").toString()));
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
						productMstRepository.save(p);
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
			Object rowKeysObj = param.get("rowKeys");
			if (!(rowKeysObj instanceof List)) {
				result.put("status", "no_data");
				result.put("deletedCount", 0);
				return result;
			}
			@SuppressWarnings("unchecked")
			List<Object> rowKeys = (List<Object>) rowKeysObj;
			List<String> prdIds = rowKeys.stream()
				.map(key -> {
					if (key instanceof Number) {
						return String.valueOf(((Number) key).longValue());
					} else {
						return String.valueOf(key);
					}
				})
				.filter(s -> s != null && !s.trim().isEmpty())
				.collect(Collectors.toList());

			if (prdIds.isEmpty()) {
				result.put("status", "no_data");
				result.put("deletedCount", 0);
				return result;
			}

			List<ProductMst> existing = productMstRepository.findAllById(prdIds);
			if (existing == null || existing.isEmpty()) {
				result.put("status", "no_exist");
				result.put("deletedCount", 0);
				return result;
			}

			try {
				productMstRepository.deleteAll(existing);
			} catch (DataIntegrityViolationException dive) {
				log.error("deleteProduct DataIntegrityViolation (FK constraint?)", dive);
				result.put("status", "constraint_violation");
				result.put("message", "삭제 실패: 연관된 데이터가 존재합니다. 먼저 관련 데이터를 삭제하세요.");
				result.put("deletedCount", 0);
				return result;
			}

			result.put("status", "success");
			result.put("deletedCount", existing.size());
			return result;
		} catch (Exception e) {
			log.error("deleteProduct error", e);
			result.put("status", "error");
			result.put("message", e.getMessage());
			result.put("deletedCount", 0);
			return result;
		}
	}

}
