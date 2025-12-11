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
import com.yeoun.masterData.entity.MaterialMst;
import com.yeoun.masterData.service.MaterialMstService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Controller
@RequestMapping("/material")
@RequiredArgsConstructor
@Log4j2
public class MaterialMstController {
	
	private final MaterialMstService materialMstService;
	
	//원재료 조회
	@ResponseBody
  	@GetMapping("/list")
  	public List<MaterialMst> materialList(Model model, @AuthenticationPrincipal LoginDTO loginDTO) {
		return materialMstService.findAll();
  	}
	//원재료 저장
    @ResponseBody
   	@PostMapping("/save")
   	public org.springframework.http.ResponseEntity<java.util.Map<String,Object>> materialSave(Model model, @AuthenticationPrincipal LoginDTO loginDTO,@RequestBody Map<String, Object> param) {
    	log.info("param------------->{}",param);
    	java.util.Map<String,Object> resp = new java.util.HashMap<>();
    	try {
    		String empId = (loginDTO != null && loginDTO.getEmpId() != null) ? loginDTO.getEmpId() : "SYSTEM";
    		String result = materialMstService.saveMaterialMst(empId, param);
    		if (result != null && result.trim().toLowerCase().startsWith("success")) {
    			resp.put("status", "success");
    			resp.put("message", result);
    			return org.springframework.http.ResponseEntity.ok(resp);
    		} else {
    			resp.put("status", "error");
    			resp.put("message", result == null ? "unknown error" : result);
    			return org.springframework.http.ResponseEntity.status(500).body(resp);
    		}
    	} catch (Exception e) {
    		log.error("materialSave error", e);
    		resp.put("status", "error");
    		resp.put("message", e.getMessage());
    		return org.springframework.http.ResponseEntity.status(500).body(resp);
    	}
   	}

	//원재료 삭제
	@ResponseBody
	@PostMapping("/delete")
	public String materialDelete(Model model,@AuthenticationPrincipal LoginDTO loginDTO,@RequestBody List<String> param) {
		return materialMstService.deleteMaterialMst(loginDTO.getEmpId(),param);
	}

}
