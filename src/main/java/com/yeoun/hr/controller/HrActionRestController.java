package com.yeoun.hr.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RestController;

import com.yeoun.approval.entity.ApprovalForm;
import com.yeoun.approval.repository.ApprovalFormRepository;
import com.yeoun.emp.dto.EmpListDTO;
import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.emp.service.EmpService;
import com.yeoun.hr.dto.HrActionDTO;
import com.yeoun.hr.dto.HrActionRequestDTO;
import com.yeoun.hr.service.HrActionService;


@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
@Log4j2
public class HrActionRestController {
	
	private final HrActionService hrActionService;
	private final EmpService empService;
	private final ApprovalFormRepository approvalFormRepository;
	private final EmpRepository empRepository;
	
	// 인사 발령 목록 (검색 + 페이징)
	@GetMapping("/actions")
	public List<HrActionDTO> getHrActionList(@RequestParam(defaultValue = "", name = "keyword") String keyword,
									        	@RequestParam(required = false, name = "actionType") String actionType,
									        	@RequestParam(required = false, name = "startDate") String startDate,
									        	@RequestParam(required = false, name = "endDate") String endDate) {

		return hrActionService.getHrActionList(keyword, actionType, startDate, endDate);

	}

	
	// =============================================================
	// 인사 발령 등록
	@PostMapping("/actions")
	public Long createHrAction(@RequestBody HrActionRequestDTO dto) {
	    return hrActionService.createAction(dto);
	}
	
	// 인사 발령 등록 화면용 사원 목록
	@ResponseBody
	@GetMapping("/employees")
	public List<EmpListDTO> getEmployeesForAction(@RequestParam(required = false, name = "deptId") String deptId,
	        									  @RequestParam(required = false, name = "posCode") String posCode,
	        									  @RequestParam(required = false, name = "status") String status,
	        									  @RequestParam(required = false, name = "keyword") String keyword) {
		return empService.getEmpListForHrAction(deptId, posCode, status, keyword);
	}
	
	// 결재자 목록 API (발령 대상자 기준)
	@GetMapping("/approvers")
	public List<EmpListDTO> getApprovers(@RequestParam(name = "formName") String formName,
	                                     @RequestParam(name = "empId") String empId) {

	    // 1) 발령 대상자 조회
	    Emp target = empRepository.findByEmpId(empId)
	            .orElseThrow(() -> new IllegalArgumentException("사원을 찾을 수 없습니다: " + empId));

	    String deptId = target.getDept().getDeptId();  // 대상자 부서 기준

	    // 2) 해당 부서 + 양식명으로 결재양식 조회
	    ApprovalForm form = approvalFormRepository
	            .findByFormNameAndDeptId(formName, deptId)
	            .orElseThrow(() -> new IllegalStateException(
	                    "해당 부서의 인사발령 결재양식이 없습니다. deptId=" + deptId));

	    // 3) APPROVER_1/2/3 → EmpListDTO로 변환 (순서 유지)
	    List<EmpListDTO> result = new java.util.ArrayList<>();

	    if (form.getApprover1() != null && !form.getApprover1().isBlank()) {
	        empRepository.findByEmpId(form.getApprover1())
	                .map(EmpListDTO::fromEntity)
	                .ifPresent(result::add);
	    }
	    if (form.getApprover2() != null && !form.getApprover2().isBlank()) {
	        empRepository.findByEmpId(form.getApprover2())
	                .map(EmpListDTO::fromEntity)
	                .ifPresent(result::add);
	    }
	    if (form.getApprover3() != null && !form.getApprover3().isBlank()) {
	        empRepository.findByEmpId(form.getApprover3())
	                .map(EmpListDTO::fromEntity)
	                .ifPresent(result::add);
	    }

	    return result;
	}


}