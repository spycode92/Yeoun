package com.yeoun.masterData.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.masterData.service.MaterialMstService;
import com.yeoun.masterData.service.BomMstService;
import com.yeoun.masterData.service.ProductMstService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Controller
@RequestMapping("/masterData")
@RequiredArgsConstructor
@Log4j2
public class ProductMstController {

	private final ProductMstService productMstService;
	private final MaterialMstService materialMstService;
	private final BomMstService bomMstService;
	
    //기준정보관리(완제품/원재료) 연결페이지
  	@GetMapping("/product")
  	public String product(Model model, @AuthenticationPrincipal LoginDTO loginDTO) {
		model.addAttribute("prdMstList", productMstService.findAll());
		model.addAttribute("mstMstList", materialMstService.findAll());
		return "masterData/product";
 	}
	//완제품 품목명(향수타입) 드롭다운
	@ResponseBody
	@GetMapping("/product/prdItemNameList")
	public List<Map<String, Object>> prdItemNameList(Model model, @AuthenticationPrincipal LoginDTO loginDTO) {
		return productMstService.findByPrdItemNameList();
	}
	//완제품 제품유형 드롭다운
	@ResponseBody
	@GetMapping("/product/prdItemTypeList")
	public List<Map<String, Object>> prdTypeList(Model model, @AuthenticationPrincipal LoginDTO loginDTO) {
		return productMstService.findByPrdTypeList();
	}
	//완제품 단위 드롭다운
	@ResponseBody
	@GetMapping("/product/prdUnitList")
	public List<Map<String, Object>> prdUnitList(Model model, @AuthenticationPrincipal LoginDTO loginDTO) {
		return productMstService.findByPrdUnitList();
	}
	//완제품 제품상태 드롭다운
	@ResponseBody
	@GetMapping("/product/prdStatusList")
	public List<Map<String, Object>> prdStatusList(Model model, @AuthenticationPrincipal LoginDTO loginDTO) {
		return productMstService.findByPrdStatusList();
	}

	// 완제품 그리드 조회
	@ResponseBody
	@GetMapping("/product/list")
	public List<Map<String, Object>> productList(Model model, @AuthenticationPrincipal LoginDTO loginDTO,
										@RequestParam(value = "prdId", required = false) String prdId,
										@RequestParam(value = "prdName", required = false) String prdName) {
		// 서비스에서 null/빈값 처리를 수행하므로 그대로 전달
		return productMstService.findByPrdIdList(prdId, prdName);
	}

	@ResponseBody
  	@PostMapping("/product/save")
	public String productSave(Model model, @AuthenticationPrincipal LoginDTO loginDTO,@RequestBody Map<String, Object> param) {
		return productMstService.saveProductMst(loginDTO.getEmpId(),param);
  	}

	@ResponseBody
	@PostMapping("/product/delete")
	public ResponseEntity<Map<String, Object>> productDelete(Model model,
		@AuthenticationPrincipal LoginDTO loginDTO,
		@RequestBody List<String> rowKeys) {

		Map<String, Object> param = new java.util.HashMap<>();
		param.put("rowKeys", rowKeys);
		Map<String, Object> res = productMstService.deleteProduct(param);
		return ResponseEntity.ok(res);
	}

  	//BOM 연결페이지
	@GetMapping("/bom_stock")
	public String bomStock(Model model, @AuthenticationPrincipal LoginDTO loginDTO) {
		// BOM 페이지에서 사용하는 bomIdList를 서비스에서 조회하여 모델에 추가
		model.addAttribute("bomIdList", bomMstService.findAllDetail());
		return "masterData/bom_stock";
	}
    
}
