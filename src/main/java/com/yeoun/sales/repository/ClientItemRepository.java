package com.yeoun.sales.repository;

import com.yeoun.sales.dto.ClientItemDTO;
import com.yeoun.sales.entity.ClientItem;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientItemRepository extends JpaRepository<ClientItem, Long> {

	// itemId로 ClientItem 조회
	Optional<ClientItem> findByItemId(Long itemId);
	
	//협력사 제품 목록 
	 List<ClientItem> findByClientId(String clientId);
	 
	 
	 //협력사 제품목록 조회
	 @Query("""
			    SELECT new com.yeoun.sales.dto.ClientItemDTO(
			        ci.itemId,
			        ci.materialId,
			        m.matName,
			        m.matUnit,       
			        ci.unit,         
			        ci.orderUnit,
			        ci.unitPrice,
			        ci.minOrderQty,
			        ci.supplyAvailable,
			        ci.leadDays,
			        m.matType
			    )
			    FROM ClientItem ci 
			    JOIN MaterialMst m ON ci.materialId = m.matId
			    WHERE ci.clientId = :clientId
			""")
			List<ClientItemDTO> findItemsWithMaterialInfo(@Param("clientId") String clientId);


	
}
