package com.yeoun.messenger.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.yeoun.common.dto.FileAttachDTO;
import com.yeoun.messenger.dto.*;
import com.yeoun.messenger.entity.MsgStatus;
import com.yeoun.messenger.repository.MsgStatusRepository;
import com.yeoun.messenger.support.RoomNameGenerator;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.messenger.entity.MsgMessage;
import com.yeoun.messenger.entity.MsgRelation;
import com.yeoun.messenger.entity.MsgRoom;
import com.yeoun.messenger.repository.MsgMessageRepository;
import com.yeoun.messenger.repository.MsgRelationRepository;
import com.yeoun.messenger.repository.MsgRoomRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class ChatService {
	
	private final SimpMessagingTemplate simpMessagingTemplate;
	private final MsgRelationRepository msgRelationRepository;
	private final MsgMessageRepository msgMessageRepository;
	private final EmpRepository empRepository;
	private final MsgRoomRepository msgRoomRepository;
	private final MsgStatusRepository msgStatusRepository;
	private final RoomNameGenerator roomNameGenerator;
	private final RoomMemberQueryService roomMemberQueryService;

	// 1) 메시지 전송 처리
	// DB 저장 + 해당 방 참여자들에게 broadcast
	@Transactional
	public void broadcastMessage(MsgMessage saved, List<FileAttachDTO> files) {

		boolean isFile = (files != null && !files.isEmpty());

		MsgStatus status = msgStatusRepository.findById(saved.getSenderId().getEmpId())
				.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다...."));

		MessageEventDTO event = MessageEventDTO.builder()
				.msgId(saved.getMsgId())
				.roomId(saved.getRoomId().getRoomId())
				.senderId(saved.getSenderId().getEmpId())
				.senderName(saved.getSenderId().getEmpName())
				.senderProfile(status.getMsgProfile())		// 찾으면 수정 예정....
				.msgType(saved.getMsgType())
				.msgContent(saved.getMsgContent())
				.fileCount(isFile ? files.size() : 0)
				.files(files)
				.preview(isFile ? "(파일)" : saved.getMsgContent())
				.sentTime(saved.getSentDate()
						.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
				.build();

		log.info("브로드캐스트 dto....... 왜 안나오니... :: " + event);

		 // 메시지 전송 1) (방을 대상으로)
		 simpMessagingTemplate.convertAndSend(
				   "/topic/chat/room/" + saved.getRoomId().getRoomId(),
				   event
		 );
		 
		log.info("************** STOMP! 메시지 전송" + event);
		
		// =======================================================================
		
		List<String> members = new ArrayList<>();
		List<MsgRelation> relationList = msgRelationRepository.findByRoomId_RoomId(event.getRoomId());
		for (MsgRelation relation : relationList) {
			members.add(relation.getEmpId().getEmpId());
		}
		members.remove(event.getSenderId());
		
		for (String member : members) {
			int unread = msgMessageRepository.countUnreadMessage(event.getRoomId(), member, event.getMsgId());
			MessageNotifyDTO notify = MessageNotifyDTO.builder()
					.roomId(event.getRoomId())
					.preview(event.getMsgContent())
					.senderId(event.getSenderId())
					.senderName(empRepository.findById(event.getSenderId()).orElseThrow(() -> new RuntimeException("오류 발생"))
							.getEmpName())
					.sentTime(event.getSentTime())
					.unreadCount(unread)
					.profileImg(msgStatusRepository.findById(event.getSenderId()).orElseThrow(() -> new RuntimeException("오류 발생"))
							.getMsgProfile())
					.groupName(roomNameGenerator.create(event.getRoomId(), event.getSenderName(),
							roomMemberQueryService.getMembers(event.getRoomId())))
					.build();
			
			// 메시지 전송 2) (외부를 대상으로)
			simpMessagingTemplate.convertAndSendToUser(
					member, 
					"/queue/messenger", 
					notify
			);
		}
		
	}
	
	
	// 2) 메시지 읽음 처리
	public void readMessage(MsgReadRequest msgReadRequest) {
		if (msgReadRequest.getGroupYn().equals("Y")) return;

		simpMessagingTemplate.convertAndSend(
				"/topic/chat/room/" + msgReadRequest.getRoomId() + "/read",
				msgReadRequest
		);
		
		log.info("************** STOMP! 메시지 읽음" + msgReadRequest);
	}
	
	
	// 3) 상태 변경 처리
	public void changeStatus(StatusChangeRequest statusChangeRequest) {
		log.info("changeStatus 진입......... EMPID	  ::: " + statusChangeRequest.getEmpId());
		log.info("changeStatus 진입......... AVLBSTAT ::: " + statusChangeRequest.getAvlbStat());
		log.info("changeStatus 진입......... WORKSTAT ::: " + statusChangeRequest.getWorkStat());

		simpMessagingTemplate.convertAndSend(
				"/topic/status/change",
				statusChangeRequest);
		
		log.info("************** STOMP! 상태 변경" + statusChangeRequest);
	}
	
	// 4) 방 퇴장 알림
	public void leaveRoom(RoomLeaveRequest roomLeaveRequest) {
		
		// 엔티티 조회
		MsgRoom room = msgRoomRepository.getReferenceById(roomLeaveRequest.getRoomId());
		Emp sender = empRepository.findByEmpId(roomLeaveRequest.getEmpId())
				.orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없음"));

		// SYSTEM 메시지는 직접 엔티티로 저장하는게 최적
		MsgMessage systemMsg = MsgMessage.builder()
				.room(room)
				.sender(sender)
				.msgType("SYSTEM")
				.msgContent(sender.getEmpName() + "님이 퇴장했습니다.")
				.build();

		msgMessageRepository.save(systemMsg);
	
		// 메시지 전송
		simpMessagingTemplate.convertAndSend(
				"/topic/chat/room/" + roomLeaveRequest.getRoomId() + "/leave",
				roomLeaveRequest
		);
		
		log.info("************** STOMP! 방 퇴장" + roomLeaveRequest);
	}
	


}







