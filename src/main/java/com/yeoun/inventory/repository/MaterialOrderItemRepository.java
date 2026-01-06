package com.yeoun.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeoun.inventory.entity.MaterialOrderItem;

public interface MaterialOrderItemRepository extends JpaRepository<MaterialOrderItem, Long> {

}
