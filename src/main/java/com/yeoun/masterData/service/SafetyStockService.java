package com.yeoun.masterData.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeoun.masterData.entity.ProductMst;
import com.yeoun.masterData.entity.SafetyStock;
import com.yeoun.masterData.repository.SafetyStockRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional
public class SafetyStockService {
	
	private final SafetyStockRepository safetyStockRepository;

	//---------------------------------------------
	//안전재고 품목종류 드롭다운
	public List<Map<String, Object>> getMatTypeList() {
		return safetyStockRepository.findByMatTypeList();
	}
	//안전재고 단위 드롭다운
	public List<Map<String, Object>> getMatUnitList() {
		return safetyStockRepository.findByMatUnitList();
	}
	//안전재고 정책방식 드롭다운
	public List<Map<String, Object>> getSafetyStockPolicyList() {
		return safetyStockRepository.findBySafetyStockPolicyList();
	}
	//안전재고 상태 드롭다운
	public List<Map<String, Object>> getSafetyStockStatusList() {
		return safetyStockRepository.findByPrdStatusList();
	}
	
	//1. 안전재고 그리드 조회
	@Transactional(readOnly = true)
	public List<SafetyStock> findByItemlList(String itemId, String itemName) {
		log.info("safetyStockRepository.findByItemlList() 조회된개수 - {}",safetyStockRepository.findByItemlList(itemId, itemName));
		return safetyStockRepository.findByItemlList(itemId, itemName);
	}

	//2. 안전재고 그리드 저장
	public String saveSafetyStock(String empId, Map<String,Object> param) {
		log.info("safetyStockSaveList------------->{}",param);
		try {
			Object createdObj = param.get("createdRows");
			if (createdObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<Map<String,Object>> created = (List<Map<String,Object>>) createdObj;
				for (Map<String,Object> row : created) {
						SafetyStock s = mapToSafetyStock(row);
						// 중복 검사: itemId가 비어있지 않으면 기존 레코드 존재 여부 확인
						if (s.getItemId() != null && !s.getItemId().isEmpty()) {
							if (safetyStockRepository.existsById(s.getItemId())) {
								throw new IllegalStateException("중복되는 itemId가 이미 존재합니다. (itemId=" + s.getItemId() + ")");
							}
						}
						safetyStockRepository.save(s);
				}
			}
			
			// updatedRows
			Object updatedObj = param.get("updatedRows");
			if (updatedObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<Map<String,Object>> updated = (List<Map<String,Object>>) updatedObj;
				List<String> missingIds = new ArrayList<>();
				for (Map<String,Object> row : updated) {
					Object idObj = row.get("itemId");
					String itemId = (idObj == null) ? "" : String.valueOf(idObj).trim();
	
					SafetyStock target = null;
					if (!itemId.isEmpty()) {
						Optional<SafetyStock> opt = safetyStockRepository.findById(itemId);
						if (opt.isPresent()) target = opt.get();
					}
	
					if (target != null) {
						// 기존 레코드 업데이트
						SafetyStock s = mapToSafetyStock(row);
						safetyStockRepository.save(s);
					} else {
						// 존재하지 않는 itemId가 명시된 경우: PK 변경 시 의도치 않은 insert를 막기 위해 에러 처리
						if (!itemId.isEmpty()) {
							missingIds.add(itemId);
							continue;
						}
						// itemId가 비어있고 매칭되는 기존 레코드가 없으면 새로 저장 (신규 추가 케이스)
						SafetyStock s = mapToSafetyStock(row);
						if (s.getItemId() != null && !s.getItemId().isEmpty()) {
							if (safetyStockRepository.existsById(s.getItemId())) {
								throw new IllegalStateException("중복되는 itemId가 이미 존재합니다. (itemId=" + s.getItemId() + ")");
							}
						}
						safetyStockRepository.save(s);
					}
			}
			 	
			}
			return "success";
		} catch (Exception e) {
			return "error: " + e.getMessage();
		}
	}

	//3. 안전재고 삭제 (itemId 목록으로 삭제)
	public String deleteSafetyStock(String empId, java.util.List<String> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) return "No itemIds provided";
		int deleted = 0;
		for (String id : itemIds) {
			if (id == null) continue;
			if (safetyStockRepository.existsById(id)) {
				safetyStockRepository.deleteById(id);
				deleted++;
			}
		}
		return "Deleted SafetyStock rows: " + deleted;
	}

	// Map을 SafetyStock 엔티티로 변환하는 헬퍼 메서드
	private SafetyStock mapToSafetyStock(Map<String, Object> row) {
		SafetyStock s = new SafetyStock();

		if (row == null) return s;

		// 문자열 필드
		if (row.get("itemId") != null) s.setItemId(String.valueOf(row.get("itemId")).trim());
		if (row.get("itemType") != null) s.setItemType(String.valueOf(row.get("itemType")).trim());
		if (row.get("itemName") != null) s.setItemName(String.valueOf(row.get("itemName")).trim());
		if (row.get("itemUnit") != null) s.setItemUnit(String.valueOf(row.get("itemUnit")).trim());
		if (row.get("policyType") != null) s.setPolicyType(String.valueOf(row.get("policyType")).trim());
		if (row.get("status") != null) s.setStatus(String.valueOf(row.get("status")).trim());
		if (row.get("remark") != null) s.setRemark(String.valueOf(row.get("remark")).trim());

		// 숫자 필드: 널/빈 문자열/숫자 변환을 안전하게 처리
		Object vol = row.get("volume");
		if (vol != null) {
			Long v = parseLongSafely(vol);
			if (v != null) s.setVolume(v);
		}

		Object policyDays = row.get("policyDays");
		if (policyDays != null) {
			Long pd = parseLongSafely(policyDays);
			if (pd != null) s.setPolicyDays(pd);
		}

		Object safetyDaily = row.get("safetyStockQtyDaily");
		if (safetyDaily != null) {
			Long sd = parseLongSafely(safetyDaily);
			if (sd != null) s.setSafetyStockQtyDaily(sd);
		}

		Object safetyQty = row.get("safetyStockQty");
		if (safetyQty != null) {
			Long sq = parseLongSafely(safetyQty);
			if (sq != null) s.setSafetyStockQty(sq);
		}

		return s;
	}

	private Long parseLongSafely(Object o) {
		if (o == null) return null;
		if (o instanceof Number) return ((Number) o).longValue();
		String s = String.valueOf(o).trim();
		if (s.isEmpty()) return null;
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			try {
				// 소수점이 있을 경우 소수 제거 후 파싱
				BigDecimal bd = new BigDecimal(s);
				return bd.longValue();
			} catch (Exception ex) {
				log.warn("Failed to parse long from value: {}", o);
				return null;
			}
		}
	}

}
