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
import com.yeoun.masterData.entity.BomMst;
import com.yeoun.masterData.entity.MaterialMst;
import com.yeoun.masterData.service.BomMstService;
import com.yeoun.outbound.dto.OutboundOrderItemDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Controller
@RequestMapping("/bom")
@RequiredArgsConstructor
@Log4j2
public class BomMstController {
	
	private final BomMstService bomMstService; 
	//BOM 완제품 드롭다운 조회
	@ResponseBody
	@GetMapping("/prdList")
	public List<Map<String, Object>> findBomPrdList(Model model, @AuthenticationPrincipal LoginDTO loginDTO) {
		return bomMstService.findBomPrdList();
	}
	
	//BOM 원재료 드롭다운 조회
	@ResponseBody
	@GetMapping("/matList")
	public List<MaterialMst> findBomMatList(Model model, @AuthenticationPrincipal LoginDTO loginDTO) {
		return bomMstService.findBomMatList();
	}
	
	//BOM 단위 드롭다운조회
	@ResponseBody
	@GetMapping("/UnitList")
	public List<Map<String, Object>> findBomUnitList(Model model, @AuthenticationPrincipal LoginDTO loginDTO) {
		return bomMstService.findBomUnitList();
	}
	
	//BOM 정보조회
 	@ResponseBody
  	@GetMapping("/list")
  	public List<Map<String, Object>> bomList(Model model, @AuthenticationPrincipal LoginDTO loginDTO,
  				@RequestParam(value = "bomId", required = false) String bomId,
  				@RequestParam(value = "matId", required = false) String matId) {
		// 보조 데이터: 페이지에서 사용할 bomId 목록
		model.addAttribute("bomIdList", bomMstService.findAllDetail());
		List<Map<String, Object>> bomList = bomMstService.findBybomList(bomId, matId);
	    return bomList;
	}

	//BOM 상세조회
 	@ResponseBody
  	@GetMapping("/bomDetail/list")
  	public List<Object[]> bomDetailList(Model model, @AuthenticationPrincipal LoginDTO loginDTO) {
 	    List<Object[]> bomDetailList = bomMstService.findAllDetail();
 	    return bomDetailList;
  	}
	//BOM 상세 -완제품 (JSON 반환)
	@ResponseBody
	@GetMapping("/bomDetail/prdList/{bomId}")
	public List<Object[]> bomDetailPrdList(Model model, @AuthenticationPrincipal LoginDTO loginDTO, @PathVariable("bomId") String bomId) {
		log.info("bomDetailPrdList bomId------------->{}", bomId);
		return bomMstService.getBomPrdList(bomId);
	}
	//BOM 상세 -원재료 (JSON 반환)
	@ResponseBody
	@GetMapping("/bomDetail/matList/{bomId}")
	public List<Object[]> bomDetailMatList(Model model, @AuthenticationPrincipal LoginDTO loginDTO, @PathVariable("bomId") String bomId) {
		log.info("bomDetailMatList bomId------------->{}", bomId);
		return bomMstService.getBomMatList(bomId);
	}
	//BOM 상세 -원재료(포장재) (JSON 반환)
	@ResponseBody
	@GetMapping("/bomDetail/matTypeList/{bomId}")
	public List<Object[]> bomDetailMatTypeList(Model model, @AuthenticationPrincipal LoginDTO loginDTO
		, @PathVariable("bomId") String bomId) {
		log.info("bomDetailMatTypeList bomId------------->{}", bomId);
		return bomMstService.getBomMatTypeList(bomId);
	}
  
	//BOM 저장
    @ResponseBody
   	@PostMapping("/save")
   	public String bomSave(Model model, @AuthenticationPrincipal LoginDTO loginDTO,@RequestBody Map<String, Object> param) {
    	String empId = (loginDTO != null && loginDTO.getEmpId() != null) ? loginDTO.getEmpId() : "SYSTEM";
    	log.info("bomSave------------->{}", param);
    	return bomMstService.saveBomMst(empId,param);
   	}
    //BOM 삭제 (prdId + matId 쌍으로 삭제)
    @ResponseBody
    @PostMapping("/delete")
    public String bomDelete(Model model, @AuthenticationPrincipal LoginDTO loginDTO,@RequestBody List<java.util.Map<String, String>> rows) {
		String empId = (loginDTO != null && loginDTO.getEmpId() != null) ? loginDTO.getEmpId() : "SYSTEM";
		log.info("bomDelete (pairs)------------->{}", rows);
		return bomMstService.deleteBomMstByPairs(empId, rows);
    }
 	
 	// prdId에 해당하는 BOM 리스트 조회
 	@GetMapping("/list/data/{prdId}")
 	@ResponseBody
 	public ResponseEntity<List<OutboundOrderItemDTO>> outboundBomList(@PathVariable("prdId") String prdId) {
 		
 		List<OutboundOrderItemDTO> bomList = bomMstService.getBomListByPrdId(prdId);
 		
 		return ResponseEntity.ok(bomList);
 	}

}
