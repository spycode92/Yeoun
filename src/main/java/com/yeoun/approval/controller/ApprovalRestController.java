package com.yeoun.approval.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yeoun.approval.dto.ApprovalDocDTO;
import com.yeoun.approval.dto.ApprovalDocGridDTO;
import com.yeoun.approval.dto.ApprovalFormDTO;
import com.yeoun.approval.dto.ApproverDTO;

import com.yeoun.approval.service.ApprovalDocService;
import com.yeoun.auth.dto.LoginDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/approvals")
@Log4j2
public class ApprovalRestController {
	private final ApprovalDocService approvalDocService;

	// -------------------------------------------------------------------------------------------------
	// 메인페이지 결제 문서 목록 조회(내가 결제해야할문서, 내가 올린 결제문서)
	@GetMapping("")
	public ResponseEntity<List<ApprovalDocDTO>> summaryApproval(@AuthenticationPrincipal LoginDTO loginDTO) {
		String empId = loginDTO.getEmpId();
		System.out.println(empId);
		Page<ApprovalDocDTO> approvalDocDTOPage = approvalDocService.getSummaryApproval(empId);

		if (approvalDocDTOPage.getContent() == null) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(approvalDocDTOPage.getContent());
	}

	@PatchMapping("/{approvalId}")
	public ResponseEntity<Map<String, String>> approvalCheck(@PathVariable("approvalId") Long approvalId,
			@AuthenticationPrincipal LoginDTO loginDTO, @RequestBody Map<String, Object> requestBody) {

		String btn = (String) requestBody.get("action");
		String stampImageBase64 = (String) requestBody.get("stampImage");
		Map<String, String> result = new HashMap<>();
		String empId = loginDTO.getEmpId();
		String msg = "";

		if ("accept".equals(btn)) {
			msg = "결제 승인";
		} else {
			msg = "반려";
		}
		try {
			// 결제확인 버튼을 눌럿을때 작동할 서비스
			approvalDocService.updateApproval(approvalId, empId, btn, stampImageBase64);

			result.put("result", msg + " 완료!!!");
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			result.put("result", msg + " 실패 : " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
		}
	}

	// 기본 결재권자 가져오기
	@GetMapping("/defaultApprover")
	public ResponseEntity<?> getDefaultApprover(@AuthenticationPrincipal LoginDTO loginDTO) {
		String empId = loginDTO.getEmpId();

		try {
			List<ApprovalFormDTO> approvalFormList = approvalDocService.getDefaultApproverList(empId);
			return ResponseEntity.ok(approvalFormList);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	@GetMapping("/approvers/{approvalId}")
	public ResponseEntity<List<ApproverDTO>> getApproverList(@PathVariable("approvalId") Long approvalId) {

		List<ApproverDTO> approverDTOList = approvalDocService.getApproverDTOList(approvalId);

		return ResponseEntity.ok(approverDTOList);

	}

	// 결재 문서의 도장 이미지 조회
	@GetMapping("/stamps/{approvalId}")
	public ResponseEntity<Map<String, String>> getApprovalStamps(@PathVariable("approvalId") Long approvalId) {
		Map<String, String> stampImages = approvalDocService.getApprovalStampImages(approvalId);
		return ResponseEntity.ok(stampImages);
	}

}
