package com.yeoun.qc.controller;

import java.util.List;
import java.util.Map;

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
import com.yeoun.qc.dto.QcDetailRowDTO;
import com.yeoun.qc.dto.QcRegistDTO;
import com.yeoun.qc.dto.QcResultListDTO;
import com.yeoun.qc.dto.QcResultViewDTO;
import com.yeoun.qc.dto.QcSaveRequestDTO;
import com.yeoun.qc.service.QcResultService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/qc")
@RequiredArgsConstructor
public class QcController {
	
	private final QcResultService qcResultService;
	
	// QC 등록 목록 페이지
	@GetMapping("/regist")
	public String qcRegistList(Model model, @AuthenticationPrincipal LoginDTO loginUser) {
		
		model.addAttribute("loginEmpId", loginUser.getEmpId());
		model.addAttribute("loginEmpName", loginUser.getEmpName());
		
		return "/qc/regist_list";
	}
	
	// QC 등록 목록 데이터
	@GetMapping("/regist/data")
	@ResponseBody
	public List<QcRegistDTO> qcRegistListForGrid() {
		return qcResultService.getQcResultListForRegist();
	}

	// QC 등록 모달 데이터
	@GetMapping("/{qcResultId}/details")
	@ResponseBody
	public List<QcDetailRowDTO> qcRegistDetails(@PathVariable("qcResultId") Long qcResultId) {
		return qcResultService.getDetailRows(qcResultId);
	}
	
	// QC 결과 조회 페이지
	@GetMapping("/result")
	public String qcResultList() {
		return "/qc/result_list";
	}
	
	// QC 결과 목록 데이터
	@GetMapping("/result/data")
	@ResponseBody
	public List<QcResultListDTO> qcResultListForGrid() {
		return qcResultService.getQcResultListForView();
	}
	
	// QC 결과 저장 (등록 모달에서 입력값 저장)
	@PostMapping("/{qcResultId}/save")
	@ResponseBody
	public Map<String, Object> saveQcResult(@PathVariable("qcResultId") Long qcResultId,
											@RequestBody QcSaveRequestDTO qcSaveRequestDTO) {
		
		qcResultService.saveQcResult(qcResultId, qcSaveRequestDTO);
		
		return Map.of(
				"success", true,
				"message", "QC 검사 결과가 저장되었습니다."
		);
	}
	
	// QC 결과 상세 (조회 모달용 헤더 + 디테일)
	@GetMapping("/result/{qcResultId}")
	@ResponseBody
	public QcResultViewDTO getQcResultView(@PathVariable("qcResultId") Long qcResultId) {
	    return qcResultService.getQcResultView(qcResultId);
	}

	
	
	

}
