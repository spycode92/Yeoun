package com.yeoun.masterData.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeoun.masterData.entity.ProdLine;

@Repository
public interface ProdLineRepository extends JpaRepository<ProdLine, String> {

}
