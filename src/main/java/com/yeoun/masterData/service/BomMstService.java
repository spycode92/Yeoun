package com.yeoun.masterData.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeoun.masterData.entity.BomMst;
import com.yeoun.masterData.entity.MaterialMst;
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
	
	
	//BOM 완제품 드롭다운 조회 findPrdList
	@Transactional(readOnly = true)
	public List<Map<String, Object>> findBomPrdList() {
		return bomMstRepository.findBomPrdList();
	}
	
	//BOM 원재료 드롭다운 조회 findMatList
	@Transactional(readOnly = true)
	public List<MaterialMst> findBomMatList() {
		return bomMstRepository.findBomMatList();
	}
	//BOM 단위 드롭다운조회
	@Transactional(readOnly = true)
	public List<Map<String, Object>> findBomUnitList() {
		return bomMstRepository.findByBomUnitList();
	}
	
	//1. BOM 그리드 조회
	@Transactional(readOnly = true)
	public List<Map<String, Object>> findBybomList(String bomId, String matId) {
		log.info("bomMstRepository.findBybomList() 조회된개수 - {}",bomMstRepository.findBybomList(bomId, matId));
		return bomMstRepository.findBybomList(bomId, matId);
	}
	//1-2. BOM 상세 그리드 조회
	@Transactional(readOnly = true)
	public List<Object[]> findAllDetail() {
		log.info("bomMstRepository.findAllDetail() 조회된개수 - {}",bomMstRepository.findAllDetail());
		return bomMstRepository.findAllDetail();
	}
	//1-3. BOM 상세 -완제품 그리드 조회 (bomId로 조회)
	@Transactional(readOnly = true)
	public List<Object[]> getBomPrdList(String bomId){
		log.info("getBomPrdListByBomId bomId------------->{}", bomId);
		return bomMstRepository.findAllDetailPrd(bomId);
	}

	//1-4. BOM 상세 -원재료 그리드 조회 (bomId로 조회)
	@Transactional(readOnly = true)
	public List<Object[]> getBomMatList(String bomId){
		log.info("getBomMatListByBomId bomId------------->{}", bomId);
		return bomMstRepository.findAllDetailMat(bomId);
	}

	//1-5. BOM 상세 -원재료(포장재) 그리드 조회 (bomId로 조회)
	@Transactional(readOnly = true)
	public List<Object[]> getBomMatTypeList(String bomId){
		log.info("getBomMatTypeListByBomId bomId------------->{}", bomId);
		return bomMstRepository.findAllDetailMatType(bomId);
	}

	//2. BOM 그리드 저장
	public String saveBomMst(String empId,Map<String,Object> param) {
		log.info("bomMstSaveList------------->{}",param);
		try{
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
					// 중복 검사: prdId + matId 조합이 이미 존재하는지 확인
					String prdIdCheck = b.getPrdId();
					String matIdCheck = b.getMatId();
					if (prdIdCheck != null && matIdCheck != null && !prdIdCheck.isEmpty() && !matIdCheck.isEmpty()) {
						if (bomMstRepository.findByPrdIdAndMatId(prdIdCheck, matIdCheck).isPresent()) {
							throw new IllegalStateException("중복되는 완제품ID +원재료ID 조합이 이미 존재합니다. (prdId=" + prdIdCheck + ", matId=" + matIdCheck + ")");
						}
					}
					b.setCreatedId(empId);
					b.setCreatedDate(LocalDate.now());
					// ensure USE_YN defaults to 'Y' when not provided
					if (b.getUseYn() == null) b.setUseYn("Y");
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
						b.setCreatedId(target.getCreatedId());
						b.setCreatedDate(target.getCreatedDate());
						// preserve existing USE_YN when row doesn't provide it
						if (b.getUseYn() == null) b.setUseYn(target.getUseYn() == null ? "Y" : target.getUseYn());
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
					return "success";
		} catch(Exception e) {
			return "error: " + e.getMessage();
		}
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
		// useYn 처리: 전달된 값이 있으면 사용, 없으면 null으로 두어 호출부에서 기본 처리
		if (row.get("useYn") != null) {
			b.setUseYn(String.valueOf(row.get("useYn")).trim());
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
			// Mark as unused (soft delete) instead of physical delete
			Optional<BomMst> opt = bomMstRepository.findByPrdIdAndMatId(prdId, matId);
			if (opt.isPresent()) {
				BomMst entity = opt.get();
				entity.setUseYn("N");
				entity.setUpdatedId(empId);
				entity.setUpdatedDate(LocalDate.now());
				bomMstRepository.save(entity);
				deletedTotal++;
			} else {
				// record did not exist — count as not deleted, but continue
				log.warn("No BOM record found for delete pair: prdId={}, matId={}", prdId, matId);
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
