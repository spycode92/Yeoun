package com.yeoun.qc.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeoun.qc.entity.QcResultDetail;

@Repository
public interface QcResultDetailRepository extends JpaRepository<QcResultDetail, String> {
	
	List<QcResultDetail> findByQcResultId(String QcResultId);

}
