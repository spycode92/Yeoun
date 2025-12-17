package com.yeoun.inventory.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.inbound.dto.InboundDTO;
import com.yeoun.inbound.dto.ReceiptDTO;
import com.yeoun.inventory.dto.InventoryModalRequestDTO;
import com.yeoun.inventory.dto.InventoryOrderCheckViewDTO;
import com.yeoun.inventory.dto.InventoryDTO;
import com.yeoun.inventory.dto.InventoryHistoryDTO;
import com.yeoun.inventory.dto.InventoryHistoryGroupDTO;
import com.yeoun.inventory.dto.InventoryHistorySearchDTO;
import com.yeoun.inventory.dto.WarehouseLocationDTO;
import com.yeoun.inventory.dto.InventorySafetyCheckDTO;
import com.yeoun.inventory.dto.WarehouseLocationCreateRequest;
import com.yeoun.inventory.entity.WarehouseLocation;
import com.yeoun.inventory.service.InventoryService;
import com.yeoun.order.dto.WorkOrderDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;


@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/inventories")
public class InventoryRestController {
	private final InventoryService inventoryService;
	
	// 재고리스트 조회
	@PostMapping("")
	public ResponseEntity<List<InventoryDTO>> inventories(@RequestBody(required = false) InventoryDTO inventoryDTO) {
//				System.out.println(inventoryDTO);
		List<InventoryDTO> inventoryDTOList = 
				inventoryService.getInventoryInfo(inventoryDTO != null ? inventoryDTO : new InventoryDTO());
		
		return ResponseEntity.ok(inventoryDTOList);
	}
	
	//창고 정보 조회
	@GetMapping("/locations")
	public ResponseEntity<List<WarehouseLocationDTO>> locations() {
		
		List<WarehouseLocationDTO> locationDTOList = inventoryService.getLocationInfo();
		
		return ResponseEntity.ok(locationDTOList);
	}
	
	//수량조절
	@PostMapping("/{ivId}/adjustQty")
	public ResponseEntity<Map<String, String>> adjustQty(
			@PathVariable("ivId") Long ivId, @RequestBody InventoryModalRequestDTO requestDTO,
			@AuthenticationPrincipal LoginDTO loginUser) {
		Map result = new HashMap<String, String>();
		String empId = loginUser.getEmpId();
		requestDTO.setIvId(ivId);
//		log.info("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ :" + requestDTO);
		inventoryService.adjustQty(requestDTO, empId);
		
		return ResponseEntity.ok(result);
	}
	
	//재고이동
	@PostMapping("/{ivId}/move")
	public ResponseEntity<Map<String, String>> moveInventory(
			@PathVariable("ivId") Long ivId, @RequestBody InventoryModalRequestDTO requestDTO,
			@AuthenticationPrincipal LoginDTO loginUser) {
		Map result = new HashMap<String, String>();
		String empId = loginUser.getEmpId();
		requestDTO.setIvId(ivId);
		
		inventoryService.moveInventory(requestDTO, empId);
		
		return ResponseEntity.ok(result);
	}
	
	//재고폐기
	@PostMapping("/{ivId}/dispose")
	public ResponseEntity<Map<String, String>> disposeInventory(
			@PathVariable("ivId") Long ivId, @RequestBody InventoryModalRequestDTO requestDTO,
			@AuthenticationPrincipal LoginDTO loginUser) {
		Map result = new HashMap<String, String>();
		String empId = loginUser.getEmpId();
		requestDTO.setIvId(ivId);
		
		inventoryService.disposeInventory(requestDTO, empId);
		
		return ResponseEntity.ok(result);
	}
	
	//-----------------------------------------------------------------------------
	// 재고이력 정보
	@PostMapping("/historys")
	public ResponseEntity<List<InventoryHistoryDTO>> historys(@RequestBody InventoryHistorySearchDTO condition) {
		
		List<InventoryHistoryDTO> historyDTOList = inventoryService.getInventoryHistorys(condition);
		
		return ResponseEntity.ok(historyDTOList);
	}
	
	//-----------------------------------------------------------------------------
	// 재고실사 - 위치의 재고정보 불러오기
	@GetMapping("/{locationId}")
	public ResponseEntity<List<InventoryDTO>> locationInventories(@PathVariable("locationId") String locationId) {
		
		List<InventoryDTO> inventoryDTOList = inventoryService.getlocationInventories(locationId);
		
		return ResponseEntity.ok(inventoryDTOList);
	}
	
	// ----------------------------------------------------------------------------
	// 대시보드
	
	// 상품별 재고정보 조회
	@GetMapping("/inventorySafetyStockCheckInfo")
	public ResponseEntity<List<InventorySafetyCheckDTO>> getIvSummary() {
		List<InventorySafetyCheckDTO> ivSummaryList = inventoryService.getIvSummary();
		return ResponseEntity.ok(ivSummaryList);
	}
	
	
	// 입출고 내역 데이터 조회
	@GetMapping("/ivHistoryGroup")
	public ResponseEntity<IvHistoryGroupResponse > getIvHistoryGroup() {
	    LocalDateTime now = LocalDateTime.now();
	    LocalDateTime oneYearAgo = now.minusYears(1);
	    
		List<InventoryHistoryGroupDTO> ivHistoryGroupList = inventoryService.getIvHistoryGroupData(now, oneYearAgo);
		
		return ResponseEntity.ok(new IvHistoryGroupResponse(
            oneYearAgo.toLocalDate().toString(),
            now.toLocalDate().toString(),
            ivHistoryGroupList
		));
	}
	// 데이터 보내기위해 묶음 설정
	public record IvHistoryGroupResponse(
			String startDate,
			String endDate,
			List<InventoryHistoryGroupDTO> data
			) {}
	
	
	
	// 원재료id로 재고 수량 조회
	@GetMapping("/stock/{id}")
	public ResponseEntity<Map<String, Integer>> getIvStock(@PathVariable("id") String id) {
		Integer stock = inventoryService.getTotalStock(id);
		
		return ResponseEntity.ok(Map.of("stock", stock));
	}
	
	
	// 원재료id로 재고 수량 조회
	@GetMapping("/orderData")
	public ResponseEntity<List<WorkOrderDTO>> getOrderData() {
		List<WorkOrderDTO> workOrderDTOList = inventoryService.getOrderData();
		
		return ResponseEntity.ok(workOrderDTOList);
	}
	
	// 재고 발주 체크 데이터
	@GetMapping("/inventoryOrderCheck")
	public ResponseEntity<List<InventoryOrderCheckViewDTO>> getIv() {
		List<InventoryOrderCheckViewDTO> inventoryOrderCheckDTOList = inventoryService.getIvOrderCheckData();
		System.out.println(inventoryOrderCheckDTOList);
		
		return ResponseEntity.ok(inventoryOrderCheckDTOList);
	}
	
	// =========================================
	// 창고 등록
//	@PostMapping("/locations/add")
//	public ResponseEntity<String> createLocations(@RequestBody WarehouseLocationCreateRequest req) {
//		inventoryService.createLocations(req);
//		
//		 return ResponseEntity.ok().build();
//	}
	
	// zone 삭제
//	@DeleteMapping("/zones/{zoneName}")
//	public ResponseEntity<?> deleteZone(@PathVariable("zoneName") String zoneName) {
//		try {
//			inventoryService.deleteLocationZone(zoneName);
//			return ResponseEntity.ok().build();
//		} catch (IllegalStateException e) {
//			return ResponseEntity
//					.status(HttpStatus.CONFLICT)
//					.body(e.getMessage());
//		} catch (Exception e) {
//			return ResponseEntity
//					.status(HttpStatus.INTERNAL_SERVER_ERROR)
//					.body("서버 오류가 발생했습니다.");
//		}
//	}
	
	// rack 삭제
//	@DeleteMapping("/racks")
//	public ResponseEntity<?> deleteRack(@RequestParam(name = "zone") String zone, @RequestParam(name = "rack") String rack) {
//		try {
//			inventoryService.deleteLocationRack(zone, rack);
//			return ResponseEntity.ok().build();
//		} catch (IllegalStateException e) {
//			return ResponseEntity
//					.status(HttpStatus.CONFLICT)
//					.body(e.getMessage());
//		} catch (Exception e) {
//			return ResponseEntity
//					.status(HttpStatus.INTERNAL_SERVER_ERROR)
//					.body("서버 오류가 발생했습니다.");
//		}
//	}
}



