package com.yeoun.inventory.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.inventory.dto.MaterialOrderDTO;
import com.yeoun.inventory.dto.SupplierDTO;
import com.yeoun.inventory.service.PurchaseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Controller
@RequestMapping("/purchase")
@RequiredArgsConstructor
@Log4j2
public class PurchaseController {
	private final PurchaseService purchaseService;
	
	@GetMapping("/purchaseOrder")
	public String purchaseOrderList() {
		return "/purchase/purchase_list";
	}
	
	// 공급업체 데이터 조회
	@GetMapping("/supplier/data")
	public ResponseEntity<?> supplierList() {
		List<SupplierDTO> supplierList = purchaseService.findAllSuppliers();
		
		return ResponseEntity.ok(supplierList);
	}
	
	// 발주 등록
	@PostMapping("/purchaseOrder")
	public ResponseEntity<Map<String, String>> registPurchaseOrder(@RequestBody SupplierDTO supplierDTO, @AuthenticationPrincipal LoginDTO loginDTO) {
		try {
			purchaseService.savePurchaseOrder(supplierDTO, loginDTO.getEmpId());
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Map.of("message", "등록 완료"));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(Map.of("message", e.getMessage()));
		}
	}
	
	// 발주 상세정보
	@GetMapping("/detail/{id}")
	public String purchaseOrderDetail(@PathVariable("id") String id, Model model) {
		MaterialOrderDTO materialOrderDTO  = purchaseService.getPurchaseOrder(id);
		
		model.addAttribute("materialOrderDTO", materialOrderDTO);
		
		return "purchase/purchase_info";
	}
}  
