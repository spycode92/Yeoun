package com.yeoun.messenger.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

import com.yeoun.messenger.dto.*;
import com.yeoun.messenger.entity.MsgMessage;
import com.yeoun.messenger.entity.MsgRelation;
import com.yeoun.messenger.entity.MsgRoom;
import com.yeoun.messenger.entity.MsgStatus;
import com.yeoun.messenger.repository.MsgMessageRepository;
import com.yeoun.messenger.repository.MsgRelationRepository;
import com.yeoun.messenger.repository.MsgRoomRepository;
import com.yeoun.messenger.repository.MsgStatusRepository;

import com.yeoun.messenger.support.HighlightGenerator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.yeoun.common.dto.FileAttachDTO;
import com.yeoun.common.repository.FileAttachRepository;
import com.yeoun.common.util.FileUtil;
import com.yeoun.emp.entity.Dept;
import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.entity.Position;
import com.yeoun.emp.repository.DeptRepository;
import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.emp.repository.PositionRepository;
import com.yeoun.messenger.entity.MsgFavorite;
import com.yeoun.messenger.mapper.MessengerMapper;
import com.yeoun.messenger.repository.MsgFavoriteRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class MessengerService {

	private final ChatService chatService;
	private final MessengerMapper messengerMapper;
	private final MsgRoomRepository msgRoomRepository;
	private final MsgStatusRepository msgStatusRepository;
	private final MsgMessageRepository msgMessageRepository;
	private final MsgFavoriteRepository msgFavoriteRepository;
	private final MsgRelationRepository msgRelationRepository;

	private final FileUtil fileUtil;
	private final EmpRepository empRepository;
	private final DeptRepository deptRepository;
	private final PositionRepository positionRepository;
	private final FileAttachRepository fileAttachRepository;
	private final HighlightGenerator highlightGenerator;


	// ====================================================
	// 친구 목록을 불러오는 서비스
	public List<MsgStatusDTO> getUsers(String username) {

		// 메신저 내 최종 상태를 결정
		List<MsgStatusDTO> dtoList = messengerMapper.selectUsers(username);
		for (int i = 0; i < dtoList.size(); i++) {
			if(dtoList.get(i).getOnlineYn().equals("N")){
				dtoList.get(i).setStatus("OFFLINE");	// 오프라인
			} else if (dtoList.get(i).getAvlbStat().equals("ONLINE")){
				dtoList.get(i).setStatus("ONLINE");		// 온라인
			} else if (dtoList.get(i).getAvlbStat().equals("AWAY")){
				dtoList.get(i).setStatus("AWAY");		// 자리비움
			} else {
				dtoList.get(i).setStatus("BUSY");		// 다른 용무중
			}
		}

		return dtoList;
	}

	// ====================================================
	// 대화 목록을 불러오는 서비스
	public List<MsgRoomListDTO> getChatRooms (String username) {
		return messengerMapper.selectChats(username);
	}

	// ========================================================
	// 즐겨찾기 추가
	public void createFavorite(MsgFavoriteDTO msgFavoriteDTO) {
		
		log.info("★★★★★★★★★★★★★★★★★★★★★★★ 즐겨찾기 추가 진입....");
		
		Emp empId = empRepository.findByEmpId(msgFavoriteDTO.getEmpId())
				.orElseThrow(() -> new RuntimeException("해당 유저를 찾을 수 없습니다"));
		Emp fvUser = empRepository.findByEmpId(msgFavoriteDTO.getFvUser())
				.orElseThrow(() -> new RuntimeException("해당 유저를 찾을 수 없습니다"));
		
		MsgFavorite msgFavorite = msgFavoriteDTO.toEntity(empId, fvUser);
		msgFavoriteRepository.save(msgFavorite);
	}

	// ========================================================
	// 즐겨찾기 여부 확인
	public boolean searchFavorite(MsgFavoriteDTO msgFavoriteDTO) {
		log.info("★★★★★★★★★★★★★★★★★★★★★★★ 즐겨찾기 확인 진입....");
		return msgFavoriteRepository.existsByEmpId_EmpIdAndFvUser_EmpId(msgFavoriteDTO.getEmpId(), msgFavoriteDTO.getFvUser());
	}

	// ========================================================
	// 즐겨찾기 삭제
	@Transactional
	public boolean deleteFavorite(MsgFavoriteDTO msgFavoriteDTO) {
		log.info("★★★★★★★★★★★★★★★★★★★★★★★ 즐겨찾기 삭제 진입....");
		return msgFavoriteRepository
				.deleteByEmpId_EmpIdAndFvUser_EmpId(msgFavoriteDTO.getEmpId(), msgFavoriteDTO.getFvUser()) > 0;
	}


	// ========================================================
	// 1:1 방 생성 여부 확인
	public Long searchRoom(String authUser, String targetUser) {
		Long roomId = msgRelationRepository.findOneToOneRoom(authUser, targetUser);
		log.info(">>>>>>>>>>>>>>>>>>>>>> searchRoom... ID : " + roomId);
		return roomId != null ? roomId : 0;
	}

	// ========================================================
	// 메시지 보내기
	@Transactional
	public MessageSaveResult saveMessage(MsgSendRequest dto, List<MultipartFile> files) throws IOException {

		Emp sender = empRepository.findById(dto.getSenderId())
				.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

		MsgRoom room = msgRoomRepository.findById(dto.getRoomId())
				.orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));

		
		// 1) 메시지 저장
		MsgMessage message = MsgMessage.builder()
				.room(room)
				.sender(sender)
				.msgContent(dto.getMsgContent())
				.msgType(dto.getMsgType())
				.build();

		MsgMessage savedMessage = msgMessageRepository.save(message);
		log.info("이게 왜 안 될까? >>>>>>>>>>>>>" + room);
		log.info("이게 왜 안 될까? >>>>>>>>>>>>>" + sender);
		log.info("이게 왜 안 될까? >>>>>>>>>>>>>" + message);
		log.info("이게 왜 안 될까? >>>>>>>>>>>>>" + dto);
		List<MsgRelation> update = msgRelationRepository.findByRoomId_RoomId(room.getRoomId());
		if (update.size() == 2) {
			for (MsgRelation relation : update) {
				relation.setParticipantYn("Y");
			}
		}

		// 2) 파일 업로드
		List<FileAttachDTO> uploaded = new ArrayList<>();

		if (files != null && !files.isEmpty()) {
			uploaded = fileUtil.uploadFile(savedMessage, files);
			fileAttachRepository.saveAll(uploaded
					.stream()
					.map(FileAttachDTO::toEntity)
					.toList());
		}
		
		for (FileAttachDTO f : uploaded) {
			f.setFileId(fileAttachRepository.findByFileName(f.getFileName()).getFileId());
		}

		return new MessageSaveResult(savedMessage, uploaded);
	}

	// ========================================================
	// 새 방 생성
	@Transactional
	public MsgRoomDTO createRoom(RoomCreateRequest roomCreateRequestDTO, String id) throws IOException {
		////////////////////////////////// 파일추가 잊지말것...... /////////////////////////////////
		boolean hasText = roomCreateRequestDTO.getFirstMessage() != null
				&& !roomCreateRequestDTO.getFirstMessage().isBlank();

		log.info("roomCreateRequestDTO : " + roomCreateRequestDTO);

		// 1) 채팅방 생성
		MsgRoom newRoom = new MsgRoom();
		newRoom.setGroupYn(roomCreateRequestDTO.getGroupYn());
		newRoom.setGroupName(roomCreateRequestDTO.getGroupName());
		msgRoomRepository.save(newRoom);

		// 2) 참여자 relations 저장
		Set<String> memberIds = new HashSet<>(roomCreateRequestDTO.getMembers());
		memberIds.add(id);

		for (String empId : memberIds) {
			System.out.println("empId = [" + empId + "]");
			MsgRelation relation = new MsgRelation();
			relation.setRoomId(newRoom);
			relation.setEmpId(empRepository.getReferenceById(empId));
			relation.setPinnedYn("N");
			msgRelationRepository.save(relation);
		}

		// 3) 첫 메시지 전송
		if (hasText) { //|| hasFiles) {	==============> 파일추가 잊지 말것 ==================

			MsgSendRequest dto = new MsgSendRequest();
			dto.setRoomId(newRoom.getRoomId());
			dto.setSenderId(roomCreateRequestDTO.getCreatedUser());
			dto.setMsgContent(roomCreateRequestDTO.getFirstMessage());
			dto.setMsgType("TEXT");
			//dto.setMsgType(files != null && !files.isEmpty() ? "FILE" : "TEXT"); ==========> 파일추가 잊지 말것

			MessageSaveResult result = saveMessage(dto, null);	// 두번째 null 파일추가 잊지 말것
			MsgMessage saved = result.getMessage();
			//List<FileAttachDTO> uploaded = new ArrayList<>();

			// 소켓 broadcast
			chatService.broadcastMessage(saved, null); //uploaded);
		}

		MsgRoomDTO newRoomDTO = new MsgRoomDTO();
		newRoomDTO.setRoomId(newRoom.getRoomId());
		newRoomDTO.setGroupYn(newRoom.getGroupYn());
		newRoomDTO.setGroupName(newRoom.getGroupName());
		log.info("MsgRoomDTO : " + newRoomDTO);
		return newRoomDTO;
	}

	// ========================================================
	// 마지막으로 읽은 메시지 체크
	@Transactional
	public void updateLastRead(MsgReadRequest dto) {

		log.info("update last read 진입.............");
		log.info("empId / roomId / lastreadId :::: " + dto);

		// 마지막 메시지 조회
		MsgMessage lastMsg = msgMessageRepository.findTop1ByRoomId_RoomIdOrderByMsgIdDesc(dto.getRoomId());
		if (lastMsg == null) return;
		Long lastMsgId = lastMsg.getMsgId();
		msgRelationRepository.updateLastRead(dto.getReaderId(), dto.getRoomId(), lastMsgId);
	}


	// ========================================================
	// 내 상태 실시간 변경
	@Transactional
	public void updateStatus(StatusChangeRequest statusChangeRequest) {
		MsgStatus msgStatus = msgStatusRepository.findById(statusChangeRequest.getEmpId())
				.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다"));
		
		if (statusChangeRequest.getAvlbStat() != null) {
			msgStatus.setAvlbStat(statusChangeRequest.getAvlbStat());
			msgStatus.setAvlbUpdated(LocalDateTime.now());

			if ("OFFLINE".equals(statusChangeRequest.getAvlbStat())){
				msgStatus.setOnlineYn("N");
			} else {
				msgStatus.setOnlineYn("Y");
			}
		}
		
		if (statusChangeRequest.getWorkStat() != null) {
			msgStatus.setManualWorkStat(statusChangeRequest.getWorkStat());
			msgStatus.setWorkStatSource("MANUAL");
			msgStatus.setWorkStatUpdated(LocalDateTime.now());
		}
		
		msgStatus.setWorkStatUpdated(LocalDateTime.now());
		msgStatusRepository.save(msgStatus);
	}

	// ========================================================
	// 방에서 나가기 처리
	@Transactional
	public void exitRoom(RoomLeaveRequest dto) {
		MsgRelation relation = msgRelationRepository.findByRoomId_RoomIdAndEmpId_EmpId(dto.getRoomId(), dto.getEmpId())
				.orElseThrow(() -> new RuntimeException("참여자 없음"));
				
		relation.setParticipantYn("N");
	}
	
	// ========================================================
	// 대화방 검색 기능
	public List<MsgRoomListDTO> searchRooms(String empId, String keyword) {
		if (keyword == null || keyword.isBlank())
			return Collections.emptyList();

		log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 대화방 검색 진입... = ");
		// 1) roomId 검색
	    List<Long> byName 	 = msgRoomRepository.findRoomIdByGroupNameContaining(keyword);
	    List<Long> byMember  = msgRelationRepository.findRoomIdByMemberName(keyword);
	    List<Long> byMessage = msgMessageRepository.findRoomIdByMessageContent(keyword);

		log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> byName = " + byName);
		log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> byMember = " + byMember);
		log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> byMessage = " + byMessage);

	    // 2) 중복 제거 및 모으기
	    Set<Long> roomIds = new HashSet<>();
	    roomIds.addAll(byName);
	    roomIds.addAll(byMember);
	    roomIds.addAll(byMessage);
		log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> roomIds = " + roomIds);

		// 3) 현재 유저가 속한 방만 남기기
		List<Long> myRooms = msgRelationRepository.findRoomIdsByEmpId(empId);
		log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> myRooms = " + myRooms);
		roomIds.retainAll(myRooms);
		if (roomIds.isEmpty())
			return Collections.emptyList();

		// 4) 방 목록 생성
		List<MsgRoomListDTO> result = new ArrayList<>();

		for (Long roomId : roomIds) {

			// a) 기본 방 정보 찾기
			MsgRoomListDTO room = messengerMapper.selectChat(empId, roomId);

			// b) 메시지 내용에서 매칭되는 문장 찾기
			String message = msgMessageRepository
			        .findMatchedMessages(roomId, keyword)
			        .stream()
			        .findFirst()
			        .orElse(null);

			// 검색어가 있을 경우 해당 메시지를 보여주고 하이라이트 처리
			if (message != null) {
				room.setPreviewMessage(highlightGenerator.create(message, keyword));
			}

			// c) 이름/그룹명에서 매칭되는 결과에 하이라이트 처리
			//String groupName = room.getGroupName();
			//if (groupName != null && !groupName.isBlank()) {
			//	room.setGroupName(highlight(groupName, keyword));
			//}

			result.add(room);

		}
		
		// 4) 최신 순 정렬
		result.sort(Comparator.comparing(MsgRoomListDTO::getPreviewTime).reversed());
		log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> result = " + result);
		return result;
	}

	// ========================================================
	// 방 이름 수정 서비스
	@Transactional
	public void renameRoom(Long roomId, String newName) {
		MsgRoom room = msgRoomRepository.findById(roomId)
				.orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
		room.setGroupName(newName);
		log.info("newName...... = " + room.getGroupName());
	}

	// ========================================================
	// 메시지 가져오기
	public List<MsgMessageDTO> getMessages(Long roomId) {
		List<MsgMessage> list =
				msgMessageRepository.findByRoomId_RoomIdOrderBySentDate(roomId);

		List<MsgMessageDTO> dtoList = new ArrayList<>();

		for (MsgMessage msgMessage : list) {
			List<FileAttachDTO> files =
					fileAttachRepository.findByRefTableAndRefId("MSG_MESSAGE", msgMessage.getMsgId())
							.stream()
							.map(FileAttachDTO::fromEntity)
							.toList();


			MsgMessageDTO dto = MsgMessageDTO.fromEntity(msgMessage, files);

			// MsgStatus에서 프로필 조회
			String senderId = msgMessage.getSenderId().getEmpId();
			Integer profile = msgStatusRepository.findById(senderId)
					.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다"))
					.getMsgProfile();

			dto.setSenderId(senderId);
			dto.setSenderName(msgMessage.getSenderId().getEmpName());
			dto.setSenderProfile(profile);

			dtoList.add(dto);

		}
		return dtoList;
	}


}
