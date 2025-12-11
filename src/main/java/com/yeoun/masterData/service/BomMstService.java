package com.yeoun.masterData.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeoun.masterData.entity.BomMst;
import com.yeoun.masterData.mapper.BomMstMapper;
import com.yeoun.masterData.repository.BomMstRepository;
import com.yeoun.outbound.dto.OutboundOrderItemDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional
public class BomMstService {
	private final BomMstRepository bomMstRepository;
	private final BomMstMapper bomMstMapper;
	
	//1. 완제품 그리드 조회
	@Transactional(readOnly = true)
	public List<BomMst> findAll() {
		log.info("bomMstRepository.findAll() 조회된개수 - {}",bomMstRepository.findAll());
		return bomMstRepository.findAll();
	}
	//2. BOM 그리드 저장
	public String saveBomMst(String empId,Map<String,Object> param) {
		log.info("bomMstSaveList------------->{}",param);
		
			Object createdObj = param.get("createdRows");
			int createdCount = 0;
			if (createdObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<Map<String,Object>> created = (List<Map<String,Object>>) createdObj;
				for (Map<String,Object> row : created) {
					BomMst b = mapToBom(row);
					if (b.getMatQty() == null) {
						throw new IllegalArgumentException("matQty is required for BOM row (prdId=" + b.getPrdId() + ", matId=" + b.getMatId() + ")");
					}
					b.setCreatedId(empId);
					b.setCreatedDate(LocalDate.now());
					bomMstRepository.save(b);
					createdCount++;
				}
			}
			
			// updatedRows
			Object updatedObj = param.get("updatedRows");
			if (updatedObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<Map<String,Object>> updated = (List<Map<String,Object>>) updatedObj;
				List<String> missingIds = new ArrayList<>();
				for (Map<String,Object> row : updated) {
					Object prdIdObj = row.get("prdId");
					Object matIdObj = row.get("matId");
					String prdId = (prdIdObj == null) ? "" : String.valueOf(prdIdObj).trim();
					String matId = (matIdObj == null) ? "" : String.valueOf(matIdObj).trim();
	
					BomMst target = null;
					if (!prdId.isEmpty()) {
						Optional<BomMst> opt = bomMstRepository.findByPrdIdAndMatId(prdId,matId);
						if (opt.isPresent()) target = opt.get();
					}
	
					if (target != null) {
						// 기존 레코드 업데이트
						BomMst b = mapToBom(row);
						b.setCreatedId(row.get("createdId").toString());
						b.setCreatedDate(LocalDate.parse(row.get("createdDate").toString()));
						b.setUpdatedId(empId);
						b.setUpdatedDate(LocalDate.now());
						bomMstRepository.save(b);
					} else {
						// 존재하지 않는 prdId,matId가 명시된 경우: PK 변경 시 의도치 않은 insert를 막기 위해 에러 처리
						if (!prdId.isEmpty() || !matId.isEmpty()) {
							missingIds.add(prdId);
							continue;
						}
						// prdId가 비어있고 매칭되는 기존 레코드가 없으면 새로 저장 (신규 추가 케이스)
						BomMst b = mapToBom(row);
						b.setCreatedId(empId);
						bomMstRepository.save(b);
					}
				}
			 	
			}
			
			// Force flush so DB constraint errors occur inside try/catch and we can return meaningful message
			bomMstRepository.flush();
			return "Success: BOM 저장이 완료되었습니다. (created=" + createdCount + ")";
	}

	// Map을 BomMst 엔티티로 변환하는 헬퍼 메서드
	private BomMst mapToBom(Map<String, Object> row) {
		BomMst b = new BomMst();
		if (row.get("bomId") != null) {
			b.setBomId(String.valueOf(row.get("bomId")));
		}
		if (row.get("prdId") != null) {
			b.setPrdId(String.valueOf(row.get("prdId")));
		}
		if (row.get("matId") != null) {
			b.setMatId(String.valueOf(row.get("matId")));
		}
		if (row.get("matQty") != null) {
			b.setMatQty(new java.math.BigDecimal(String.valueOf(row.get("matQty"))));
		}
		if (row.get("matUnit") != null) {
			b.setMatUnit(String.valueOf(row.get("matUnit")));
		}
		if (row.get("bomSeqNo") != null) {
			Object seqObj = row.get("bomSeqNo");
			Long seqNo = null;
			if (seqObj instanceof Number) {
				seqNo = ((Number) seqObj).longValue();
			} else {
				String s = String.valueOf(seqObj).trim();
				if (!s.isEmpty()) {
					try {
						seqNo = Long.parseLong(s);
					} catch (NumberFormatException nfe) {
						log.warn("bomSeqNo is not an integer, skipping: {}", seqObj);
					}
				}
			}
			if (seqNo != null) b.setBomSeqNo(seqNo);
		}
		return b;
	}

	//4-2. BOM 삭제 (prdId + matId 쌍으로 삭제 요청 처리)
	public String deleteBomMstByPairs(String empId, List<java.util.Map<String, String>> rows) {
		log.info("deleteBomMstByPairs called by {} rows={}", empId, rows);
		int deletedTotal = 0;
		if (rows == null || rows.isEmpty()) {
			return "Success: BOM 삭제가 완료되었습니다. (deleted=0)";
		}
		for (java.util.Map<String, String> row : rows) {
			if (row == null) continue;
			String prdId = row.get("prdId");
			String matId = row.get("matId");
			if (prdId == null || matId == null) {
				log.warn("Skipping delete pair because prdId or matId is missing: {}", row);
				continue;
			}
			bomMstRepository.findByPrdIdAndMatId(prdId, matId).ifPresent(entity -> {
				bomMstRepository.delete(entity);
			});
			// Note: we cannot increment deletedTotal inside lambda easily; re-check existence/size
			// For simplicity, check again
			if (!bomMstRepository.findByPrdIdAndMatId(prdId, matId).isPresent()) {
				// assume deletion succeeded or record did not exist
				deletedTotal++;
			}
		}
		return "Success: BOM 삭제가 완료되었습니다. (deleted=" + deletedTotal + ")";
	}
	// =================================================================
	// prdId에 해당하는 BOM 리스트 조회
	public List<OutboundOrderItemDTO> getBomListByPrdId(String prdId) {
		return bomMstMapper.findByPrdIdList(prdId);
	}

}
