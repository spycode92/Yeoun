package com.yeoun.sales.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.yeoun.sales.dto.ClientItemDTO;
import com.yeoun.sales.entity.ClientItem;
import com.yeoun.sales.repository.ClientItemRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClientItemService {

    private final ClientItemRepository repo;
    

    @Transactional
    public void addItems(String clientId, List<ClientItemDTO> items, String empId) {

        for (ClientItemDTO dto : items) {
        	ClientItem item = ClientItem.builder()
        	        .clientId(clientId)
        	        .materialId(dto.getMaterialId())
        	        .unitPrice(dto.getUnitPrice())
        	        .minOrderQty(dto.getMoq())
        	        .unit(dto.getUnit())
        	        .orderUnit(dto.getOrderUnit())   
        	        .leadDays(dto.getLeadDays())     
        	        .supplyAvailable(dto.getSupplyAvailable())
        	        .createdAt(LocalDateTime.now())
        	        .createdBy(empId)
        	        .build();


            repo.save(item);
        }
    }

    /** 🔥 품명 + 단위까지 포함된 DTO 목록 반환 */
    public List<ClientItemDTO> getItems(String clientId) {
        return repo.findItemsWithMaterialInfo(clientId);
    }
   
    
    
    
}
