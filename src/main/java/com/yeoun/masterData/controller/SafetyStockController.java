package com.yeoun.masterData.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.masterData.entity.SafetyStock;
import com.yeoun.masterData.service.SafetyStockService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Controller
@RequestMapping("/safetyStock")
@RequiredArgsConstructor
@Log4j2
public class SafetyStockController {
	
	private final SafetyStockService safetyStockService;
	
	//안전재고 조회
	@ResponseBody
  	@GetMapping("/list")
  	public List<SafetyStock> safetyStockList(Model model, @AuthenticationPrincipal LoginDTO loginDTO) {
		return safetyStockService.findAll();
  	}
	//안전재고 저장
	@ResponseBody
	@PostMapping(value = "/save", consumes = "application/json")
	public String safetyStockSave(Model model, @AuthenticationPrincipal LoginDTO loginDTO, @RequestBody Map<String, Object> param) {
		log.info("safetyStockSave------------->{}", param);
		return safetyStockService.saveSafetyStock(loginDTO.getEmpId(), param);
	}

	//안전재고 삭제
	@ResponseBody
	@PostMapping(value = "/delete", consumes = "application/json")
	public String safetyStockDelete(Model model, @AuthenticationPrincipal LoginDTO loginDTO, @RequestBody java.util.List<String> itemIds) {
		log.info("safetyStockDelete by {} -> {}", loginDTO == null ? "anonymous" : loginDTO.getEmpId(), itemIds);
		return safetyStockService.deleteSafetyStock(loginDTO == null ? null : loginDTO.getEmpId(), itemIds);
	}

}
