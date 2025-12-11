package com.yeoun.outbound.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeoun.outbound.entity.OutboundItem;

public interface OutboundItemRepository extends JpaRepository<OutboundItem, Long>{

	// 출고 품목 조회
	List<OutboundItem> findByOutbound_OutboundId(String outboundId);
	
	// 작업지시번호 기준 출고 품목 조회 (원자재 LOT 연계용)
	List<OutboundItem> findByOutbound_WorkOrderIdAndItemTypeIn(
            String workOrderId,        
            List<String> itemTypes     
    );

}
