package com.yeoun.sales.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeoun.sales.entity.ShipmentItem;

public interface ShipmentItemRepository extends JpaRepository<ShipmentItem, Long> {

    // 출하 ID 기준 상세 목록 조회
    List<ShipmentItem> findByShipmentId(String shipmentId);
}


