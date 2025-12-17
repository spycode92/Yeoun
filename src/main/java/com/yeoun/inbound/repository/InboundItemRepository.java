package com.yeoun.inbound.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeoun.inbound.entity.InboundItem;

public interface InboundItemRepository extends JpaRepository<InboundItem, Long> {

	// 입고ID로 입고품목 조회
	List<InboundItem> findAllByInbound_InboundId(String inboundId);

	Optional<InboundItem> findTopByLotNoOrderByInboundItemIdDesc(String lotNo);

	InboundItem findByLotNo(String lotNo);

	InboundItem findFirstByLotNo(String lotNo);
	
}
