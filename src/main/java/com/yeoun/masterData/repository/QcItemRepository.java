package com.yeoun.masterData.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeoun.masterData.entity.QcItem;

@Repository
public interface QcItemRepository extends JpaRepository<QcItem, String> {

	//
	// 대상구분 + 사용여부 기준으로 항목 조회
    List<QcItem> findByTargetTypeAndUseYnOrderBySortOrderAsc(String targetType, String useYn);
    
}
