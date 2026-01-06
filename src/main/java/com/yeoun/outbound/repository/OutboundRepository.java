package com.yeoun.outbound.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yeoun.outbound.entity.Outbound;

public interface OutboundRepository extends JpaRepository<Outbound, String> {

	// 오늘 날짜 기준 최대 seq 조회
	@Query(value = """
			SELECT MAX(i.outboundId)
			FROM Outbound i
			WHERE i.outboundId LIKE :pattern
			""")
	String findMaxOrderId(@Param("pattern") String pattern);

	// 출고 내역 조회
	Optional<Outbound> findByOutboundId(String outboundId);
	
	// 작업지시서로 출고기록조회
	Optional<Outbound> findByWorkOrderId(String workOrderId);

	// 출하지시서 ID로 출고 내역 조회
	Optional<Outbound> findByShipmentId(String shipmentId);

}
