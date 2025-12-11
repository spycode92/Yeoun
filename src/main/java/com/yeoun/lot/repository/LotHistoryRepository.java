package com.yeoun.lot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeoun.lot.entity.LotHistory;

@Repository
public interface LotHistoryRepository extends JpaRepository<LotHistory, Long> {
	
	// LOT + 공정 기준으로 가장 마지막 이벤트 1건 조회
	Optional<LotHistory> findTopByLot_LotNoAndProcess_ProcessIdOrderByHistIdDesc(
            String lotNo,
            String processId
    );

}
