package com.yeoun.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	// WebSocketMessageBrokerConfigurer 인터페이스의 추상메서드 2개 오버라이딩 (registerStompEndpoints, configureMessageBroker)
	
	// =========================================================================
	// 1) 웹소켓 클라이언트가 연결할 엔드포인트 등록
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/websocket")	// 클라이언트에서 웹소켓 연결 요청 주소 등록하여 엔드포인트 연결 (new SockJS("/ws") 형태로 설정된 주소)
        		.setAllowedOrigins(
        		"http://c4d2510t1p1.itwillbs.com",
        		"http://localhost:8080"
        		)  
                .withSockJS();	// SockJS의 Fallback 기능 활성화
    }
	// => 이 메서드 정의 시 기본적인 웹소켓 연결 처리 완료
    
	// =========================================================================
	// 2) 메세지 전송을 처리할 메세지 브로커 설정
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
    	// 클라이언트가 "/pub/xxx" 형태로 전송한 메세지를 @MessagingMapping 어노테이션으로 라우팅(전달)
        registry.setApplicationDestinationPrefixes("/app");
        // 메세지 브로커(서버측)가 "/sub/xxx" 형태로 발행한 메세지를 구독자(xxx)에게 전달하기 위한 토픽 주소를 브로커로 지정
        registry.enableSimpleBroker("/topic", "/queue", "/alarm");
    }
}
