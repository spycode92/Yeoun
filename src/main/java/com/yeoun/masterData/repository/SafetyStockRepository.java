package com.yeoun.masterData.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeoun.masterData.entity.SafetyStock;

public interface SafetyStockRepository extends JpaRepository<SafetyStock, String> {

}
