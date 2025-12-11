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
import org.springframework.web.bind.annotation.ResponseBody;

import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.masterData.entity.BomMst;
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
	
	//BOM 조회
 	@ResponseBody
  	@GetMapping("/list")
  	public List<BomMst> bomList(Model model, @AuthenticationPrincipal LoginDTO loginDTO) {
 	    List<BomMst> bomList = bomMstService.findAll();
 	    return bomList;
  	}
  
	//BOM 저장
    @ResponseBody
   	@PostMapping("/save")
   	public String bomSave(Model model, @AuthenticationPrincipal LoginDTO loginDTO,@org.springframework.web.bind.annotation.RequestBody Map<String, Object> param) {
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
