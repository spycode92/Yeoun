package com.yeoun.masterData.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeoun.masterData.entity.BomMst;
import com.yeoun.masterData.entity.BomMstId;

@Repository
public interface BomMstRepository extends JpaRepository<BomMst, BomMstId>{
  
  // 특정 품목의 bom 찾기
	List<BomMst> findByPrdId(String prdId);

	Optional<BomMst> findByPrdIdAndMatId(String prdId, String matId);

}
