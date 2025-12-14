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

import com.yeoun.masterData.dto.ProductMstDTO;
import com.yeoun.masterData.entity.ProductMst;
import com.yeoun.masterData.repository.ProductMstRepository;

import jakarta.persistence.EntityNotFoundException;
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
	@Transactional
	public String saveProductMst(String empId, Map<String, List<ProductMstDTO>> param) {
		log.info("productMstSaveList------------->{}",param);
		try {
			// 기존 완제품 품번(prdId와 동일한 prdId를 생성하려고하면 실패처리 추가 필요)
			// createdRows 객체로 생성
			List<ProductMstDTO> createdRows = param.get("createdRows");
			// 새로생성한 prd가있을때
			if (createdRows != null && !createdRows.isEmpty()) {
				// 새로생성한 prdMst row를 반복실행
				for (ProductMstDTO row : createdRows) {
					// 새로 입력한 prdId가 존재하지 않을때 새로생성
					ProductMst existP = productMstRepository.findById(row.getPrdId())
							.orElseGet(() -> {
						// 입력받은 DTO를 엔티티로 변형
						ProductMst p = row.toEntity();
						// 작성자 empId 등록
						p.setCreatedId(empId);
						
						return productMstRepository.save(p);
					});
				}
			}

			// updatedRows
			List<ProductMstDTO> updatedRows = param.get("updatedRows");
			// 수정 된 prdMst 가 존재할때
			if (updatedRows != null && !updatedRows.isEmpty()) {
				
				// 업데이트 로우정보 반복 
				for (ProductMstDTO row : updatedRows) {
					String prdId = row.getPrdId();
					// prdId로 수정할 엔티티 설정
					ProductMst target = productMstRepository.findById(prdId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품입니다."));
					// 엔티티 내용 수정
			        target.setPrdName(row.getPrdName());
			        target.setPrdCat(row.getPrdCat());
			        target.setPrdUnit(row.getPrdUnit());
			        target.setPrdStatus(row.getPrdStatus());
			        target.setUnitPrice(row.getUnitPrice());
			        target.setPrdSpec(row.getPrdSpec());
			        target.setUpdatedId(empId);
			        target.setUpdatedDate(LocalDate.now());
			        target.setEffectiveDate(row.getEffectiveDate());
				}
			}
			
			// deletedRows
			List<ProductMstDTO> deletedRows = param.get("deletedRows");
			// 삭제 된 prdMst 가 존재할때
			if (deletedRows != null && !deletedRows.isEmpty()) {
				
				// 업데이트 로우정보 반복 
				for (ProductMstDTO row : deletedRows) {
					String prdId = row.getPrdId();
					// 삭제요청 된 엔티티가 존재하면 삭제
				    if (productMstRepository.existsById(prdId)) {
				        productMstRepository.deleteById(prdId);
				    }
				}
			}

			return "success";
		} catch (Exception e) {
			log.error("saveProductMst error", e);
			return "error: " + e.getMessage();
		}
	}

	// -------------------------------------------------------------------------

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
