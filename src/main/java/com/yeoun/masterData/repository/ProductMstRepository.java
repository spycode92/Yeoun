package com.yeoun.masterData.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
  
import com.yeoun.masterData.entity.MaterialMst;
import com.yeoun.masterData.entity.ProductMst;

@Repository
public interface ProductMstRepository extends JpaRepository<ProductMst, String> {

	//1. 완제품 조회
	//Optional<ProductMst> findByProductAll();
	//2. 완제품 수정
	//3. 완제품 삭제

	Optional<ProductMst> findByItemNameAndPrdName(String itemName, String prdName);

	// 제품ID로 조회
	Optional<ProductMst> findByPrdId(String prdId);

}
