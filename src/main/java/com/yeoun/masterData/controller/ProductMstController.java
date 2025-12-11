package com.yeoun.masterData.controller;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.masterData.entity.ProductMst;
import com.yeoun.masterData.service.ProductMstService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Controller
@RequestMapping("/masterData")
@RequiredArgsConstructor
@Log4j2
public class ProductMstController {

	private final ProductMstService productMstService;
    //기준정보관리(완제품/원재료) 연결페이지
  	@GetMapping("/product")
  	public String product(Model model, @AuthenticationPrincipal LoginDTO loginDTO) {
		//model.addAttribute("empList", approvalDocService.getEmp());//기안자 목록 불러오기
		return "masterData/product";
 	}
  	
  	@ResponseBody
  	@GetMapping("/product/list")
  	public List<ProductMst> productList(Model model, @AuthenticationPrincipal LoginDTO loginDTO) {
  		log.info("productMstService.getProductAll()------------->{}",productMstService.findAll());
		return productMstService.findAll();
  	}

	@ResponseBody
  	@PostMapping("/product/save")
	public String productSave(Model model, @AuthenticationPrincipal LoginDTO loginDTO,@RequestBody Map<String, Object> param) {
  		log.info("param------------->{}",param);
		return productMstService.saveProductMst(loginDTO.getEmpId(),param);
  	}

	@ResponseBody
	@PostMapping("/product/delete")
	public ResponseEntity<Map<String, Object>> productDelete(Model model,
		@AuthenticationPrincipal LoginDTO loginDTO,
		@RequestBody List<String> rowKeys) {

		log.info("rowKeys------------->{}", rowKeys);
		Map<String, Object> param = new java.util.HashMap<>();
		param.put("rowKeys", rowKeys);
		Map<String, Object> res = productMstService.deleteProduct(param);
		return ResponseEntity.ok(res);
	}

  	//BOM 연결페이지
  	@GetMapping("/bom_stock")
  	public String bomStock(Model model, @AuthenticationPrincipal LoginDTO loginDTO) {
		//model.addAttribute("empList", approvalDocService.getEmp());//기안자 목록 불러오기
		return "masterData/bom_stock";
 	}
    
}
