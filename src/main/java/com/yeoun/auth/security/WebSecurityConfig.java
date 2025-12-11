package com.yeoun.auth.security;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity	// 스프링 시큐리티 기능 설정 클래스로 지정
@RequiredArgsConstructor
public class WebSecurityConfig {
	
	private final CustomUserDetailsService customuserDetailsService;
	private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
	
	// ====================================================================
	// 스프링 시큐리티 보안 필터 설정
	// => 리턴타입이 SecurityFilterChain 타입을 리턴하는 메서드여야 함
	// => 메서드 파라미터에 HttpSecurity 타입을 선언하여 보안 처리용 객체를 자동 주입
	@Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
		// HttpSecurity 객체의 다양한 메서드를 메서드 체이닝 형태로 호출하여 스프링 시큐리티 관련 설정을 수행하고
		// 마지막에 build() 메서드 호출하여 HttpSecurity 객체 생성하여 리턴
		return httpSecurity
				
				// --------- 세션이 유효하지 않을 때(만료/서버 재시작 등) 이동할 URL -------
				.sessionManagement(session -> session
	                    .invalidSessionUrl("/login?session=expired")
	            )
				
				// --------- 요청에 대한 접근 허용 여부 등의 요청 경로에 대한 권한 설정 -------
				.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
				    // 공통: 정적 리소스 및 로그인/회원가입 등 완전 공개 구역
					.requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
					.requestMatchers("/assets/**", "/css/**", "/custom_bg/**", "/icon/**", "/js/**").permitAll()
					.requestMatchers("/", "/login", "/logout").permitAll()
					
					// ================== 로그인 한 모든 사원 ==================
		            .requestMatchers("/orgchart/**", "/hr/actions", "/attendance/my/**", "/leave/my/**", "/attendance/outwork", "/attendance/toggle/**",
		            				 "/my/**")
		                .authenticated()

					// ================== 관리자/인사/MES 권한 ==================
		            // 시스템 관리자만 가능
		            .requestMatchers("/auth/**")
		            	.hasAnyRole("SYS_ADMIN")
		                
		            // 인사관리 - 관리자 및 인사팀, 부서장
	                .requestMatchers("/emp")
	                    .hasAnyRole("SYS_ADMIN", "HR_ADMIN", "DEPT_MANAGER")

	                // 인사관리 - 관리자 및 인사팀
	                .requestMatchers("/emp/regist/**", "/emp/edit/**", "/hr/actions/**")
	                    .hasAnyRole("SYS_ADMIN", "HR_ADMIN")

		           
	                // 근태 관리
		            // 근태 관리 - 관리자 및 근태 담당자, 부서장
	                .requestMatchers("/attendance/list/**", "attendance/search", "attendance/*")
                    	.hasAnyRole("SYS_ADMIN", "DEPT_MANAGER", "ATTEND_ADMIN")
                    // 근태 관리 - 관리자 및 근태 담당자
                    .requestMatchers("/attendance/policy/**", "/leave/list/**", "leave/*")
                    	.hasAnyRole("SYS_ADMIN", "ATTEND_ADMIN")
                	// 근태 관리 - 관리자
                	.requestMatchers("/attendance/accessList/**")
                    	.hasAnyRole("SYS_ADMIN")
                    	
	                // 급여 관리
                    	// 사원용 급여명세서
                    	.requestMatchers("/pay/emp_pay", "/pay/emp_pay/**" ,"/pay/pdf/**")
                    	    .authenticated()

                    	// 급여 관리자 페이지
                    	.requestMatchers("/pay/rule/**", "/pay/rule_calc/**", "/pay/rule_item/**", "/pay/calc/**", "/pay/history/**", "/pay/**" )
                       	.hasAnyRole("SYS_ADMIN")
                    	
                    	
	                // 전자결재 설정(양식/결재선 관리 등)
	                // 전자결재 일반 사용 (결재 상신/조회 등)
	                // 공지 관리
	                // MES 관리자
	                // MES 일반 사용자
						.requestMatchers("/order/**")
						.permitAll()

	                // 그 외 나머지는 로그인만 되어있으면 접근 허용
	                .anyRequest().authenticated()
				 )
				// ---------- 로그인 처리 설정 ---------
				.formLogin(login -> login
					.loginPage("/login") 			// 로그인 폼 요청에 사용할 URL 지정(컨트롤러에서 매핑 처리)
					.loginProcessingUrl("/login") 	// 로그인 폼에서 제출된 데이터 처리용 요청 주소(자동으로 POST 방식으로 처리됨)
					.usernameParameter("empId") 	// 로그인 과정에서 사용할 사용자명(username)을 사원번호(empId)로 지정(기본값 : username)
					.passwordParameter("empPwd") 	// 로그인 과정에서 사용할 패스워드 지정(기본값 : password)
					.successHandler(customAuthenticationSuccessHandler)  // 로그인 성공 시 별도의 추가 작업을 처리할 핸들러 지정
					.failureUrl("/login?error")		// 로그인 실패 시 리다이렉트
					.permitAll() 					// 로그인 경로 관련 요청 주소를 모두 허용
				) 
				// ---------- 로그아웃 처리 설정 ---------
				.logout(logout -> logout
					.logoutUrl("/logout") 				// 로그아웃 요청 URL 지정(POST 방식 요청으로 취급함)
					.logoutSuccessUrl("/login?logout") 	// 로그아웃 성공 후 리디렉션 할 URL 지정
					.invalidateHttpSession(true)		// 로그아웃 시 세션을 무효화(세션 데이터 모두 삭제)
					.deleteCookies("JSESSIONID", "remember-me")  // 로그아웃 시 삭제할 쿠키 지정 (JESSIONID = 세션 ID를 담고 있는 기본 쿠키)
					.clearAuthentication(true) 			// 로그아웃 시 인증 정보를 완전히 제거
					.permitAll()
				)
				// ---------- 접근 권한 오류 --------------
				.exceptionHandling(ex -> ex
					// 인증은 됐는데(로그인 O) 권한이 없을 때 → 403
					.accessDeniedHandler((request, response, e) -> {
						response.sendRedirect("/error/403");
					})
				)
				// ---------- 자동 로그인 처리 설정 ----------
				.rememberMe(rememberMeCustormizer -> rememberMeCustormizer
					.rememberMeParameter("remember-me") 		// 자동 로그인 수행을 위한 체크박스 파라미터명 지정(체크 여부 자동으로 판별)
					.key("my-fixed-secret-key") 				// 서버 재시작해도 이전 로그인에서 사용했던 키 동일하게 사용
					.tokenValiditySeconds(60 * 60 * 24 * 30) 	// 자동 로그인 토큰 유효기간 설정(30일)
					.userDetailsService(customuserDetailsService) 
					.authenticationSuccessHandler(customAuthenticationSuccessHandler)
				)
				.build();
    }

	// ====================================================================
	// 패스워드 인코더로 사용할 빈 등록
	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
}