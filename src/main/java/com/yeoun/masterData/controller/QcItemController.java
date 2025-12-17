package com.yeoun.masterData.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.masterData.entity.QcItem;
import com.yeoun.masterData.service.QcItemService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Controller
@RequestMapping("/masterData")
@RequiredArgsConstructor
@Log4j2
public class QcItemController {
	private final QcItemService qcItemService;

	//품질항목관리 연결페이지(검사 X)
  	@GetMapping("/qc_item")
  	public String qcItem(Model model, @AuthenticationPrincipal LoginDTO loginDTO) {
		model.addAttribute("qcIdList", qcItemService.qcIdList());//기안자 목록 불러오기
		return "masterData/qc_item";
 	}
    
  	//품질의기준 조회
  	@ResponseBody
  	@GetMapping("/qc_item/list")
  	public List<Map<String, Object>> qcItemList(Model model, @AuthenticationPrincipal LoginDTO loginDTO
	  			,@RequestParam(value = "qcItemId", required = false) String qcItemId) {
			log.info("qc_item/list called with qcItemId={}", qcItemId);
			return qcItemService.qcItemList(qcItemId);
  	}
  	
  	//품질기준 저장
  	@PostMapping("/qcItem/save")
  	public String saveItem(Model model,@AuthenticationPrincipal LoginDTO loginDTO,@ModelAttribute @Valid QcItem qcItem) {
  		//model.addAttribute("qcItem", qcItem);
  		
  		qcItemService.saveQcItem(loginDTO.getEmpId(),qcItem);
  		return "redirect:/masterData/qc_item";
  		
  	}
	//품질기준 삭제 (AJAX 호출을 위한 응답: 텍스트 반환)
	@ResponseBody
	@PostMapping(value = "/qcItem/delete", consumes = "application/json")
	public String deleteItem(@RequestBody List<String> param) {
		return qcItemService.deleteQcItem(param);
	}
  	

}
