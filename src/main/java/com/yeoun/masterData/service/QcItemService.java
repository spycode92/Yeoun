package com.yeoun.masterData.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeoun.masterData.entity.ProductMst;
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
	
	//조회
	@Transactional(readOnly = true)
	public List<QcItem> findAll() {
		return qcItemRepository.findAll();
	}
	//저장
	@Transactional
	public QcItem saveQcItem(String empId,QcItem qcItem) {
		qcItem.setCreatedId(empId);
		return qcItemRepository.save(qcItem);		
	}
	//삭제
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
