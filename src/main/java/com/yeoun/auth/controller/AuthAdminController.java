package com.yeoun.auth.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yeoun.auth.service.AuthAdminService;
import com.yeoun.emp.dto.EmpListDTO;
import com.yeoun.emp.repository.DeptRepository;
import com.yeoun.emp.service.EmpService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

// 관리자 - 접근권한 부여 및 비밀번호 초기화
@Controller
@RequestMapping("/auth/manage")
@RequiredArgsConstructor
public class AuthAdminController {
	
	private final AuthAdminService authAdminService;
    private final DeptRepository deptRepository;
    private final EmpService empService;
    private final AuthenticationManager authenticationManager;
	
	// 권한관리 메인 화면
	@GetMapping("")
	public String managePage(Model model) {
		
		model.addAttribute("deptList", deptRepository.findActive());
		model.addAttribute("roleList", authAdminService.getAllRoles());
		
		return "auth/manage";
	}
	
	// 사원 목록 데이터 API
	@GetMapping("/data")
	@ResponseBody
	public Map<String, Object> getEmpData(@RequestParam(required = false, name = "keyword") String keyword,
										  @RequestParam(required = false, name = "deptId") String deptId) {
		
		List<EmpListDTO> list = authAdminService.getEmpListForAuth(deptId, keyword);
		
		Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", 1);
        pagination.put("totalCount", list.size());

        Map<String, Object> data = new HashMap<>();
        data.put("contents", list);
        data.put("pagination", pagination);

        Map<String, Object> result = new HashMap<>();
        result.put("result", true);
        result.put("data", data);
		
		return result;
	}
	
	// 사원 현재 역할 목록
	@GetMapping("/{empId}/roles")
	@ResponseBody
	public List<String> getRoles(@PathVariable("empId") String EmpId) {
		return authAdminService.getRoleCodesByEmp(EmpId);
	}
	
	// 새로운 역할 저장
	@PostMapping("/save")
	public String saveRoles(@RequestParam("empId") String empId,
							@RequestParam(required = false, name = "roleCodes") List<String> roleCodes,
							RedirectAttributes rttr,
							HttpSession session) {
		
		// ✅ 최근 비밀번호 재확인 체크 (예: 5분)
	    Long t = (Long) session.getAttribute("AUTHZ_VERIFIED_AT");
	    long now = System.currentTimeMillis();
	    if (t == null || now - t > 5 * 60 * 1000L) {
	        rttr.addFlashAttribute("msg", "관리자 비밀번호 확인이 필요합니다.");
	        return "redirect:/auth/manage";
	    }
		
		authAdminService.updateEmpRoles(empId, roleCodes);
		rttr.addFlashAttribute("msg", "권한이 저장되었습니다.");
		return "redirect:/auth/manage";
	}
	
	// ==================================
	// 비밀번호를 초기값(1234)으로 재설정
	@PostMapping("/emp/{empId}/pwdResetDefault")
	@PreAuthorize("hasAnyRole('SYS_ADMIN', 'HR_ADMIN')")
	public String resetPwdToDefault(@PathVariable("empId") String empId,
								    RedirectAttributes rttr) {
		
		empService.resetPwdToDefaultByAdmin(empId);
		
		rttr.addFlashAttribute("msg", "비밀번호를 초기값(1234)으로 재설정했습니다.");
		rttr.addFlashAttribute("infoMsg", "사원에게 초기 비밀번호(1234)로 로그인 후 반드시 변경하도록 안내해 주세요.");
		
		
		return "redirect:/emp/edit/" + empId;
		
	}
	
	// ======================= 접근 권한 보완 ========================
	@PostMapping("/verify")
	@ResponseBody
	@PreAuthorize("hasRole('SYS_ADMIN')")
	public Map<String, Object> verifyAdminPassword(@RequestParam("password") String password,
	                                               Authentication auth,
	                                               HttpSession session) {
	    try {
	        authenticationManager.authenticate(
	            new UsernamePasswordAuthenticationToken(auth.getName(), password)
	        );

	        session.setAttribute("AUTHZ_VERIFIED_AT", System.currentTimeMillis());

	        return Map.of("success", true);
	    } catch (AuthenticationException e) {
	        return Map.of("success", false, "message", "비밀번호가 올바르지 않습니다.");
	    }
	}

	

} // AuthAdminController 끝

