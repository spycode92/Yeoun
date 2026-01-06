package com.yeoun.auth.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.repository.EmpRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 스프링 시큐리티 로그인 성공 시 추가 작업을 처리하는 핸들러 정의
@Log4j2
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
	
	private final EmpRepository empRepository;
	
	// 로그인 성공 시 별도의 추가 작업을 onAuthenticationSuccess() 메서드 오버라이딩을 통해 처리
	@Override
	@Transactional
	public void onAuthenticationSuccess(HttpServletRequest request, 
										HttpServletResponse response,
										Authentication authentication) throws IOException, ServletException {
		
//		log.info(">>>>> authentication.getName() : " + authentication.getName()); 	// 사용자명(username = 현재는 empId 사용)
		
		// --------------------- 마지막 로그인 시간 업데이트 --------------------- 
		String empId = authentication.getName();
		
		Emp emp = empRepository.findById(empId)
							.orElseThrow(() -> new IllegalStateException("로그인 성공 후 사원 조회 실패 : " + empId));
		
		emp.setLastLogin(LocalDateTime.now());
		empRepository.save(emp);
		
		// ======================= 사원번호 저장 쿠키 ======================= 
		// 1. 사원번호 저장 체크박스 파라미터값 가져오기
		String rememberId = request.getParameter("remember-id");
//		log.info("▶▶▶▶▶▶▶▶▶▶ rememberId : " + rememberId); // null 또는 "on"
		
		// 2. 쿠키 생성 공통 코드
		// 2-1) Cookie 객체 생성하여 "remember-id" 라는 이름으로 사용자명(empId) 저장
		// 만약, 한글 등의 값이 포함된 문자열일 경우 인코딩 필요
		Cookie cookie = new Cookie("remember-id", URLEncoder.encode(authentication.getName(), StandardCharsets.UTF_8));
		// 2-2) 쿠키 사용 경로 설정
		cookie.setPath("/"); // 현재 서버 애플리케이션 내에서 모든 경로 상에서 해당 쿠키 사용이 가능하도록 설정
		
		// 2-3) 사원번호 저장 체크박스 값에 따른 처리
		if(rememberId != null && rememberId.equals("on")) { // 체크박스 체크 시
 			// 쿠키 정보 설정 => 쿠키 유효기간을 설정하여 쿠키 사용 가능하도록 처리
			cookie.setMaxAge(60 * 60 * 24 * 7);
		} else { // 체크박스 체크 해제 시
			// 쿠키 삭제를 위해 유효기간을 0초로 설정 = 클라이언트가 해당 쿠키 정보를 수신하는 "즉시" 쿠키 삭제
			cookie.setMaxAge(0);
		}
		
		// 2-4) 응답 객체에 쿠키 추가
		response.addCookie(cookie);
		
		// ======================= 이동할 URL ======================= 
		// 기본값: 메인 화면
		String targetUrl = request.getContextPath() + "/main";
		
		// 비밀번호 변경 강제 대상이면 비밀번호 변경 화면으로 이동
		if ("Y".equals(emp.getPwdChangeReq())) {
//			log.info("[비밀번호 변경 필요] empId={} → /my/password로 리다이렉트", empId);
			targetUrl = request.getContextPath() + "/my/password";
		}
		
		// =====================================================================================
		// 로그인 성공 시 이동할 페이지로 리다이렉트 처리
		response.sendRedirect(targetUrl);
	}
	
	
}
