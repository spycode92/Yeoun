package com.yeoun.messenger.controller;

import com.yeoun.messenger.dto.*;
import com.yeoun.messenger.service.MessengerService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.yeoun.messenger.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
@Log4j2
public class ChatController {
	
	private final ChatService chatService;
	private final MessengerService messengerService;


	// ====================================================
	// 클라이언트로부터 수신되는 웹소켓 채팅 메세지(/topic/xxx)를 전달받아 처리할 매핑 작업
	// => 클라이언트에서 서버측으로 "/topic/chat/send" 주소로 전송할 경우
	//	  컨트롤러 측에서 @MessageMapping("xxx") 형태로 매핑 메서드 작성
	// 	  이 때, 경로 변수처럼 바인딩이 필요할 때 @PathVariable 대신 @DestinationVariable 어노테이션 사용(방법은 동일)
	// => 수신된 메세지를 다시 다른 클라이언트들에게 전송할 경우 @SendTo 어노테이션 활용
	
	// ====================================================
	// 1. 메시지 전송 
	// 클라이언트 -> app/chat/send
	@MessageMapping("/chat/send")
	public void sendMessage (MsgSendRequest msgSendRequest) throws IOException {

		// 1) DB 저장 (텍스트)
		MessageSaveResult result = messengerService.saveMessage(msgSendRequest, null);

		// 2) 실시간 전송
		chatService.broadcastMessage(result.getMessage(), result.getFiles());
	}
	
	// ====================================================
	// 2. 메시지 읽음
	// 클라이언트 -> app/chat/read
	@MessageMapping("/chat/read")
	public void readMessage(MsgReadRequest msgReadRequest) {

		// 1) DB 저장
		messengerService.updateLastRead(msgReadRequest);

		// 2) 실시간 전송
		chatService.readMessage(msgReadRequest);
	}
	
	// ====================================================
	// 3. 상태 변경 처리
	// 클라이언트 -> app/status/change
	@MessageMapping("/status/change")
	public void changeStatus(StatusChangeRequest statusChangeRequest) {

		// 1) DB 저장 =====> 메모리 or Redis 변환 예정
		messengerService.updateStatus(statusChangeRequest);

		// 2) 실시간 전송
		chatService.changeStatus(statusChangeRequest);
	}
	
	// ====================================================
	// 4. 방 퇴장 알림
	// 클라이언트 -> app/room/leave
	@MessageMapping("/room/leave")
	public void leaveRoom(RoomLeaveRequest roomLeaveRequest) {

		// 1) DB 저장
		messengerService.exitRoom(roomLeaveRequest);

		// 2) 실시간 전송
		chatService.leaveRoom(roomLeaveRequest);
	}
	

}






