package com.yeoun.inbound.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.inbound.dto.InboundDTO;
import com.yeoun.inbound.dto.ReceiptDTO;
import com.yeoun.inbound.service.InboundService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Controller
@RequestMapping("/inventory/inbound")
@RequiredArgsConstructor
@Log4j2
public class InboundController {
	private final InboundService inboundService;

	// 입고관리 페이지
	@GetMapping("")
	public String inboundList(Model model) {
		// 탭 활성화를 위한 정보
		model.addAttribute("activeTab", "mat");
		return "inbound/inbound_list";
	}
	
	// 원재료 페이지
	@GetMapping("/materialList")
	public String material(Model model) {
		// 탭 활성화를 위한 정보
		model.addAttribute("activeTab", "mat");
		return "inbound/inbound_list";
	}
	
	// 재입고 페이지
	@GetMapping("/reInboundList")
	public String reMaterial(Model model) {
		// 탭 활성화를 위한 정보
		model.addAttribute("activeTab", "re");
		return "inbound/inbound_list";
	}
	
	// 완제품 페이지
	@GetMapping("/productList")
	public String product(Model model) {
		// 탭 활성화를 위한 정보
		model.addAttribute("activeTab", "prd");
		return "inbound/inbound_list";
	}
	
	// 입고상세정보 - 작업지시서 새창
	@GetMapping("/detail/prodWin/{id}")
	public String product(Model model, @PathVariable("id") String id) {
		// 탭 활성화를 위한 정보
		model.addAttribute("id", id);
		return "inbound/workorder_window";
	}
	
	// 원재료 목록 데이터(날짜 지정과 검색 기능 포함)
	@GetMapping("/materialList/data")
	@ResponseBody
	public ResponseEntity<List<ReceiptDTO>> materialList(
			@RequestParam(required = false, name = "startDate") String startDate, 
			@RequestParam(required = false, name = "endDate") String endDate,
			@RequestParam(required = false, name = "searchType") String searchType,
			@RequestParam(required = false, name = "keyword") String keyword) {
		
		// 문자열로 들어온 날짜 데이터를 변환
		LocalDateTime start = (startDate != null) ? LocalDate.parse(startDate).atStartOfDay() : LocalDate.now().withDayOfMonth(1).atStartOfDay();
		LocalDateTime end = (endDate != null) ? LocalDate.parse(endDate).atTime(23, 59, 59) : LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).atTime(23, 59, 59);
		
		List<ReceiptDTO> inboundList = inboundService.getMaterialInboundList(start, end, searchType, keyword);
		
		return ResponseEntity.ok(inboundList);
	}
	
	// 원재료 입고 상세페이지
	@GetMapping("/mat/{inboundId}")
	public String materialDetail(@PathVariable("inboundId") String inboundId, Model model) {
		
		ReceiptDTO receiptDTO = inboundService.getMaterialInbound(inboundId);
		model.addAttribute("receiptDTO", receiptDTO);
		
		return "inbound/inbound_detail";
	}
	
	// 완제품 입고 상세페이지
	@GetMapping("/prd/{inboundId}")
	public String productDetail(@PathVariable("inboundId") String inboundId, Model model) {
		
		ReceiptDTO receiptDTO = inboundService.getMaterialInbound(inboundId);
		model.addAttribute("receiptDTO", receiptDTO);
		
		return "inbound/product_detail";
	}
	
	// 재입고 상세페이지
	@GetMapping("/re/{inboundId}")
	public String reMaterialDetailDetail(@PathVariable("inboundId") String inboundId, Model model) {
		
		ReceiptDTO receiptDTO = inboundService.getMaterialInbound(inboundId);
		model.addAttribute("receiptDTO", receiptDTO);
		
		return "inbound/re_material_detail";
	}
	
	// 원재료 입고 처리
	@PostMapping("/mat/complete")
	@ResponseBody
	public Map<String, Object> materialComplete(@RequestBody ReceiptDTO receiptDTO, @AuthenticationPrincipal LoginDTO loginDTO) {
		inboundService.updateInbound(receiptDTO, loginDTO.getEmpId());
		Map<String, Object> result = new HashMap<>();
		
		result.put("success", true);
		
		return result;
	}
	
	// 재입고 처리
	@PostMapping("/re/complete")
	@ResponseBody
	public Map<String, Object> reMaterialComplete(@RequestBody ReceiptDTO receiptDTO, @AuthenticationPrincipal LoginDTO loginDTO) {
		inboundService.updateReInbound(receiptDTO, loginDTO.getEmpId());
		Map<String, Object> result = new HashMap<>();
		
		result.put("success", true);
		
		return result;
	}
}
