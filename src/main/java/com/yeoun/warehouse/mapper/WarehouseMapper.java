package com.yeoun.warehouse.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.yeoun.inventory.dto.WarehouseLocationDTO;

@Mapper
public interface WarehouseMapper {

	// 창고 조회 (전체 조회)
	List<WarehouseLocationDTO> findAllLocations();

}
