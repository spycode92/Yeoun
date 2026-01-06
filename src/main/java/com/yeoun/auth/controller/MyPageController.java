package com.yeoun.auth.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.auth.dto.PasswordChangeDTO;
import com.yeoun.common.service.CommonCodeService;
import com.yeoun.emp.dto.EmpDTO;
import com.yeoun.emp.dto.EmpDetailDTO;
import com.yeoun.emp.entity.Dept;
import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.repository.DeptRepository;
import com.yeoun.emp.repository.PositionRepository;
import com.yeoun.emp.service.EmpService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/my")
@RequiredArgsConstructor
public class MyPageController {
	
	private final EmpService empService;
    private final BCryptPasswordEncoder encoder;
    private final DeptRepository deptRepository;
    private final PositionRepository positionRepository;
    private final CommonCodeService commonCodeService;

    // 1.  내 정보 JSON (모달용)
    @GetMapping("/info")
    @ResponseBody
    public EmpDetailDTO getMyInfo(@AuthenticationPrincipal LoginDTO loginUser) {

        return  empService.getEmpDetail(loginUser.getEmpId());
    }
    
	// 내 정보 수정
	@GetMapping("/info/edit")
	public String editMyInfo(@AuthenticationPrincipal LoginDTO loginUser,
							 Model model) {
		
		EmpDTO empDTO = empService.getEmpForEdit(loginUser.getEmpId());
		
		model.addAttribute("empDTO", empDTO);
		model.addAttribute("mode", "edit");
		model.addAttribute("formAction", "/my/info/update");
		
		// 마이페이지용 타이틀/플래그
	    model.addAttribute("pageTitle", "내 정보 관리");
	    model.addAttribute("isMyPage", true);
		
		// 마이페이지에서 급여정보 수정 불가
		model.addAttribute("canEditBankInfo", false);
		
		// --- 조직/직무 셀렉트 공통 세팅 (EmpController.setupEmpFormCommon 과 동일) ---
        List<Dept> topDeptList =
                deptRepository.findByParentDeptIdAndUseYn("DEP999", "Y");

        List<Dept> subDeptList =
                deptRepository.findByParentDeptIdIsNotNullAndParentDeptIdNotAndUseYn("DEP999", "Y");
		
		// emp_form.html에서 필요한 공통 select box 세팅
        model.addAttribute("topDeptList", topDeptList);
        model.addAttribute("subDeptList", subDeptList);
	    model.addAttribute("positionList", positionRepository.findActive());
	    model.addAttribute("bankList", commonCodeService.getBankList());
		
		return "emp/emp_form";
	}
	
	@PostMapping("/info/update")
	public String updateMyInfo(@AuthenticationPrincipal LoginDTO loginUser,
	                           @ModelAttribute("empDTO") @Validated(EmpDTO.Edit.class) EmpDTO empDTO,
	                           BindingResult bindingResult,
	                           Model model,
	                           RedirectAttributes rttr) {

		// 로그인한 본인 아이디 강제 세팅
	    empDTO.setEmpId(loginUser.getEmpId()); 
	    
	    // 원본 DTO 한 번 조회 (기존 사진 / 통장 사본 유지용)
	    EmpDTO original = empService.getEmpForEdit(loginUser.getEmpId());
	    
	    // 주민번호 마스킹, 파일 id 등 원본 기준으로 복원
        empDTO.setRrnMasked(original.getRrnMasked());
        empDTO.setPhotoFileId(original.getPhotoFileId());
        empDTO.setFileId(original.getFileId()); 
        
        // 새 통장사본 업로드 들어와도 무시
//        empDTO.setBankbookFile(null);
	    
	    // 1) Bean Validation 실패 폼 다시 보여주기
	    if (bindingResult.hasErrors()) {
	    	
	        model.addAttribute("empDTO", empDTO);
	        model.addAttribute("mode", "edit");
	        model.addAttribute("formAction", "/my/info/update");
	        
	        model.addAttribute("pageTitle", "내 정보 관리");
	        model.addAttribute("isMyPage", true);
	        
            List<Dept> topDeptList =
                    deptRepository.findByParentDeptIdAndUseYn("DEP999", "Y");
            List<Dept> subDeptList =
                    deptRepository.findByParentDeptIdIsNotNullAndParentDeptIdNotAndUseYn("DEP999", "Y");

            model.addAttribute("topDeptList", topDeptList);
            model.addAttribute("subDeptList", subDeptList);
	        model.addAttribute("positionList", positionRepository.findActive());
	        model.addAttribute("bankList", commonCodeService.getBankList());

	        return "emp/emp_form";
	    }
	    
	    // 2) 중복 검사 등 비즈니스 에러 처리
	    try {
	        empService.updateEmp(empDTO);
	    } catch (IllegalStateException e) {

	        String msg = e.getMessage();

	        if (msg.contains("이메일")) {
	            bindingResult.rejectValue("email", "duplicate", msg);
	        } else if (msg.contains("연락처")) {
	            bindingResult.rejectValue("mobile", "duplicate", msg);
	        } else {
	            bindingResult.reject("empEditError", msg);
	        }

	        empDTO.setRrnMasked(original.getRrnMasked());
	        empDTO.setPhotoFileId(original.getPhotoFileId());
	        empDTO.setFileId(original.getFileId());

	        model.addAttribute("empDTO", empDTO);
	        model.addAttribute("formAction", "/my/info/update");
	        model.addAttribute("mode", "edit");
	        model.addAttribute("pageTitle", "내 정보 관리");
	        model.addAttribute("isMyPage", true);
	        model.addAttribute("canEditBankInfo", false);
	        
	        List<Dept> topDeptList =
	                deptRepository.findByParentDeptIdAndUseYn("DEP999", "Y");
	        List<Dept> subDeptList =
	                deptRepository.findByParentDeptIdIsNotNullAndParentDeptIdNotAndUseYn("DEP999", "Y");
	        
	        model.addAttribute("topDeptList", topDeptList);
            model.addAttribute("subDeptList", subDeptList);
	        model.addAttribute("positionList", positionRepository.findActive());
	        model.addAttribute("bankList", commonCodeService.getBankList());

	        return "emp/emp_form";
	    }

	    // 3) 성공
	    rttr.addFlashAttribute("msg", "내 정보가 수정되었습니다.");
	    return "redirect:/main";  
	}

    // 2. 비밀번호 변경 폼 
    @GetMapping("/password")
    public String changePasswordForm(@AuthenticationPrincipal LoginDTO login, Model model) {
    	if (!model.containsAttribute("passwordChangeDTO")) {
            model.addAttribute("passwordChangeDTO", new PasswordChangeDTO());
        }
    	
    	// 초기 비밀번호인지(Y) 여부 확인
    	String empId = login.getEmpId();
    	 
    	Emp emp = empService.getEmpEntity(empId);
    	boolean forceChange = "Y".equals(emp.getPwdChangeReq());
    	 
    	model.addAttribute("forceChange", forceChange);
    	 
        return "auth/password_change";   
    }

    // 3. 비밀번호 변경 처리 
    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("passwordChangeDTO") PasswordChangeDTO dto,
                                 BindingResult bindingResult,
                                 Authentication auth,
                                 RedirectAttributes rttr,
                                 Model model) {
    	
        LoginDTO login = (LoginDTO) auth.getPrincipal();
        String empId = login.getEmpId();
        
        // 1) 폼 검증 에러
        if (bindingResult.hasErrors()) {
            return "auth/password_change";
        }

        // 2) 새 비밀번호/확인 일치 확인
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "새 비밀번호와 확인이 일치하지 않습니다.");
            return "auth/password_change";
        }

        // 3) 현재 비밀번호 확인
        String currentEncodedPwd = login.getEmpPwd();   
        
        if (!encoder.matches(dto.getCurrentPassword(), currentEncodedPwd)) {
            bindingResult.rejectValue("currentPassword", "invalid", "현재 비밀번호가 올바르지 않습니다.");
            return "auth/password_change";
        }
        
        // 4) 현재 비밀번호와 새 비밀번호 일치 확인
        if (encoder.matches(dto.getNewPassword(), currentEncodedPwd)) {
        	bindingResult.rejectValue("newPassword", "sameAsCurrent", "현재 사용 중인 비밀번호와 다른 비밀번호를 입력해 주세요.");
        	return "auth/password_change";
        }

        // 5) 실제 비밀번호 변경
        empService.changePassword(empId, dto.getNewPassword());
        
        // 성공 메시지 + 로그아웃 플래그를 플래시 속성으로 전달
        rttr.addFlashAttribute("successMessage", "비밀번호가 변경되었습니다. 다시 로그인해주세요.");
        rttr.addFlashAttribute("logoutAfterChange", true);

        // 같은 페이지로 리다이렉트
        return "redirect:/my/password";
    }

}
