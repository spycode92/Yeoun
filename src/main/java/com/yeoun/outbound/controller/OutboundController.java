package com.yeoun.outbound.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.inbound.dto.ReceiptDTO;
import com.yeoun.outbound.dto.OutboundOrderDTO;
import com.yeoun.outbound.service.OutboundService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Controller
@RequestMapping("/inventory/outbound")
@RequiredArgsConstructor
@Log4j2
public class OutboundController {
	private final OutboundService outboundService;
	
	// 출고관리 페이지
	@GetMapping("/list")
	public String outboundList(Model model) {
		// 탭 활성화를 위한 정보
		model.addAttribute("activeTab", "mat");
		return "outbound/outbound_list";
	}
	
	// 원재료 페이지
	@GetMapping("/materialList")
	public String material(Model model) {
		// 탭 활성화를 위한 정보
		model.addAttribute("activeTab", "mat");
		return "outbound/outbound_list";
	}
	
	// 완제품 페이지
	@GetMapping("/productList")
	public String product(Model model) {
		// 탭 활성화를 위한 정보
		model.addAttribute("activeTab", "pro");
		return "outbound/outbound_list";
	}
	
	// 출고 등록(원재료)
	@PostMapping("/mat/regist")
	public ResponseEntity<Map<String, String>> registMatOutbound(@RequestBody OutboundOrderDTO outboundOrderDTO, @AuthenticationPrincipal LoginDTO loginDTO) {
		try {
			outboundService.saveOutbound(outboundOrderDTO, loginDTO.getEmpId());
			
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Map.of("message", "등록 완료"));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(Map.of("message", e.getMessage()));
		}
	}
	
	// 출고 등록(완제품)
	@PostMapping("/fg/regist")
	public ResponseEntity<Map<String, String>> registFgOutbound(@RequestBody OutboundOrderDTO outboundOrderDTO, @AuthenticationPrincipal LoginDTO loginDTO) {
		try {
			outboundService.saveOutbound(outboundOrderDTO, loginDTO.getEmpId());
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Map.of("message", "등록 완료"));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(Map.of("message", e.getMessage()));
		}
	}
	
	// 출고 리스트 조회
	@GetMapping("/list/data")
	public ResponseEntity<List<OutboundOrderDTO>> outboundList(
			@RequestParam(required = false, name = "startDate") String startDate, 
			@RequestParam(required = false, name = "endDate") String endDate,
			@RequestParam(required = false, name = "keyword") String keyword
			) {
		// 문자열로 들어온 날짜 데이터를 변환
		LocalDateTime start = (startDate != null) ? LocalDate.parse(startDate).atStartOfDay() : LocalDate.now().withDayOfMonth(1).atStartOfDay();
		LocalDateTime end = (endDate != null) ? LocalDate.parse(endDate).atTime(23, 59, 59) : LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).atTime(23, 59, 59);
		
		List<OutboundOrderDTO> outboundList = outboundService.getOuboundList(start, end, keyword);
		
		return ResponseEntity.ok(outboundList);
	}
	
	// 원재료 출고 상세페이지
	@GetMapping("/mat/{outboundId}")
	public String materialDetail(@PathVariable("outboundId") String outboundId, Model model) {
		OutboundOrderDTO outboundOrderDTO = outboundService.getMaterialOutbound(outboundId);
		
		model.addAttribute("outboundOrderDTO", outboundOrderDTO);
		
		return "outbound/material_detail";
	}
	
	// 완제품 출고 상세페이지
	@GetMapping("/prd/{outboundId}")
	public String productDetail(@PathVariable("outboundId") String outboundId, Model model) {
		OutboundOrderDTO outboundOrderDTO = outboundService.getProductOutbound(outboundId);
		
		model.addAttribute("outboundOrderDTO", outboundOrderDTO);
		
		return "outbound/product_detail";
	}
	
	// 출고완료
	@PostMapping("/mat/complete")
	@ResponseBody
	public Map<String, Object> materialComplete(@RequestBody OutboundOrderDTO OutboundOrderDTO, @AuthenticationPrincipal LoginDTO loginDTO) {
		outboundService.updateOutbound(OutboundOrderDTO, loginDTO.getEmpId());
		
		Map<String, Object> result = new HashMap<>();
		
		result.put("success", true);
		
		return result;
	}
	
	@PostMapping("/prd/complete")
	@ResponseBody
	public Map<String, Object> productComplete(@RequestBody OutboundOrderDTO OutboundOrderDTO, @AuthenticationPrincipal LoginDTO loginDTO) {
		outboundService.updateOutbound(OutboundOrderDTO, loginDTO.getEmpId());
		
		Map<String, Object> result = new HashMap<>();
		
		result.put("success", true);
		
		return result;
	}
}
