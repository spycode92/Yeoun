package com.yeoun.lot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeoun.lot.entity.LotRelationship;

@Repository
public interface LotRelationshipRepository extends JpaRepository<LotRelationship, Long> {

	// 부모 LOT 기준으로 자재 관계 조회
	List<LotRelationship> findByOutputLot_LotNo(String lotNo);
	
}
