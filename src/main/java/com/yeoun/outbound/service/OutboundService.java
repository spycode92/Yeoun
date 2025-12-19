package com.yeoun.outbound.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import com.yeoun.common.e_num.AlarmDestination;
import com.yeoun.common.service.AlarmService;
import com.yeoun.inventory.dto.InventoryHistoryDTO;
import com.yeoun.inventory.entity.Inventory;
import com.yeoun.inventory.repository.InventoryRepository;
import com.yeoun.inventory.service.InventoryService;
import com.yeoun.inventory.util.InventoryIdUtil;
import com.yeoun.lot.dto.LotHistoryDTO;
import com.yeoun.lot.service.LotTraceService;
import com.yeoun.order.entity.WorkOrder;
import com.yeoun.order.repository.WorkOrderRepository;
import com.yeoun.outbound.dto.OutboundDTO;
import com.yeoun.outbound.dto.OutboundItemDTO;
import com.yeoun.outbound.dto.OutboundOrderDTO;
import com.yeoun.outbound.dto.OutboundOrderItemDTO;
import com.yeoun.outbound.entity.Outbound;
import com.yeoun.outbound.entity.OutboundItem;
import com.yeoun.outbound.mapper.OutboundMapper;
import com.yeoun.outbound.repository.OutboundItemRepository;
import com.yeoun.outbound.repository.OutboundRepository;
import com.yeoun.sales.dto.OrderDetailDTO;
import com.yeoun.sales.entity.Orders;
import com.yeoun.sales.entity.Shipment;
import com.yeoun.sales.enums.OrderStatus;
import com.yeoun.sales.enums.ShipmentStatus;
import com.yeoun.sales.repository.OrdersRepository;
import com.yeoun.sales.repository.ShipmentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class OutboundService {
	private final InventoryService inventoryService;
	private final LotTraceService lotTraceService;
	private final OutboundRepository outboundRepository;
	private final InventoryRepository inventoryRepository;
	private final WorkOrderRepository workOrderRepository;
	private final OutboundItemRepository outboundItemRepository;
	private final ShipmentRepository shipmentRepository;
	private final OrdersRepository ordersRepository;
	private final OutboundMapper outboundMapper;
	private final SimpMessagingTemplate messagingTemplate;
	private final AlarmService alarmService;
	
	// 출고 리스트 조회
	public List<OutboundOrderDTO> getOuboundList(LocalDateTime start, LocalDateTime end, String keyword) {
		return outboundMapper.findAllOutboundList(start, end, keyword);
	}

	// 출고 등록
	@Transactional
	public void saveOutbound(OutboundOrderDTO outboundOrderDTO, String empId) {
		String date = LocalDate.now().toString().replace("-", "");
		String pattern = "OUT" + date + "-%";
		
		// 오늘 날짜 기준 최대 seq 조회
		String maxId = outboundRepository.findMaxOrderId(pattern);
		
		// 출고 아이디 생성
		String outboundId = InventoryIdUtil.generateId(maxId, "OUT", date);
		
		String workOrderId = Optional.ofNullable(outboundOrderDTO.getWorkOrderId())
                					 .orElse(null);
		
		String shipmentId = Optional.ofNullable(outboundOrderDTO.getShipmentId())
                					.orElse(null);
		
		// 출고 DTO 생성
		OutboundDTO outboundDTO = OutboundDTO.builder()
				.outboundId(outboundId)
				.requestBy(outboundOrderDTO.getCreatedId())
				.workOrderId(workOrderId)
				.shipmentId(shipmentId)
				.status("WAITING")
				.expectOutboundDate(outboundOrderDTO.getStartDate())
				.build();
		
		// 출고 대상 itemId 수집
		Set<String> itemsId = outboundOrderDTO.getItems().stream()
				.map(item -> "MAT".equals(outboundOrderDTO.getType()) ? item.getMatId() : item.getPrdId())
				.collect(Collectors.toSet());
		
		// 재고 조회
		List<Inventory> inventories = inventoryRepository.findByItemIdAndIvStatusNot(itemsId, "EXPIRED");
		
		if (inventories.isEmpty()) {
			throw new IllegalArgumentException("재고가 없습니다.");
		}
		
		// itemId 기준으로 재고 그룹핑
		Map<String, List<Inventory>> inventoryMap =
				inventories.stream()
				.collect(Collectors.groupingBy(Inventory::getItemId));
		
		// 출고 품목 지정할 변수
		List<OutboundItemDTO> items = new ArrayList<>();
		
		// outboundOrderDTO에서 품목들 정보를 가져오기 위해 반복문 사용
		for (OutboundOrderItemDTO item : outboundOrderDTO.getItems()) {
			
			// MAT 타입이면 원재료 출고, FG이면 완제품 출고
			String itemId = "MAT".equals(outboundOrderDTO.getType())
					? item.getMatId()
					: item.getPrdId();
			
			// 출고 필요 수량
			Long requireQty = item.getOutboundQty();
			
			// 재고 조회
			 List<Inventory> stockList = inventoryMap.get(itemId);
			
			// FIFO 
			Long remaining = requireQty;
			
			for (Inventory stock : stockList) {
				if (remaining <= 0) break;
				
				// 기존 예정 수량
				Long currentExpect = stock.getExpectObAmount();
				// 실제 사용 가능한 수량
				Long canUse = stock.getIvAmount() - currentExpect;
				
				// 가용 재고가 없으면 다름 LOT로 넘어감
				if (canUse <= 0) continue;
				
				// LOT번호마다 사용 가능한 수량
				Long useQty = Math.min(canUse, remaining);
				
				// 기존 예정수량 + 새 예정 수량
				stock.setExpectObAmount(currentExpect + useQty);
				
				remaining -= useQty;
				
				// 출고품목 생성 로직
				OutboundItemDTO outboundItemDTO = OutboundItemDTO.builder()
						.outboundId(outboundId)
						.itemId(itemId)
						.lotNo(stock.getLotNo())
						.outboundAmount(useQty)
						.itemType(stock.getItemType())
						.ivId(stock.getIvId())
						.locationId(stock.getWarehouseLocation().getLocationId())
						.build();
				
				items.add(outboundItemDTO);
				
			}
			
			// 필요한 수량을 채우지 못한 경우 
			if (remaining > 0) {
				throw new IllegalArgumentException("재고 부족");
			}
		}
		
		// 출고 DTO를 엔터티로 변환
		Outbound outbound = outboundDTO.toEntity();
		
		// 출고 품목들을 엔터티로 변환
		for (OutboundItemDTO itemDTO : items) {
			OutboundItem outboundItem = itemDTO.toEntity();
			
			outbound.addItem(outboundItem);
		}
		
		// 출고 등록이 되면 출하지시서 상태 변경하기
		if (outboundOrderDTO.getType() != null && "FG".equals(outboundOrderDTO.getType())) {
			Shipment shipment = shipmentRepository.findByShipmentId(outboundOrderDTO.getShipmentId())
					.orElseThrow(() -> new NoSuchElementException("출하지시서를 찾을 수 없습니다."));
			
			// 출하지시 상태 변경
			shipment.changeStatus(ShipmentStatus.PENDING);
		}
    
		if ("MAT".equals(outboundOrderDTO.getType())) {
			WorkOrder workOrder = workOrderRepository.findByOrderId(workOrderId)
					.orElseThrow(() -> new NoSuchElementException("작업지시서를 찾을 수 없습니다."));
			
			workOrder.updateOutboundYn("P");
		}

	
		outboundRepository.save(outbound);
		
	}

	// 출고 상세 페이지
	public OutboundOrderDTO getMaterialOutbound(String outboundId) {
		return outboundMapper.findOutbound(outboundId);
	}
	
	// 완제품 출고 상세페이지
	public OutboundOrderDTO getProductOutbound(String outboundId) {
		return outboundMapper.findShipmentOutbound(outboundId);
	}

	// 출고 완료
	@Transactional
	public void updateOutbound(OutboundOrderDTO outboundOrderDTO, String empId) {
		// 출고 조회
		Outbound outbound = outboundRepository.findByOutboundId(outboundOrderDTO.getOutboundId())
				.orElseThrow(() -> new NoSuchElementException("출고 내역을 찾을 수 없습니다."));
		
		// 출고 담당자 등록
		outbound.registProcessBy(empId);
		// 출고일 등록
		outbound.registOutboundDate(LocalDateTime.now());
		
		// 출고 아이템 조회
		List<OutboundItem> items = outboundItemRepository.findByOutbound_OutboundId(outboundOrderDTO.getOutboundId());
		
		for (OutboundItem item : items) {
			// 재고 조회
			Inventory stock = inventoryRepository.findByIvId(item.getIvId())
					.orElseThrow(() -> new IllegalArgumentException("재고가 존재하지 않습니다."));
			
			// 사용 가능한 재고
			Long available = stock.getIvAmount(); 
			// 출고 수량
			Long outboundQty = item.getOutboundAmount();
			// 출고 예정 수량
			Long expectObAmount = stock.getExpectObAmount();
			
			if (available < outboundQty) {
				throw new IllegalArgumentException("재고 부족");
			}
			
			// 실제 재고 차감 
			stock.setIvAmount(available - outboundQty);
			stock.setExpectObAmount(expectObAmount - outboundQty);
			
			// 재고 이력 기록
			InventoryHistoryDTO inventoryHistoryDTO = InventoryHistoryDTO.builder()
					.empId(empId)
					.lotNo(item.getLotNo())
					.itemName(stock.getItemName())
					.workType("OUTBOUND")
					.prevAmount(available)
					.currentAmount(stock.getIvAmount())
					.reason(outboundOrderDTO.getOutboundId())
					.currentLocationId(stock.getWarehouseLocation().getLocationId())
					.build();
			
			inventoryService.registInventoryHistory(inventoryHistoryDTO);
			
			// ----------------------------------------
			// LOT 이력 남기기
			String eventType = "RM_ISSUE";
			String status = "ISSUED";
			String orderId = outboundOrderDTO.getWorkOrderId();
			
			if ("FG".equals(outboundOrderDTO.getType())) {
				
				eventType = "FG_SHIP";
				status = "SHIPPED";
				orderId = "";
			}
			// lotHistory 생성
			LotHistoryDTO historyDTO = LotHistoryDTO.builder()
					.lotNo(item.getLotNo())
					.orderId(orderId)
					.processId("")
					.eventType(eventType)
					.status(status)
					.locationType("WH")
					.locationId("WH-" + stock.getWarehouseLocation().getLocationId())
					.quantity(outboundQty.intValue())
					.workedId(empId)
					.build();
			
			lotTraceService.registLotHistory(historyDTO);
			
			// 재고 수량이 0이면 삭제
			if (stock.getIvAmount() == 0) {
				inventoryRepository.delete(stock);
			} else {
				inventoryRepository.save(stock);
			}
		}
		// 원재료 출고일 경우
		if ("MAT".equals(outboundOrderDTO.getType())) {
		WorkOrder workOrder = workOrderRepository.findByOrderId(outboundOrderDTO.getWorkOrderId())
				.orElseThrow(() -> new NoSuchElementException("작업지시 내역을 찾을 수 없습니다."));
		
			// 작업지시서의 출고여부 상태 업데이트
			workOrder.updateOutboundYn("Y");
		} else { // 완제품 출고일 경우
			Shipment shipment = shipmentRepository.findByShipmentId(outboundOrderDTO.getShipmentId())
					.orElseThrow(() -> new NoSuchElementException("출하지시서를 찾을 수 없습니다."));
			
			// 수주확인서 조회
			Orders orders = ordersRepository.findByOrderId(shipment.getOrderId())
					.orElseThrow(() -> new NoSuchElementException("수주 내역을 찾을 수 없습니다."));
			
			// 수주 상태값 변경(출하)
			orders.changeStatus(OrderStatus.SHIPPED);		
		}
		// 출고 상태 업데이트
		outbound.updateStatus("COMPLETED");
		
		// 모든 출고완료 처리 완료 후 각 페이지로 메세지 보내기
		if("FG".equals(outboundOrderDTO.getType())) {
			String message = "새로 등록된 상품 출고가 있습니다. 확인하십시오.";
			alarmService.sendAlarmMessage(AlarmDestination.INVENTORY, message);
			alarmService.sendAlarmMessage(AlarmDestination.SALES, message);
		} else {
			// 완제품이 아닌 입고일 경우
			String message = "새로 등록된 원자재 출고가 있습니다. 확인하십시오.";
			alarmService.sendAlarmMessage(AlarmDestination.INVENTORY, message);
			alarmService.sendAlarmMessage(AlarmDestination.ORDER, message);
		}
	}
	
	// 원자재 출고 등록 취소
	@Transactional
	public void canceledMaterialOutbound(String orderId) {
		// 출고 내역 조회
		Outbound outbound = outboundRepository.findByWorkOrderId(orderId)
				.orElseThrow(() -> new NoSuchElementException("출고 내역을 찾을 수 없습니다."));
		
		canceledOutbound(outbound);
	}
	
	// 완제품 출고 등록 취소
	@Transactional
	public void canceledProductOutbound(String shipmentId) {
		Outbound outbound = outboundRepository.findByShipmentId(shipmentId)
				.orElseThrow(() -> new NoSuchElementException("출하지시서를 찾을 수 없습니다."));
		
		// 출고 취소
		canceledOutbound(outbound);
	}
	
	private void canceledOutbound(Outbound outbound) {
		// 이미 취소된 상태라면 로직 중단
	    if ("CANCELED".equals(outbound.getStatus())) {
	        throw new IllegalStateException("이미 취소된 출고 내역입니다.");
	    }
		
		for (OutboundItem item : outbound.getItems()) {
			// 재고 조회
			Inventory inventory = inventoryRepository.findByIvId(item.getIvId())
					.orElseThrow(() -> new NoSuchElementException("재고를 찾을 수 없습니다."));
			
			// 출고 예정 수량
			long expectObAmount = inventory.getExpectObAmount();
			// 출고 수량
			long outboundAmount = item.getOutboundAmount();
			
			long result = expectObAmount -outboundAmount;
			
			// 출고 예정수량 변경(음수로 내려가지 않도록 Math.max 사용)
			inventory.setExpectObAmount(Math.max(0, result));
		}
		
		// 출고 상태 변경(취소)
		outbound.updateStatus("CANCELED");
	}
	
	// ===========================================================================
	// 출하지시서 목록 조회 (추후 출하지시 관련된 작업으로 옮길 예정)
	public List<OutboundOrderDTO> getShipmentList() {
		return outboundMapper.findAllShipment();
	}

	// 출하지지서 상세 조회
	public OrderDetailDTO getShipmentDetail(String shipmentId) {
		return outboundMapper.findShipment(shipmentId);
	}

}
