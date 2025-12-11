package com.yeoun.inbound.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.yeoun.common.dto.DisposeDTO;
import com.yeoun.common.e_num.AlarmDestination;
import com.yeoun.common.service.AlarmService;
import com.yeoun.common.service.DisposeService;
import com.yeoun.inbound.dto.InboundDTO;
import com.yeoun.inbound.dto.InboundItemDTO;
import com.yeoun.inbound.dto.ReceiptDTO;
import com.yeoun.inbound.dto.ReceiptItemDTO;
import com.yeoun.inbound.entity.Inbound;
import com.yeoun.inbound.entity.InboundItem;
import com.yeoun.inbound.mapper.InboundMapper;
import com.yeoun.inbound.repository.InboundItemRepository;
import com.yeoun.inbound.repository.InboundRepository;
import com.yeoun.inventory.dto.InventoryDTO;
import com.yeoun.inventory.dto.InventoryHistoryDTO;
import com.yeoun.inventory.entity.MaterialOrder;
import com.yeoun.inventory.entity.MaterialOrderItem;
import com.yeoun.inventory.repository.MaterialOrderRepository;
import com.yeoun.inventory.service.InventoryService;
import com.yeoun.inventory.util.InventoryIdUtil;
import com.yeoun.lot.dto.LotHistoryDTO;
import com.yeoun.lot.dto.LotMasterDTO;
import com.yeoun.lot.service.LotTraceService;
import com.yeoun.masterData.entity.MaterialMst;
import com.yeoun.masterData.entity.ProductMst;
import com.yeoun.masterData.repository.MaterialMstRepository;
import com.yeoun.masterData.repository.ProductMstRepository;
import com.yeoun.order.entity.WorkOrder;
import com.yeoun.order.repository.WorkOrderRepository;
import com.yeoun.process.entity.WorkOrderProcess;
import com.yeoun.process.repository.WorkOrderProcessRepository;
import com.yeoun.sales.entity.ClientItem;
import com.yeoun.sales.repository.ClientItemRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class InboundService {
	private final LotTraceService lotTraceService;
	private final InventoryService inventoryService;
	private final DisposeService disposeService;
	private final InboundRepository inboundRepository;
	private final InboundItemRepository inboundItemRepository;
	private final ClientItemRepository clientItemRepository;
	private final MaterialMstRepository materialMstRepository;
	private final MaterialOrderRepository materialOrderRepository;
	private final WorkOrderProcessRepository workOrderProcessRepository;
	private final WorkOrderRepository workOrderRepository;
	private final ProductMstRepository productMstRepository;
	private final InboundMapper inboundMapper;
	private final SimpMessagingTemplate messagingTemplate;
	private final AlarmService alarmService;
	
	// 입고대기 등록
	@Transactional
	public void saveInbound(MaterialOrder materialOrder) {
		String date = LocalDate.now().toString().replace("-", "");
		String pattern = "INB" + date + "-%";
		
		// 오늘 날짜 기준 최대 seq 조회
		String maxId = inboundRepository.findMaxOrderId(pattern);
		
		// 입고 아이디 생성
		String inboundId = InventoryIdUtil.generateId(maxId, "INB", date);
		
		// 입고 품목 저장할 변수
		List<InboundItemDTO> items = new ArrayList<>();
		
		// 원재료의 유효기간(유통기한) 정보
		LocalDate datePlus = LocalDate.parse(materialOrder.getDueDate()).plusMonths(36);
		
		// 파라미터로 전달받은 발주 엔터티에서 발주 품목들 반복문으로 DTO로 변환
		for (MaterialOrderItem item : materialOrder.getItems()) {
			
			// 공급 품목 조회
			ClientItem clientItem = clientItemRepository.findByItemId(item.getItemId())
					.orElseThrow(() -> new NoSuchElementException("해당 품목 정보를 찾을 수 없습니다."));
			// 원자재 조회
			MaterialMst materialMst = materialMstRepository.findByMatId(clientItem.getMaterialId())
					.orElseThrow(() -> new NoSuchElementException("해당 원재료 정보를 찾을 수 없습니다."));
			
			// 입고대기 품목 생성
			InboundItemDTO inboundItemDTO = InboundItemDTO.builder()
					.inboundId(inboundId)
					.itemId(clientItem.getMaterialId())
					.requestAmount(item.getOrderAmount())
					.inboundAmount(0L)
					.disposeAmount(0L)
					.itemType(materialMst.getMatType())
					.locationId(null)
					.manufactureDate(LocalDate.parse(materialOrder.getDueDate()).atStartOfDay())
					.expirationDate(datePlus.atStartOfDay())
					.build();

			items.add(inboundItemDTO);
		}
		
		// 입고 DTO 생성
		InboundDTO inboundDTO = InboundDTO.builder()
				.inboundId(inboundId)
				.expectArrivalDate(LocalDate.parse(materialOrder.getDueDate()).atStartOfDay())
				.inboundStatus("PENDING_ARRIVAL")
				.materialId(materialOrder.getOrderId())
				.prodId(null)
				.items(items)
				.build();
		
		// 입고 DTO를 엔터티로 변환
		Inbound inbound = inboundDTO.toEntity();
		
		// 입고 품목들을 엔터티로 변환
		for (InboundItemDTO itemDTO : items) {
			InboundItem inboundItem = itemDTO.toEntity();
			
			inbound.addItem(inboundItem);
		}
		
		inboundRepository.save(inbound);
	}
	
	// 완제품 입고 대기
	@Transactional
	public void saveProductInbound(String wopId) {
		// 작업지시 공정 정보 가져오기
		WorkOrderProcess workOrderProcess = workOrderProcessRepository.findByWopId(wopId)
				.orElseThrow(() -> new NoSuchElementException("해당 공정 정보를 찾을 수 없습니다."));
		// 작업지시ID
		String orderId = workOrderProcess.getWorkOrder().getOrderId();
		
		// 작업지시 정보 조회
		WorkOrder workOrder = workOrderRepository.findByOrderId(orderId)
				.orElseThrow(() -> new NoSuchElementException("해당 작업지시 정보를 찾을 수 없습니다."));
		
		// 제품ID
		String prdId = workOrder.getProduct().getPrdId();
		
		// 제품 정보 조회
		ProductMst productMst = productMstRepository.findByPrdId(prdId)
				.orElseThrow(() -> new NoSuchElementException("해당 제품 정보를 찾을 수 없습니다."));
		
		// 입고 생성		
		String date = LocalDate.now().toString().replace("-", "");
		String pattern = "INB" + date + "-%";
		
		// 오늘 날짜 기준 최대 seq 조회
		String maxId = inboundRepository.findMaxOrderId(pattern);
		
		// 입고 아이디 생성
		String inboundId = InventoryIdUtil.generateId(maxId, "INB", date);
		
		InboundDTO inboundDTO = InboundDTO.builder()
				.inboundId(inboundId)
				.expectArrivalDate(workOrderProcess.getEndTime())
				.inboundStatus("PENDING_ARRIVAL")
				.materialId(null)
				.prodId(orderId)
				.build();
		
		// DTO -> Entity 변환
		Inbound inbound = inboundDTO.toEntity();
		
		// 입고 등록
		inboundRepository.save(inbound);
		
		// -------------------------------------
		// 입고 품목 등록
		// 제품 기준정보의 유효일자(개월) 가져오기
		int valiDays = productMst.getEffectiveDate();
		
		InboundItemDTO inboundItemDTO = InboundItemDTO.builder()
				.lotNo(workOrderProcess.getLotNo())
				.inboundId(inboundId)
				.itemId(prdId)
				.requestAmount((long) workOrderProcess.getGoodQty())
				.inboundAmount(0L)
				.disposeAmount(0L)
				.manufactureDate(workOrderProcess.getEndTime())
				.expirationDate(workOrderProcess.getEndTime().plusMonths(valiDays))
				.itemType("FG")
				.locationId(null)
				.build();
		
		InboundItem inboundItem = inboundItemDTO.toEntity();
		
		inbound.addItem(inboundItem);
		
		inboundRepository.save(inbound);
	}

	// 원재료 목록 데이터(날짜 지정과 검색 기능 포함)
	public List<ReceiptDTO> getMaterialInboundList(LocalDateTime startDate, LocalDateTime endDate, String searchType, String keyword) {
		return inboundMapper.findAllMaterialInbound(startDate, endDate, searchType, keyword);
	}

	// 입고 상세 조회
	public ReceiptDTO getMaterialInbound(String inboundId) {
		return inboundMapper.findInbound(inboundId);
	}

	// 입고완료 처리(원재료)
	@Transactional
	public void updateInbound(ReceiptDTO receiptDTO, String empId) {
		// 입고 조회
		Inbound inbound = inboundRepository.findByinboundId(receiptDTO.getInboundId())
				.orElseThrow(() -> new NoSuchElementException("입고 내역을 찾을 수 없습니다."));
		
		// 원자재 입고완료 처리시에만 시행
		if(inbound.getMaterialId() != null && !inbound.getMaterialId().isEmpty()) {
			// 발주 조회
			MaterialOrder materialOrder = materialOrderRepository.findByOrderId(inbound.getMaterialId())
					.orElseThrow(() -> new NoSuchElementException("발주 내역을 찾을 수 없습니다."));
			
			// 발주 상태를 완료로 변경
			materialOrder.changeStatus("COMPLETED");
		}
		
		// 입고담당자 등록
		inbound.registEmpId(empId);
		
		// 입고 상태를 완료로 변경
		inbound.changeStatus("COMPLETED");
		
		Map<Long, InboundItem> inboundItemMap = inboundItemRepository
				.findAllByInbound_InboundId(receiptDTO.getInboundId())
				.stream()
				.collect(Collectors.toMap(InboundItem::getInboundItemId, item -> item));
		
		// 입고하는 재고의 타입을 확인하기위해 변수설정
		String inboundItemType = "";
		
		// 반복문 통해서 입고 품목 LOT 생성 및 수량 정보 업데이트
		for (ReceiptItemDTO itemDTO : receiptDTO.getItems()) {
			
			InboundItem inboundItem = inboundItemMap.get(itemDTO.getInboundItemId());
			
			if (inboundItem == null) {
				throw new NoSuchElementException("입고 품목을 찾을 수 없습니다.");
			}
			
			Integer qty = itemDTO.getInboundAmount().intValue();
			
			// ----------------------------------------------------------------
			// LotMasterDTO 생성
			
			String lotNo = "";
			// 로트번호가 존재하지 않을때 만 실행
			if(itemDTO.getLotNo() == null || itemDTO.getLotNo().isEmpty()) {
				// LOT 생성
				LotMasterDTO lotMasterDTO = LotMasterDTO.builder()
						.lotType(itemDTO.getItemType())
						.prdId(itemDTO.getItemId())
						.quantity(qty)
						.currentStatus("NEW")
						.currentLocType("WH")
						.currentLocId("WH" + itemDTO.getLocationId())
						.statusChangeDate(LocalDateTime.now())
						.build();
				
				// LOT 생성 및 LOT번호 반환
				lotNo = lotTraceService.registLotMaster(lotMasterDTO, "00");
				
				// lotHistory 생성
				LotHistoryDTO createLotHistoryDTO = LotHistoryDTO.builder()
						.lotNo(lotNo)
						.orderId("")
						.processId("")
						.eventType("CREATE")
						.status("NEW")
						.locationType("WH")
						.locationId("WH-" + itemDTO.getLocationId())
						.quantity(qty)
						.workedId(empId)
						.build();
				lotTraceService.registLotHistory(createLotHistoryDTO);
			} else {
				// 완제품은 lotNo가 정해져있음
				lotNo = itemDTO.getLotNo();
				// 완제품은 LotMst, LotHistory 생성 등록 필요없음
			}
				
			// InboundItem 업데이트
			inboundItem.updateInfo(lotNo, itemDTO.getInboundAmount(), itemDTO.getDisposeAmount(), itemDTO.getLocationId());
				
			
			// ------------------------------------------------------
			// 재고 등록
			InventoryDTO inventoryDTO = InventoryDTO.builder()
					.lotNo(lotNo)
					.locationId(itemDTO.getLocationId())
					.itemId(itemDTO.getItemId())
					.ivAmount(itemDTO.getInboundAmount())
					.expirationDate(inboundItem.getExpirationDate())
					.manufactureDate(inboundItem.getManufactureDate())
					.ibDate(LocalDateTime.now())
					.ivStatus("NORMAL")
					.expectObAmount(0L)
					.itemType(itemDTO.getItemType())
					.build();
			// 재고등록시 등록되는 itemType을 설정(원자재, 완제품이 동시에 입고되지않음)
			inboundItemType = inventoryDTO.getItemType();
			
			inventoryService.registInventory(inventoryDTO);
			
			// 재고이력 등록
			InventoryHistoryDTO inventoryHistoryDTO = InventoryHistoryDTO.builder()
					.lotNo(lotNo)
					.itemName(itemDTO.getItemName())
					.empId(empId)
					.workType("INBOUND")
					.prevAmount(0L)
					.currentAmount(itemDTO.getInboundAmount())
					.reason(receiptDTO.getInboundId())
					.currentLocationId(itemDTO.getLocationId())
					.build();
			
			inventoryService.registInventoryHistory(inventoryHistoryDTO);
			
			// ---------------------------------------------
			// 재고 등록 후 LOT HISTORY 업데이트
			// lotHistory 생성
			// eventType 설정 : RM_RECEIVE / FG_INBOUND
			String eventType = "RM_RECEIVE";
			if ("FG".equals(itemDTO.getItemType())) {
				eventType = "FG_INBOUND";
			}
			
			LotHistoryDTO updateLotHistoryDTO = LotHistoryDTO.builder()
					.lotNo(lotNo)
					.orderId("")
					.processId("")
					.eventType(eventType)
					.status("IN_STOCK")
					.locationType("WH")
					.locationId("WH-" + itemDTO.getLocationId())
					.quantity(qty)
					.workedId(empId)
					.build();
			
			lotTraceService.registLotHistory(updateLotHistoryDTO);
			// ------------------------------------------------------
			
			if (itemDTO.getDisposeAmount() > 0) {
				DisposeDTO dispose = DisposeDTO.builder()
						.lotNo(lotNo)
						.itemId(itemDTO.getItemId())
						.workType("INBOUND")
						.empId(empId)
						.disposeAmount(itemDTO.getDisposeAmount())
						.disposeReason("입고폐기")
						.build();
				
				disposeService.registDispose(dispose);
				
				Integer disposeQty = itemDTO.getDisposeAmount().intValue();
				
				// 폐기한 원재료 LOT 이력에 업데이트
				LotHistoryDTO disposeLotHistoryDTO = LotHistoryDTO.builder()
						.lotNo(lotNo)
						.orderId("")
						.processId("")
						.eventType("SCRAPPED")
						.status("SCRAPPED")
						.locationType("WH")
						.locationId("WH-" + itemDTO.getLocationId())
						.quantity(disposeQty)
						.workedId(empId)
						.build();
				
				lotTraceService.registLotHistory(disposeLotHistoryDTO);
			}
		}
		// 모든 입고완료 처리 완료 후 각 페이지로 메세지 보내기
		// 완제품 입고의 경우
		if("FG".equals(inboundItemType)) {
			String message = "새로 등록된 상품 입고가 있습니다.";
			alarmService.sendAlarmMessage(AlarmDestination.INVENTORY, message);
			alarmService.sendAlarmMessage(AlarmDestination.SALES, message);
		} else {
			// 완제품이 아닌 입고일 경우
			String message = "새로 등록된 원자재 입고가 있습니다.";
			alarmService.sendAlarmMessage(AlarmDestination.INVENTORY, message);
			alarmService.sendAlarmMessage(AlarmDestination.ORDER, message);
		}
		
	}
}
