package com.yeoun.inventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.yeoun.inventory.entity.Inventory;
import com.yeoun.inventory.entity.WarehouseLocation;

public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, String> {

	// zone으로 창고 조회
	List<WarehouseLocation> findAllByZone(String zoneName);

	// zone 이름으로 모두 삭제
	void deleteAllByZone(String zoneName);

	// zone과 rack
	List<WarehouseLocation> findAllByZoneAndRack(String zone, String rack);

	// zone과 rack 이름으로 모두 삭제
	void deleteAllByZoneAndRack(String zone, String rack);

	// 창고 ID 시퀀스 가져오기
	@Query(value = "SELECT WAREHOUSE_LOCATION_SEQ.NEXTVAL FROM DUAL", nativeQuery = true)
	Long getNextLocationSeq();

}
