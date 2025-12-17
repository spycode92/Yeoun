package com.yeoun.masterData.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeoun.masterData.entity.QcItem;
import com.yeoun.masterData.repository.QcItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional
public class QcItemService {
	private final QcItemRepository qcItemRepository;
	
	//품질 항목 기준 qcId 목록 조회 (distinct)
	@Transactional(readOnly = true)
	public List<String> qcIdList() {
		return qcItemRepository.qcIdList();
	}
	//품질 항목 기준 조회
	@Transactional(readOnly = true)
	public List<Map<String, Object>> qcItemList(String qcItemId) {
		return qcItemRepository.findByQcItemList(qcItemId);
	}
	//품질 항목 기준 저장
	@Transactional
	public QcItem saveQcItem(String empId,QcItem qcItem) {
		log.info("qcItem-------------------------->",qcItem);
		Optional<QcItem> qc = qcItemRepository.findById(qcItem.getQcItemId());
		
		if(qc.isPresent()) {// 수정시 저장
	        QcItem existingItem = qc.get();

	        // 수정 시, 기존의 CreatedId/CreatedDate 유지
	        qcItem.setCreatedId(existingItem.getCreatedId());
	        qcItem.setCreatedDate(existingItem.getCreatedDate());
	        
	        // 3. UpdatedId와 UpdatedDate 설정
	        qcItem.setUpdatedId(empId);
	        qcItem.setUpdatedDate(LocalDate.now());
	        
	    } else {
	        
	        //신규 등록 시, CreatedId와 CreatedDate 설정
	        qcItem.setCreatedId(empId);
	        qcItem.setCreatedDate(LocalDate.now());
	        
	    }
		return qcItemRepository.save(qcItem);		
	}
	// 품질 항목 기준 삭제
	@Transactional
	public String deleteQcItem(List<String> qcItemIds) {
		log.info("qcItemRepository------------->{}", qcItemIds);
		try {
			// 기본 삭제 수행
			qcItemRepository.deleteAllById(qcItemIds);
			// 즉시 flush 하여 DB 제약(FOREIGN KEY 등) 오류가 있으면 이 시점에 발생하도록 함
			qcItemRepository.flush();
			return "success";
		} catch (DataIntegrityViolationException dive) {
			// 제약 위반은 구체적으로 로깅하고 사용자에게 명확한 메시지를 반환
			log.error("QC Item delete constraint violation", dive);
			String causeMsg = dive.getMostSpecificCause() != null ? dive.getMostSpecificCause().getMessage() : dive.getMessage();
			return "error: constraint violation - " + (causeMsg != null ? causeMsg : "referential integrity");
		} catch (Exception e) {
			log.error("qcItemRepository error", e);
			return "error: " + e.getMessage();
		}
	}

}
