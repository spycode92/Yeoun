package com.yeoun.qc.controller;

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
import org.springframework.web.multipart.MultipartFile;

import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.common.dto.FileAttachDTO;
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
	public List<QcResultListDTO> qcResultListForGrid(@RequestParam(name = "startDate", required = false) String startDate,
											         @RequestParam(name = "endDate", required = false) String endDate,
											         @RequestParam(name = "keyword", required = false) String keyword,
											         @RequestParam(name = "result", defaultValue = "ALL") String result) {
		return qcResultService.getQcResultListForView(startDate, endDate, keyword, result);
	}
	
	// --------------------------------------------------------------
	// QC 검사 시작
	@PostMapping("/start")
	@ResponseBody
	public Map<String, Object> startQc(@RequestParam("orderId") String orderId) {
	    qcResultService.startQc(orderId);
	    return Map.of("success", true, "message", "QC 검사를 시작했습니다.");
	}

	// QC 검사 중단(저장 없이 닫기)
	@PostMapping("/cancel")
	@ResponseBody
	public Map<String, Object> cancelQc(@RequestParam("orderId") String orderId) {
	    qcResultService.cancelQc(orderId);
	    return Map.of("success", true, "message", "QC 검사가 중단되었습니다.");
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

	// -----------------------------------------------------------------------------------
	// QC 상세 항목별 첨부파일
    // 1) QC 상세별 파일 업로드
    @PostMapping("/detail/{qcResultDtlId}/files")
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadQcDetailFiles(@PathVariable("qcResultDtlId") String qcResultDtlId,
            													   @RequestParam("qcDetailFiles") List<MultipartFile> qcDetailFiles) {

        Map<String, String> msg = new HashMap<>();

        try {
            qcResultService.saveQcDetailFiles(qcResultDtlId, qcDetailFiles);
            msg.put("msg", "QC 첨부파일이 등록되었습니다.");
            return ResponseEntity.ok(msg);

        } catch (Exception e) {
            msg.put("msg", "QC 첨부파일 등록에 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
        }
    }

    // --------------------------------------------------
    // 2) QC 상세별 파일 목록 조회
    @GetMapping("/detail/{qcResultDtlId}/files")
    @ResponseBody
    public ResponseEntity<List<FileAttachDTO>> getQcDetailFiles(@PathVariable("qcResultDtlId") String qcResultDtlId) {

        List<FileAttachDTO> files = qcResultService.getQcDetailFiles(qcResultDtlId);
        return ResponseEntity.ok(files);
    }



	
	
	

}
