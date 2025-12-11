package com.yeoun.inventory.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.yeoun.inventory.dto.MaterialOrderDTO;
import com.yeoun.inventory.dto.SupplierDTO;

@Mapper
public interface PurchaseMapper {

	// 공급업체 조회
	List<SupplierDTO> findAllSuppliers();

	// 발주 상세 정보 가져오기
	MaterialOrderDTO findPurchaseOrder(String id);
}
