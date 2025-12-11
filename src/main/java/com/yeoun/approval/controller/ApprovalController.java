package com.yeoun.approval.controller;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yeoun.approval.dto.ApprovalDocDTO;
import com.yeoun.approval.dto.ApprovalDocGridDTO;
import com.yeoun.approval.dto.ApprovalFormDTO;
import com.yeoun.approval.repository.ApprovalDocRepository;
import com.yeoun.approval.service.ApprovalDocService;
import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.common.dto.FileAttachDTO;
import com.yeoun.emp.dto.EmpDTO;
import com.yeoun.emp.dto.EmpListDTO;
import com.yeoun.emp.entity.Dept;
import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.emp.service.EmpService;
import com.yeoun.pay.dto.PayrollHistoryProjection;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Controller
@RequestMapping("/approval")
@RequiredArgsConstructor
@Log4j2
public class ApprovalController {

	private final ApprovalDocService approvalDocService;
	
	//전자결재 연결페이지
  	@GetMapping("/approval_doc")
  	public String approvalDoc(Model model, @AuthenticationPrincipal LoginDTO loginDTO) {
		model.addAttribute("empList", approvalDocService.getEmp());//기안자 목록 불러오기
		model.addAttribute("formTypes",approvalDocService.getFormTypes(loginDTO.getDeptId())); //"DEP001"결재- 기안서 양식종류 불러오기
		model.addAttribute("deptList", approvalDocService.getDept()); //부서목록 불러오기
		model.addAttribute("approvalDocGridDTO", new ApprovalDocGridDTO());//문서DTO
		model.addAttribute("positionList",approvalDocService.getPosition());//직급정보불러오기
		// --------------------------------------------
		model.addAttribute("currentUserId", loginDTO.getEmpId());
		model.addAttribute("currentUserName", loginDTO.getEmpName());
		return "approval/approval_doc";
 	}

	//사원목록불러오기 토스트 셀렉트박스
	@ResponseBody
	@GetMapping("/empList")
	public List<Object[]> getDeptList() {
		 return (List<Object[]>) approvalDocService.getEmp2();
	}

	//날짜,제목 기안자 조회
	@PostMapping("/searchAllGrids")
	@ResponseBody 
	public Map<String, List<ApprovalDocGridDTO>> searchApprovalDocGrid1(@AuthenticationPrincipal LoginDTO loginDTO ,@RequestBody Map<String,Object> searchParams){
		Map<String, List<ApprovalDocGridDTO>> allGridsData = approvalDocService.getAllGridsData(loginDTO.getEmpId(), searchParams);
		return allGridsData;
		
	}

	// 결재문서 파일데이터 조회
    @GetMapping("/file/{approvalId}")
    public ResponseEntity<List<FileAttachDTO>> getApprovalDocFile(@PathVariable("approvalId")Long approvalId){
    	List<FileAttachDTO> fileDTOList = approvalDocService.getApprovalDocFiles(approvalId);
        
        return ResponseEntity.ok(fileDTOList);
    }

	//기안서 등록(저장)
    @PostMapping("/approval_doc")
    public ResponseEntity<Map<String, Object>> approvalDocSave(
									@AuthenticationPrincipal LoginDTO loginDTO, 
									@RequestParam Map<String, String> doc,
    								@RequestParam(value = "itemImgFiles", required = false) MultipartFile[] files) {
        
		log.info("로그인 사용자 ID: {}", loginDTO.getEmpId());
    	log.info("받은 폼 데이터 (Map): {}", doc); 
    	int fileCount = (files != null) ? files.length : 0;
    	log.info("첨부된 파일 수: {}", fileCount);
										
    	try {
    	    approvalDocService.saveApprovalDoc(loginDTO.getEmpId(), doc, files); 
			
    	    Map<String, Object> response = new HashMap<>();
    	    response.put("status", "success");
    	    response.put("message", "결재 문서가 성공적으로 등록되었습니다.");
			
    	    return ResponseEntity.ok(response); 
			
    	} catch (Exception e) {
    	    // 예외 처리 로직 추가 (필수)
    	    log.error("결재 문서 등록 실패: {}", e.getMessage(), e); // e를 추가하여 스택 트레이스 출력
		
    	    Map<String, Object> errorResponse = new HashMap<>();
    	    errorResponse.put("status", "error");
    	    errorResponse.put("message", "결재 문서 등록 중 오류가 발생했습니다: " + e.getMessage());
		
    	    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    	}
    }

}
