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

import com.yeoun.masterData.dto.MaterialMstDTO;
import com.yeoun.masterData.entity.MaterialMst;
import com.yeoun.masterData.entity.ProductMst;
import com.yeoun.masterData.repository.MaterialMstRepository;

import jakarta.persistence.EntityNotFoundException;
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
	@Transactional
	public String saveMaterialMst(String empId, Map<String, List<MaterialMstDTO>> param) {
		log.info("materialMstSaveList------------->{}",param);
		try {
			// createdRows
			List<MaterialMstDTO> created = param.get("createdRows");
			// 새로 추가한 원재료 정보가 있을 때
			if (created != null && !created.isEmpty()) {
				// 리스트 반복
				for (MaterialMstDTO row : created) {
					// db에 존재하지않나 한번더 확인
					MaterialMst existM = materialMstRepository.findById(row.getMatId()) 
							.orElseGet(() -> { // 존재하지 않는다면
								// 새로 저장할 원자재 객체 생성후
								MaterialMst newMst = row.toEntity();
								
								newMst.setCreatedId(empId);
								// 저장
								materialMstRepository.save(newMst);
								
								return materialMstRepository.save(newMst);
							});
				}
			}
			
			log.info("param.get(\"updatedRows\")------------------->{}",param.get("updatedRows"));
			// updatedRows
			List<MaterialMstDTO> updated = param.get("updatedRows");
			
			if (updated != null && !updated.isEmpty()) {
				// 업데이트 로우 정보 반복
				for (MaterialMstDTO row : updated) {
					// 수정할 원자재id 
					String matId = row.getMatId();
	
					MaterialMst target = materialMstRepository.findById(matId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 원자재입니다."));
					
					// 엔티티 내용 수정
					target.setMatName(row.getMatName());
					target.setMatType(row.getMatType());
					target.setMatUnit(row.getMatUnit());
					target.setEffectiveDate(row.getEffectiveDate());
					target.setMatDesc(row.getMatDesc());
					target.setUpdatedId(empId);
					target.setUseYn(row.getUseYn());
				}
			 	
			}
			
			// deletedRows
			List<MaterialMstDTO> deleted = param.get("deletedRows");
			// 삭제된 원자재가 존재할때
			if(deleted != null && !deleted.isEmpty()) {
				for (MaterialMstDTO row : deleted) {
					String matId = row.getMatId();
					// 삭제요청된 엔티티가 존재하면 삭제
					if(materialMstRepository.existsById(matId)) {
						materialMstRepository.deleteById(matId);
					}
				}
			}


			return "success";
		} catch (Exception e) {
			log.error("saveProductMst error", e);
			return "error: " + e.getMessage();
		}
	}

	
	// ---------------------------------------------------------------
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