package com.yeoun.masterData.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeoun.masterData.entity.MaterialMst;

public interface MaterialMstRepository extends JpaRepository<MaterialMst, String> {

	// 원자재 조회
	Optional<MaterialMst> findByMatId(String materialOrder);

	List<MaterialMst> findByMatType(String matType);

	
	//2. 원자재 수정
	//3. 원자재 삭제

}
