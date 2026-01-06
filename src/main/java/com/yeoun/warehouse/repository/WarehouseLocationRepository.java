package com.yeoun.warehouse.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.yeoun.warehouse.entity.WarehouseLocation;

public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, String> {

	// zone으로 창고 조회
	List<WarehouseLocation> findAllByZone(String zoneName);

	// zone과 rack
	List<WarehouseLocation> findAllByZoneAndRack(String zone, String rack);

	// 창고 ID 시퀀스 가져오기
	@Query(value = "SELECT WAREHOUSE_LOCATION_SEQ.NEXTVAL FROM DUAL", nativeQuery = true)
	Long getNextLocationSeq();

	// 창고 ID로 정보 조회
	Optional<WarehouseLocation> findByLocationId(String locationId);
	
	// 활성화된 창고 정보 가져오기
	List<WarehouseLocation> findByUseYn(String useYn);

}
