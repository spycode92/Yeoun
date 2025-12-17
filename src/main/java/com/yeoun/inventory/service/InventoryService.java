package com.yeoun.inventory.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.yeoun.inventory.dto.InventoryModalRequestDTO;
import com.yeoun.inventory.dto.InventoryOrderCheckViewDTO;
import com.yeoun.inventory.dto.InventorySafetyCheckDTO;
import com.yeoun.inventory.dto.WarehouseLocationCreateRequest;
import com.yeoun.common.e_num.AlarmDestination;
import com.yeoun.common.entity.Dispose;
import com.yeoun.common.repository.DisposeRepository;
import com.yeoun.common.service.AlarmService;
import com.yeoun.inventory.dto.InventoryDTO;
import com.yeoun.inventory.dto.InventoryHistoryDTO;
import com.yeoun.inventory.dto.InventoryHistoryGroupDTO;
import com.yeoun.inventory.dto.WarehouseLocationDTO;
import com.yeoun.inventory.entity.Inventory;
import com.yeoun.inventory.entity.InventoryHistory;
import com.yeoun.inventory.entity.WarehouseLocation;
import com.yeoun.inventory.repository.InventoryHistoryRepository;
import com.yeoun.inventory.repository.InventoryOrderCheckViewRepository;
import com.yeoun.inventory.repository.InventoryRepository;
import com.yeoun.inventory.repository.WarehouseLocationRepository;
import com.yeoun.inventory.specification.InventorySpecs;
import com.yeoun.order.dto.WorkOrderDTO;
import com.yeoun.order.repository.WorkOrderRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class InventoryService {
	private final InventoryRepository inventoryRepository;
	private final InventoryHistoryRepository inventoryHistoryRepository;
	private final WarehouseLocationRepository warehouseLocationRepository;
	private final DisposeRepository disposeRepository;
	private final WorkOrderRepository workOrderRepository;
	private final InventoryOrderCheckViewRepository ivOrderCheckViewRepository;
	private final SimpMessagingTemplate messagingTemplate;
	private final AlarmService alarmService;
	
	// 검색조건을 통해 재고리스트 조회
	public List<InventoryDTO> getInventoryInfo(InventoryDTO inventoryDTO) {
		
		Specification<Inventory> spec =
		        InventorySpecs.lotNoContains(inventoryDTO.getLotNo());
		
		spec = Specification.allOf(
		        spec,
		        InventorySpecs.prodNameContains(inventoryDTO.getProdName()),
		        InventorySpecs.itemTypeEq(inventoryDTO.getItemType()),
		        InventorySpecs.zoneEq(inventoryDTO.getZone()),
		        InventorySpecs.rackEq(inventoryDTO.getRack()),
		        InventorySpecs.statusEq(inventoryDTO.getStatus()),
		        InventorySpecs.ibDateGoe(inventoryDTO.getIbDateFrom()),
		        InventorySpecs.ibDateLoe(inventoryDTO.getIbDateTo()),
		        InventorySpecs.expirationDateGoe(inventoryDTO.getExpDateFrom()),
		        InventorySpecs.expirationDateLoe(inventoryDTO.getExpDateTo())
		);
		
	    List<Inventory> list = inventoryRepository.findAll(spec);

	    return list.stream()
	               .map(InventoryDTO::fromEntity)
	               .toList();
	}
	
	//창고 로케이션 정보 불러오기 로직
	public List<WarehouseLocationDTO> getLocationInfo() {
		
		return warehouseLocationRepository.findAll().stream()
				.map(WarehouseLocationDTO::fromEntity)
				.toList();
	}
	
	// 재고 수량조절 로직
	@Transactional
	public void adjustQty(InventoryModalRequestDTO requestDTO, String empId) {
		Inventory inventory = inventoryRepository.findById(requestDTO.getIvId()).orElseThrow(() -> new EntityNotFoundException("존재하지않는 재고입니다."));
		
		Long prevInventoryQty = inventory.getIvAmount();
		Long changeInventoryQty = 0l;
		
		// 수량조절 유효성검사
		// 증가 감소 구분하여 변경할 Qty 값 설정
		if("INC".equals(requestDTO.getAdjustType())) {
			changeInventoryQty = prevInventoryQty + requestDTO.getAdjustQty();
			inventory.setIvAmount(changeInventoryQty);
		} else {
			changeInventoryQty = prevInventoryQty - requestDTO.getAdjustQty();
			// 변경후 수량이 0보다 작을경우
			if(changeInventoryQty < 0) {
				throw new IllegalArgumentException("변경 후 재고 수량이 0보다 작을 수 없습니다.");
			
			// 변경후 수량이 출고예정 수량보다 적은경우
			} else if(changeInventoryQty < inventory.getExpectObAmount()) {
				throw new IllegalArgumentException("변경 후 재고 수량이 출고예정 수량보다 작을 수 없습니다.");
				
			// 변경후 수량이 0인경우 재고삭제
			} else if(changeInventoryQty == 0l) {
				inventoryRepository.deleteById(inventory.getIvId());
			}
			// 변경수량 적용
			inventory.setIvAmount(changeInventoryQty);
		}
		
		// 재고내역 엔티티 작성
		InventoryHistory history = InventoryHistory.builder()
			.lotNo(inventory.getLotNo()) //재고의 lot번호
			.itemName(inventory.getItemName()) // 원자재 및 상품의 itemId(mat/prod)
			.prevWarehouseLocation(inventory.getWarehouseLocation())// 이전위치
			.currentWarehouseLocation(inventory.getWarehouseLocation()) // 현재위치
			.empId(empId) //작성자
			.workType(requestDTO.getAdjustType()) // 작업타입
			.prevAmount(prevInventoryQty) // 이전수량
			.currentAmount(changeInventoryQty) // 현재수량
			.moveAmount(0l)
			.reason(requestDTO.getReason()) // 이유
			.build();
		
		// 재고내역 등록
		inventoryHistoryRepository.save(history);
		
		// 재고 수량 조절 후 페이지 알림 
		String message = "재고 내 변동사항이 있습니다. 새로고침후 작업하십시오";
		alarmService.sendAlarmMessage(AlarmDestination.INVENTORY, message);
		
	}
	
	
	// 재고 이동 로직
	@Transactional
	public void moveInventory(InventoryModalRequestDTO requestDTO, String empId) {
		// 현재 이동해야할 재고 정보
		Inventory inventory = inventoryRepository.findById(requestDTO.getIvId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 재고입니다.")); 
		// 이동할 위치 
		WarehouseLocation location = warehouseLocationRepository.findById(requestDTO.getMoveLocationId().toString()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 위치입니다."));
		// 이동수량
		Long moveQty = requestDTO.getMoveAmount();
		// 이동요청 수량이 이동가능 수량보다 클때
	    Long availableQty = inventory.getIvAmount() - inventory.getExpectObAmount();
	    if (moveQty > availableQty) {
	        throw new IllegalArgumentException(
	            String.format("이동 불가능합니다. 현재 재고: %d, 출고예정: %d, 이동가능: %d, 요청수량: %d", 
	                inventory.getIvAmount(), inventory.getExpectObAmount(), availableQty, moveQty)
	        );
	    }
		// 이동요청 수량이 1보다 작을때
		if(moveQty < 1) {
	        throw new IllegalArgumentException("이동 수량은 1 이상이어야 합니다.");
		}
		
		// 이동위치에 이동하는 재고 lotNo 재고조회
		Optional<Inventory> existingInventoryOpt = inventoryRepository.findByWarehouseLocationAndLotNo(location, inventory.getLotNo());
		
		if(inventory.getIvAmount().equals(moveQty)) {
			// 전체 재고 이동시 기존 재고 정보 삭제
			inventoryRepository.delete(inventory);
		} else { 
			// 부분이동시 재고수량 감소
			Long beforeAmount = inventory.getIvAmount();
			inventory.setIvAmount(beforeAmount - moveQty);
		}
		
		if(existingInventoryOpt.isPresent()) {
			//이동위치에 재고가 있으면 수량 합치기
			Inventory existingInventory = existingInventoryOpt.get();
			existingInventory.setIvAmount(existingInventory.getIvAmount() + moveQty);
			
			inventoryRepository.save(existingInventory);
			// 이력생성
			InventoryHistory history = InventoryHistory.createFromMove(inventory, existingInventory, moveQty, empId);
			inventoryHistoryRepository.save(history);
		} else { // 이동위치에 재고가 없으면  
			// 이동한 재고 생성
			Inventory moveInventory = inventory.createMovedInventory(location, moveQty);
			inventoryRepository.save(moveInventory);
			// 이력생성
			InventoryHistory history = InventoryHistory.createFromMove(inventory, moveInventory, moveQty, empId);
			inventoryHistoryRepository.save(history);
		}
		
		// 재고 위치 이동후 
		String message = "재고 내 변동사항이 있습니다. 새로고침후 작업하십시오";
		alarmService.sendAlarmMessage(AlarmDestination.INVENTORY, message);
		
	}
	
	// 재고 폐기 처리 
	@Transactional
	public void disposeInventory(InventoryModalRequestDTO requestDTO, String empId) {
		// 폐기처리할 재고 정보
		Inventory inventory = inventoryRepository.findById(requestDTO.getIvId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 재고입니다."));
		// 폐기수량
		Long disposeQty = requestDTO.getDisposeAmount();
		// 이전 수량
		Long prevIvQty = inventory.getIvAmount();
		// 재고수량 - 출고예정수량
		Long expectIvQty =prevIvQty - inventory.getExpectObAmount();
		
		// 폐기후 재고 수량
		Long remainQty = prevIvQty - disposeQty;
		
		
		// 유효성검사
	    if (disposeQty > expectIvQty) {
	        throw new IllegalArgumentException(
	            String.format("폐기 불가능합니다. 현재 재고: %d, 출고예정: %d, 폐기가능: %d, 요청폐기: %d", 
	            		prevIvQty, inventory.getExpectObAmount(), expectIvQty, disposeQty)
	        );
	    }
		// 이동요청 수량이 1보다 작을때
		if(disposeQty < 1) {
	        throw new IllegalArgumentException("폐기 수량은 1 이상이어야 합니다.");
		}
		
		// 폐기수량이 재고의 전체 수량일 경우 재고 삭제
		if(disposeQty.equals(prevIvQty)) {
			inventoryRepository.delete(inventory);
		} else { // 폐기후 남는 수량이 있을 경우
			inventory.setIvAmount(remainQty);
		}
		
		// 재고내역 엔티티 작성
		InventoryHistory history = InventoryHistory.builder()
			.lotNo(inventory.getLotNo()) //재고의 lot번호
			.itemName(inventory.getItemName()) // 원자재 및 상품의 itemId(mat/prod)
			.prevWarehouseLocation(inventory.getWarehouseLocation())// 이전위치
			.currentWarehouseLocation(inventory.getWarehouseLocation()) // 현재위치
			.empId(empId) //작성자
			.workType("DISPOSE") // 작업타입
			.prevAmount(prevIvQty) // 이전수량
			.currentAmount(remainQty) // 현재수량
			.moveAmount(0l)
			.reason(requestDTO.getReason()) // 이유
			.build();
		
		// 재고내역 등록
		inventoryHistoryRepository.save(history);
		
		//폐기테이블에 추가
		Dispose dispose = Dispose.builder()
				.disposeAmount(disposeQty)
				.disposeReason(requestDTO.getReason())
				.empId(empId)
				.itemId(inventory.getItemId())
				.lotNo(inventory.getLotNo())
				.workType("INVENTORY")
				.build();
		
		disposeRepository.save(dispose);
		
		// 재고 폐기 후
		String message = "재고 내 변동사항이 있습니다. 새로고침후 작업하십시오";
		alarmService.sendAlarmMessage(AlarmDestination.INVENTORY, message);
	}
	
	
	// ------------------------------------------------------------------------
	// 재고내역 불러오기
	public List<InventoryHistoryDTO> getInventoryHistorys() {
		List<InventoryHistory> historyList = inventoryHistoryRepository.findAll();
		
		return historyList.stream().map(InventoryHistoryDTO::fromEntity).toList(); 
	}
	
	// 재고 등록(입고 시 사용)
	@Transactional
	public void registInventory(InventoryDTO inventoryDTO) {
		Inventory inventory = inventoryDTO.toEntity();
		inventoryRepository.save(inventory);
	}
	// 특정위치의 재고목록 불러오기
	public List<InventoryDTO> getlocationInventories(String locationId) {
		WarehouseLocation location = warehouseLocationRepository.findById(locationId).orElseThrow(() -> new EntityNotFoundException("존재하지않는 로케이션입니다.") ); 
		List<Inventory> inventoryList = inventoryRepository.findByWarehouseLocation(location);
		
		return inventoryList.stream().map(InventoryDTO::fromEntity).toList();
	}
	
	// 안전재고와 재고통계 조회
	public List<InventorySafetyCheckDTO> getIvSummary() {
		
		return inventoryRepository.getIvSummaryWithSafetyStock();
	}
	
	// 재고 이력 등록
	@Transactional
	public void registInventoryHistory(InventoryHistoryDTO inventoryHistoryDTO) {
		InventoryHistory inventoryHistory = inventoryHistoryDTO.toEntity();
		inventoryHistoryRepository.save(inventoryHistory);
	}
	
	// 재고 유통기한 체크
	@Transactional
	public void changeIvStatus() {
	    LocalDateTime today = LocalDateTime.now();
	    inventoryRepository.updateAllStatusByExpirationDate(today, today.plusDays(30));
	}
	
	
	// 입출고추이 차트를 그리기위한 데이터 조회(재고내역 그룹화)
	public List<InventoryHistoryGroupDTO> getIvHistoryGroupData(LocalDateTime now, LocalDateTime oneYearAgo) {

	    List<Object[]> rows = inventoryHistoryRepository.getIvHistoryGroupData(now, oneYearAgo);
	    
	    // Objectp[]로 조회결과를 받고 데이터타입을 맞춰서 dto생성
	    return rows.stream()
	        .map(r -> {
	            // TRUNC(created_date)
	            Object dateObj = r[0];
	            LocalDate createdDate;
	            if (dateObj instanceof java.sql.Timestamp ts) {
	                createdDate = ts.toLocalDateTime().toLocalDate();
	            } else if (dateObj instanceof java.sql.Date d) {
	                createdDate = d.toLocalDate();
	            } else {
	                throw new IllegalStateException("Unknown date type: " + dateObj.getClass());
	            }
	            
	            String workType = (String) r[1];
	            Long sumCurrent = ((Number) r[2]).longValue();
	            Long sumPrev    = ((Number) r[3]).longValue();
	            
	            InventoryHistoryGroupDTO dto = new InventoryHistoryGroupDTO();
	            dto.setCreatedDate(createdDate);
	            dto.setWorkType(workType);
	            dto.setSumCurrent(sumCurrent);
	            dto.setSumPrev(sumPrev);
	            return dto;
	        })
	        .toList();
	}

	// id로 재고 조회
	public Integer getTotalStock(String id) {
		return inventoryRepository.findAvailableStock(id);
	}
	
	// 작업지시서 목록 데이터 조회하기
	public List<WorkOrderDTO> getOrderData() {
		return workOrderRepository.findAll().stream()
				.map(WorkOrderDTO::fromEntity).toList();
	}
	
	// 발주위해 필요한데이터 조회(예상 재고수량, 생산계획필요수량, 작업지시서를 토대로 출고된 수량, 안전재고수량, 예상입고량)
	public List<InventoryOrderCheckViewDTO> getIvOrderCheckData() {
		
		return ivOrderCheckViewRepository.findAll().stream()
				.map(InventoryOrderCheckViewDTO::fromEntity).toList();
	}

	// ==================================
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
				
				WarehouseLocation location = locationDTO.toEntity();
				
				toInsert.add(location);
			}
		}
		warehouseLocationRepository.saveAll(toInsert);
	}
	
	private String normalizeRack(String rack) {
	    return String.format("%02d", Integer.parseInt(rack));
	}

	// zone 삭제
	@Transactional
	public void deleteLocationZone(String zoneName) {
		// zone으로 창고 조회
		List<WarehouseLocation> warehouseLocations = warehouseLocationRepository.findAllByZone(zoneName);
		
		if (warehouseLocations.isEmpty()) {
			 throw new IllegalStateException("존재하지 않는 Zone입니다.");
		}
		
		for (WarehouseLocation loc : warehouseLocations) {
			if (inventoryRepository.existsByWarehouseLocation_LocationId(loc.getLocationId())) {
				throw new IllegalStateException("재고가 존재하는 위치가 있어 삭제할 수 없습니다.");
			}
		}
		
		warehouseLocationRepository.deleteAllByZone(zoneName);
	}

	// rack 삭제
	@Transactional
	public void deleteLocationRack(String zone, String rack) {
		List<WarehouseLocation> warehouseLocations = warehouseLocationRepository.findAllByZoneAndRack(zone, rack);
		
		if (warehouseLocations.isEmpty()) {
			 throw new IllegalStateException("존재하지 않는 Rack 입니다.");
		}
		
		// 재고가 존재하는 location 이면 true가 반환
		boolean hasInventory = warehouseLocations.stream()
				.anyMatch(loc -> 
					inventoryRepository.existsByWarehouseLocation_LocationId(loc.getLocationId()));
		
		if (hasInventory) {
			throw new IllegalStateException("재고가 존재하는 위치가 있어 삭제할 수 없습니다.");
		}
		
		warehouseLocationRepository.deleteAllByZoneAndRack(zone, rack);
	}

}
