package com.yeoun.attendance.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yeoun.attendance.dto.AccessLogDTO;
import com.yeoun.attendance.dto.AttendanceDTO;
import com.yeoun.attendance.dto.WorkPolicyDTO;
import com.yeoun.attendance.entity.WorkPolicy;
import com.yeoun.attendance.service.AttendanceService;
import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.common.dto.CommonCodeDTO;
import com.yeoun.common.service.CommonCodeService;
import com.yeoun.emp.dto.EmpListDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Controller
@RequestMapping("/attendance")
@RequiredArgsConstructor
@Log4j2
public class AttendanceController {
	
	private final AttendanceService attendanceService;
	private final CommonCodeService commonCodeService;
	
	// 출/퇴근 입력
	@PostMapping("/toggle/{empId}")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> processAttendance(@PathVariable("empId") String empId) {
		Map<String, Object> result = new HashMap<>();
		try {
			String resultStatus = attendanceService.registAttendance(empId);
			
			result.put("success", true);
			result.put("status", resultStatus);
			result.put("buttonEnabled", attendanceService.isAttendanceButtonEnabled(empId));
			
			return ResponseEntity.ok(result);
		} catch (NoSuchElementException e) {
			// 사원 정보가 없을 경우
			e.printStackTrace();
			result.put("success", false);
			result.put("message", e.getMessage());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
		} catch (Exception e) {
			e.printStackTrace();
			// 그 외의 모든 예외
			result.put("success", false);
			result.put("message", e.getMessage());
			result.put("error", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
		}
	}
	
	// 관리자용 출/퇴근 현황 페이지
	@GetMapping("/list")
	public String attendanceAdmin() {
		return "attendance/commute_admin";
	}
	
	// 부서장 또는 관리자가 확인하는 근태현황
	@GetMapping("/list/data")
	@ResponseBody
	public ResponseEntity<List<AttendanceDTO>> attendanceAdminList(
			@AuthenticationPrincipal LoginDTO loginDTO, 
			@RequestParam(required = false, name = "startDate") String startDate,  
			@RequestParam(required = false, name = "endDate") String endDate) {
		
		LocalDate start = (startDate != null) ? LocalDate.parse(startDate) : LocalDate.now().withDayOfMonth(1);
		LocalDate end = (endDate != null) ? LocalDate.parse(endDate) : LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
		
		List<AttendanceDTO> attendanceList = attendanceService.getAttendanceListByRole(loginDTO, start, end);;
		
		return ResponseEntity.ok(attendanceList);
	}
	
	// 사원번호 조회
	@GetMapping("/search")
	public ResponseEntity<?> empInfo(@RequestParam("empId") String empId) {
		try {
			EmpListDTO empListDTO = attendanceService.getEmp(empId);
			
			return ResponseEntity.ok(empListDTO);
		} catch (NoSuchElementException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
								 .body(Map.of("message", e.getMessage()));
		}
	}
	
	// 관리자 출/퇴근 수기 등록
	@PostMapping
	public ResponseEntity<Map<String, String>> registAttendance(@RequestBody AttendanceDTO attendanceDTO, @AuthenticationPrincipal LoginDTO loginDTO) {
		try {
			attendanceService.registAttendance(attendanceDTO, loginDTO);
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Map.of("message", "등록 완료"));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(Map.of("message", e.getMessage()));
		}
	}
	
	// 출/퇴근 상세 내역
	@GetMapping("/{attendanceId}")
	public ResponseEntity<?> attendanceInfo(@PathVariable("attendanceId") Long attendanceId) {
		 try {
			 AttendanceDTO attendanceDTO = attendanceService.getAttendance(attendanceId);
			 return ResponseEntity.ok(attendanceDTO);
		 } catch (NoSuchElementException e) {
			 return ResponseEntity.status(HttpStatus.NOT_FOUND)
					 		      .body(Map.of("message", e.getMessage()));
		 }
	}
	
	// 출/퇴근 수정
	@PatchMapping("/{attendanceId}")
	public ResponseEntity<Map<String, String>> modifyAttendance(
			@PathVariable("attendanceId") Long attendanceId, 
			@RequestBody AttendanceDTO attendanceDTO,
			@AuthenticationPrincipal LoginDTO loginDTO) {
		try {
			attendanceService.modifyAttendance(attendanceId, attendanceDTO, loginDTO);
			return ResponseEntity.ok(Map.of("message", "출/퇴근 수정 완료"));
		} catch (NoSuchElementException  e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("message", e.getMessage()));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
		} catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "출근 등록 중 오류가 발생했습니다."));
		}
	}
	
	@GetMapping("/my")
	public String attendanceMyPage() {
	    return "attendance/commute";
	}
	
	// 개인 출/퇴근 현황 페이지
	@GetMapping("/my/data")
	@ResponseBody
	public ResponseEntity<List<AttendanceDTO>> attendanceList(
			@AuthenticationPrincipal LoginDTO loginDTO, 
			@RequestParam(required = false, name = "startDate") String startDate,  
			@RequestParam(required = false, name = "endDate") String endDate) {
		
		LocalDate start = (startDate != null) ? LocalDate.parse(startDate) : LocalDate.now().withDayOfMonth(1);
		LocalDate end = (endDate != null) ? LocalDate.parse(endDate) : LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
		
		try {
			List<AttendanceDTO> AttendanceDTOList = attendanceService.getMyAttendanceList(loginDTO.getEmpId(), start, end);
			return ResponseEntity.ok(AttendanceDTOList);
		} catch(NoSuchElementException  e) {
			return null;
		}
	}
	
	// 외근 등록
	@PostMapping("/outwork")
	public String registOutwork(@ModelAttribute("accessLogDTO") AccessLogDTO accessLogDTO, RedirectAttributes redirectAttributes) {
		attendanceService.registOutwork(accessLogDTO);
		redirectAttributes.addFlashAttribute("message", "외근 등록이 완료되었습니다.");
		
		return "redirect:/attendance/outwork";
	}
	
	// 근무정책관리 조회
	@GetMapping("/policy")
	public String policyForm(Model model) {
		WorkPolicyDTO workPolicyDTO = attendanceService.getWorkPolicy();
		// 공통코드 테이블에서 연차 산정 기준 조회
		List<CommonCodeDTO> commonCodeDTO = commonCodeService.getAnnualBasis("ANNUAL_BASIS");
		
		model.addAttribute("workPolicyDTO", workPolicyDTO);
		model.addAttribute("commonCodeDTO", commonCodeDTO);
		
		return "attendance/policy";
	}
	
	// 근무정책 등록
	@PostMapping("/policy")
	public String registPolicy(@ModelAttribute("workPolicyDTO") @Valid WorkPolicyDTO workPolicyDTO,  
			BindingResult bindingResult, 
			RedirectAttributes redirectAttributes) {
		
		if (bindingResult.hasErrors()) {
			return  "attendance/policy";
		}
		
		String message = attendanceService.registWorkPolicy(workPolicyDTO);
		redirectAttributes.addFlashAttribute("msg", message);
		
		return "redirect:/attendance/policy";
	}
	
	// 건물 출입 현황 페이지
	@GetMapping("/accessList")
	public String accessList() {
		return "attendance/access_list";
	}
	
	// 건물 출입 현황 데이터
	@GetMapping("/accessList/data")
	public ResponseEntity<?> accessListInfo(
			@AuthenticationPrincipal LoginDTO loginDTO, 
			@RequestParam(required = false, name = "startDate") String startDate,  
			@RequestParam(required = false, name = "endDate") String endDate) {
		
		LocalDate start = (startDate != null) ? LocalDate.parse(startDate) : LocalDate.now().withDayOfMonth(1);
		LocalDate end = (endDate != null) ? LocalDate.parse(endDate) : LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
		
		boolean isAdmin = loginDTO.getEmpRoles().stream()
		        .anyMatch(r -> r.getRole().getRoleCode().equals("ROLE_SYS_ADMIN"));
		
		// 권한 확인 후 권한이 없을 경우 메인 페이지로 이동(accessList에서 메인페이지로 이동하도록 처리)
		if (!isAdmin) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
		            .body(Map.of("msg", "접근 권한이 없습니다."));
		}
		
		List<AccessLogDTO> AccessLogDTOList = attendanceService.getAccessLogList(start, end);
		
		return ResponseEntity.ok(AccessLogDTOList);
	}
	
	// 출퇴근 등록 및 수정하는 모달창에서 사용할 정책 데이터 API
	@GetMapping("/policy/data")
	public ResponseEntity<Map<String, String>> getMethodName() {
		WorkPolicyDTO workPolicyDTO = attendanceService.getWorkPolicy();
		
		return ResponseEntity.ok(Map.of(
				"startTime", workPolicyDTO.getInTime(),
				"endTime", workPolicyDTO.getOutTime()
		));
	}
	
	// 외근 현황 페이지
	@GetMapping("/outwork")
	public String outworkList() {
		return "attendance/outwork";
	}
	
	// 외근 현황 조회
	@GetMapping("/outwork/data")
	public ResponseEntity<List<AccessLogDTO>> getOutwork(
			@AuthenticationPrincipal LoginDTO loginDTO, 
			@RequestParam(required = false, name = "startDate") String startDate,  
			@RequestParam(required = false, name = "endDate") String endDate) {
		
		LocalDate start = (startDate != null) ? LocalDate.parse(startDate) : LocalDate.now().withDayOfMonth(1);
		LocalDate end = (endDate != null) ? LocalDate.parse(endDate) : LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
		
		List<AccessLogDTO> outworkList = attendanceService.getAllOutwork(start, end, loginDTO.getEmpId());
		
		return ResponseEntity.ok(outworkList);
	}
}
