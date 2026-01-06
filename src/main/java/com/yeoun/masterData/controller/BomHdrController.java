package com.yeoun.masterData.controller;

import java.util.List;
import java.util.Map;

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
import com.yeoun.masterData.entity.BomHdr;
import com.yeoun.masterData.entity.BomMst;
import com.yeoun.masterData.entity.MaterialMst;
import com.yeoun.masterData.service.BomHdrService;
import com.yeoun.masterData.service.BomMstService;
import com.yeoun.outbound.dto.OutboundOrderItemDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Controller
@RequestMapping("/bom")
@RequiredArgsConstructor
@Log4j2
public class BomHdrController {
	
	private final BomHdrService bomHdrService; 
	//BOM 그룹 BOM_HDR_TYPE 타입 드롭다운
	//hdrTypeLis
	@ResponseBody
	@GetMapping("/hdrTypeList")
	public List<Map<String, Object>> findBomHdrTypeList(Model model, @AuthenticationPrincipal LoginDTO loginDTO) {
		return bomHdrService.findBomHdrTypeList();
	}
	
	
	//BOM 그룹 조회
	@ResponseBody
	@GetMapping("/bomHdrList")
	public List<BomHdr> findBomHdrList(Model model, @AuthenticationPrincipal LoginDTO loginDTO,
			@RequestParam(value = "bomHdrId", required = false) String bomHdrId,
			@RequestParam(value = "bomHdrType", required = false) String bomHdrType
			) {
		return bomHdrService.findBomHdrList(bomHdrId,bomHdrType);
	}
	
	//Bom 그룹 수정(저장)
	@ResponseBody
	@PostMapping("/bomHdrSave")
	public String findBomHdrSave(@AuthenticationPrincipal LoginDTO loginDTO,@RequestBody Map<String, Object> param) {
		return bomHdrService.saveBomHdr(loginDTO.getEmpId(),param);
	}
	

}
