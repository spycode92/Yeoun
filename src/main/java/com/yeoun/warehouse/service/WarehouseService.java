package com.yeoun.warehouse.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.yeoun.inventory.dto.WarehouseLocationDTO;
import com.yeoun.inventory.repository.InventoryRepository;
import com.yeoun.warehouse.dto.WarehouseLocationCreateRequest;
import com.yeoun.warehouse.entity.WarehouseLocation;
import com.yeoun.warehouse.mapper.WarehouseMapper;
import com.yeoun.warehouse.repository.WarehouseLocationRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class WarehouseService {
	private final WarehouseLocationRepository warehouseLocationRepository;
	private final InventoryRepository inventoryRepository;
	private final WarehouseMapper warehouseMapper;
	
	// 창고 조회 (전체 조회)
	public List<WarehouseLocationDTO> getAllLocations() {
		return warehouseMapper.findAllLocations();
	}

	// 창고 상태 변경
	@Transactional
	public void updateLocationStatus(String locationId, String useYn) {
		WarehouseLocation location = warehouseLocationRepository.findByLocationId(locationId)
				.orElseThrow(() -> new NoSuchElementException("창고 위치를 찾을 수 없습니다."));
		
		// 비활성화 시키는 경우 창고에 재고가 있는지 확인 하는 로직
		if ("N".equals(useYn)) {
			boolean existInventory =  inventoryRepository.existsByWarehouseLocation_LocationId(locationId);
			
			if (existInventory) {
				throw new IllegalStateException("재고가 있는 위치는 비활성화할 수 없습니다");
			}
			
		}
		
		// 상태값 변경
		location.changeStatus(useYn);
	}
	
	// 창고 등록
	@Transactional
	public void createLocations(WarehouseLocationCreateRequest req) {
		// 기존 창고 조회
		List<WarehouseLocation> existing = warehouseLocationRepository.findAllByZoneAndRack(req.getZone(), req.getRack());
		
		// 기존 위치 Set 생성
		Set<String> existingSet = existing.stream()
				.map(d -> d.getRackRow() + "-" + d.getRackCol())
				.collect(Collectors.toSet());
		
		List<WarehouseLocation> toInsert = new ArrayList<>();
		
		for (int r = req.getRowStart(); r <= req.getRowEnd(); r++) {
			// r은 숫자로 들어오고 이걸 문자로 변환
			String rowChar = String.valueOf((char) ('A' + r - 1));
			
			for (int c = req.getColStart(); c <= req.getColEnd(); c++) {
				String colStr  = String.format("%02d", c);
				String key = rowChar + "-" + colStr;
				
				if (existingSet.contains(key)) {
					continue;
				}
				
				Long seq = warehouseLocationRepository.getNextLocationSeq();
				
				WarehouseLocationDTO locationDTO = new WarehouseLocationDTO();
				locationDTO.setLocationId(String.valueOf(seq));
				locationDTO.setZone(req.getZone());
				locationDTO.setRack(normalizeRack(req.getRack()));
				locationDTO.setRackRow(rowChar);
				locationDTO.setRackCol(colStr);
				locationDTO.setUseYn("Y");
				
				WarehouseLocation location = locationDTO.toEntity();
				
				toInsert.add(location);
			}
		}
		warehouseLocationRepository.saveAll(toInsert);
	}
	
	private String normalizeRack(String rack) {
	    return String.format("%02d", Integer.parseInt(rack));
	}

}
