package com.yeoun.inventory.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.yeoun.inbound.service.InboundService;
import com.yeoun.inventory.dto.MaterialOrderDTO;
import com.yeoun.inventory.dto.MaterialOrderItemDTO;
import com.yeoun.inventory.dto.SupplierDTO;
import com.yeoun.inventory.dto.SupplierItemDTO;
import com.yeoun.inventory.entity.MaterialOrder;
import com.yeoun.inventory.entity.MaterialOrderItem;
import com.yeoun.inventory.mapper.PurchaseMapper;
import com.yeoun.inventory.repository.MaterialOrderRepository;
import com.yeoun.inventory.util.InventoryIdUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class PurchaseService {
	private final PurchaseMapper purchaseMapper;
	private final MaterialOrderRepository materialOrderRepository;
	private final InboundService inboundService;

	// 공급 업체 조회
	public List<SupplierDTO> findAllSuppliers() {
		return purchaseMapper.findAllSuppliers();
	}

	// 발주 등록
	@Transactional
	public void savePurchaseOrder(SupplierDTO supplierDTO, String empId) {
		String date = LocalDate.now().toString().replace("-", "");
		String pattern = "MAT" + date + "-%";
		
		// 오늘 날짜의 최대 seq 조회
		String maxId = materialOrderRepository.findMaxOrderId(pattern);
		
		// 발주 아이디 생성
		String orderId = InventoryIdUtil.generateId(maxId, "MAT", date);
		
		List<MaterialOrderItemDTO> items = new ArrayList<>();
		
		// 발주 테이블에 들어갈 총 금액 계산
		int totalAmount = 0;
		
		// 공급업체의 공급 물품들 각각의 공급가액, 부가세 합계 계산
		for (SupplierItemDTO item : supplierDTO.getSupplierItemList()) {
			int supply = item.getOrderAmount() * item.getUnitPrice(); // 공급가액
			int vat = (int) (supply * 0.1); // 부가세
			int total = supply + vat;
			
			// 품목별 합계액을 더해서 전체 총 금액에 더함
			totalAmount += total;
			
			// 공급 물품 DTO 생성
			MaterialOrderItemDTO materialOrderItemDTO = MaterialOrderItemDTO.builder()
					.orderId(orderId)
					.itemId(item.getItemId()) 
					.orderAmount((long) item.getOrderAmount())
					.unitPrice((long) item.getUnitPrice())
					.VAT((long) vat)
					.totalPrice((long) total)
					.supplyAmount((long) supply)
					.build();
			
			items.add(materialOrderItemDTO);
		}
		
		// 공급 DTO 생성
		MaterialOrderDTO materialOrderDTO = MaterialOrderDTO.builder()
				.orderId(orderId)
				.clientId(supplierDTO.getClientId())
				.empId(empId)
				.dueDate(supplierDTO.getDueDate().toString())
				.totalAmount(String.valueOf(totalAmount))
				.items(items)
				.build();
		
		// 공급 DTO 엔터티로 변환
		MaterialOrder materialOrder = materialOrderDTO.toEntity();
		
		// 공급물품 DTO를 엔터티로 변환
		for (MaterialOrderItemDTO itemDTO : items) {
			MaterialOrderItem orderItem = itemDTO.toEntity();
			
			materialOrder.addItem(orderItem);
		}
		
		materialOrderRepository.save(materialOrder);
		
		// 발주 등록 후 입고 테이블 데이터 입력
		inboundService.saveInbound(materialOrder);
	}

	// 발주 상세 정보
	public MaterialOrderDTO getPurchaseOrder(String id) {
		return purchaseMapper.findPurchaseOrder(id);
	}
}
