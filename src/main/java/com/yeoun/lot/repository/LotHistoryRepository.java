package com.yeoun.lot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.yeoun.lot.entity.LotHistory;

@Repository
public interface LotHistoryRepository extends JpaRepository<LotHistory, Long> {
	
	// LOT + 공정 기준으로 가장 마지막 이벤트 1건 조회
	Optional<LotHistory> findTopByLot_LotNoAndProcess_ProcessIdOrderByHistIdDesc(
            String lotNo,
            String processId
    );

	// 출하 정보
	// 특정 LOT에 FG_SHIP(출하) 이력이 존재하는지 확인
	boolean existsByLot_LotNoAndEventType(String lotNo, String eventType);
	
	// 특정 LOT의 FG_SHIP(출하) 이력 중 가장 최신 1건 조회
	Optional<LotHistory> findFirstByLot_LotNoAndEventTypeOrderByCreatedDateDesc(String lotNo, String eventType);
	
	// 특정 LOT의 FG_SHIP(출하) 수량 합계 -> 부분출하도 대비
	@Query("""
			select coalesce(sum(h.quantity), 0)
			from LotHistory h
			where h.lot.lotNo = :lotNo
			and h.eventType = :eventType
			""")
	int sumQuantityByLotNoAndEventType(@Param("lotNo") String lotNo,
									   @Param("eventType") String eventType);
	
	Optional<LotHistory> findFirstByLot_LotNoOrderByCreatedDateAscHistIdAsc(String lotNo);
}
