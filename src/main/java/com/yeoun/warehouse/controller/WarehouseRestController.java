package com.yeoun.warehouse.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yeoun.inventory.dto.WarehouseLocationDTO;
import com.yeoun.warehouse.dto.WarehouseLocationCreateRequest;
import com.yeoun.warehouse.service.WarehouseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/api/warehouse")
@Log4j2
@RequiredArgsConstructor
public class WarehouseRestController {
	
	private final WarehouseService warehouseService;
	
	// 창고 정보 조회 (전체 조회)
	@GetMapping("/locations")
	public ResponseEntity<List<WarehouseLocationDTO>> locations() {
		
		List<WarehouseLocationDTO> locationList = warehouseService.getAllLocations();
		
		return ResponseEntity.ok(locationList);
		
	}
	
	// 창고 상태 변경 
	@PostMapping("/locations/{locationId}")
	public ResponseEntity<?> modifyLocationStatus(@PathVariable("locationId") String locationId, @RequestBody Map<String, String> request) {
		try {
			String useYn = request.get("useYn");
			warehouseService.updateLocationStatus(locationId, useYn);
			return ResponseEntity.ok().build();
		} catch (IllegalStateException e) {
	        return ResponseEntity
	                .status(HttpStatus.CONFLICT)
	                .body(Map.of("message", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "서버 오류가 발생했습니다."));
		}
	}
	
	// 창고 등록
	@PostMapping("/locations/add")
	public ResponseEntity<String> createLocations(@RequestBody WarehouseLocationCreateRequest req) {
		warehouseService.createLocations(req);
		
		 return ResponseEntity.ok().build();
	}
}
