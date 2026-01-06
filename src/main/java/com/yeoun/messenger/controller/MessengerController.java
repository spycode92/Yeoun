package com.yeoun.messenger.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.messenger.dto.*;
import com.yeoun.messenger.entity.MsgRoom;
import com.yeoun.messenger.repository.MsgRoomRepository;

import com.yeoun.messenger.service.ChatService;
import com.yeoun.messenger.service.RoomMemberQueryService;
import com.yeoun.messenger.support.RoomNameGenerator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.messenger.service.MessengerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Controller
@RequestMapping("/messenger")
@RequiredArgsConstructor
@Log4j2
public class MessengerController {

	private final ChatService chatService;
	private final MessengerService messengerService;
	private final EmpRepository empRepository;
	private final MsgRoomRepository msgRoomRepository;
	private final RoomNameGenerator roomNameGenerator;
	private final RoomMemberQueryService roomMemberQueryService;
	
	// ==========================================================================
	// 메신저 팝업 목록 - 친구목록
	@GetMapping("/list")
	public String list(Authentication authentication, Model model) {
		LoginDTO loginDTO = (LoginDTO)authentication.getPrincipal();
		List<MsgStatusDTO> msgStatusDTOList = messengerService.getUsers(loginDTO.getUsername());

		model.addAttribute("friends", msgStatusDTOList);
		return "/messenger/list";
	}

	// ==========================================================================
	// 대화목록 새로 렌더링
	@PostMapping("/list/chat")
	public ResponseEntity<?> getChatRooms (Authentication authentication){
		LoginDTO loginDTO = (LoginDTO)authentication.getPrincipal();
		List<MsgRoomListDTO> msgRoomsDTOList = messengerService.getChatRooms(loginDTO.getUsername());
		log.info("왜안돼!!!!!!!!!!!!!!!!!!!! " + msgRoomsDTOList);
		return ResponseEntity.ok().body(msgRoomsDTOList);
	}
	
	// ==========================================================================
	// 메신저 상태 실시간 변경
	@PatchMapping("/status")
	public ResponseEntity<?> changeStatus(Authentication authentication,
										@RequestBody StatusChangeRequest statusChangeRequest){
		statusChangeRequest.setEmpId(authentication.getName());
		messengerService.updateStatus(statusChangeRequest);
		chatService.changeStatus(statusChangeRequest);
		return ResponseEntity.noContent().build();
	}

	// ==========================================================================
	// 메신저 상태 실시간 변경 (오프라인 처리용)
	@PostMapping("/status/offline")
	public ResponseEntity<?> changeStatusOffline(Authentication authentication){
		StatusChangeRequest req = new StatusChangeRequest();

		req.setEmpId(authentication.getName());
		req.setAvlbStat("OFFLINE");
		messengerService.updateStatus(req);
		chatService.changeStatus(req);
		return ResponseEntity.noContent().build();
	}
	
	// ==========================================================================
	// 친구목록 즐겨찾기 토글
	@PatchMapping("/favorite/{id}")
	public ResponseEntity<?> toggleFavorite(Authentication authentication, @PathVariable("id") String id){
		
		MsgFavoriteDTO msgFavoriteDTO = new MsgFavoriteDTO();
		msgFavoriteDTO.setEmpId(((LoginDTO)authentication.getPrincipal()).getUsername());
		msgFavoriteDTO.setFvUser(id);
		
		if (messengerService.searchFavorite(msgFavoriteDTO)) {
			messengerService.deleteFavorite(msgFavoriteDTO);
		} else {
			messengerService.createFavorite(msgFavoriteDTO);
		}
		
		return ResponseEntity.ok().body("success");
	}
	
	// ==========================================================================
	// 메신저 팝업 채팅방 - 대상 선택 진입
	@GetMapping("/target/{id}")
	public String startChat(Authentication authentication, Model model, @PathVariable("id") String id){

		log.info("startChat 진입.......");
		log.info("id..................." + id);

		// target과의 방 찾기
		Long roomId = messengerService.searchRoom(authentication.getName(), id);

		// 진짜 기존 방을 발견했다면
		if (roomId != 0)
			return "redirect:/messenger/room/" + roomId;

		Emp target = empRepository.findByEmpId(id).get();
		String groupName = target.getEmpName() + " " + target.getPosition().getPosName();

		log.info("groupName ::: " + groupName);
		// 첫 대화인 경우
		model.addAttribute("targetEmpId", id);
		model.addAttribute("groupYn", "N");
		model.addAttribute("targetName", empRepository.findById(id).get().getEmpName());
		model.addAttribute("groupName", groupName);
		return "/messenger/chat";
	}

	// ==========================================================================
	// 메신저 팝업 채팅방 - 방 선택 진입
	@GetMapping("/room/{id}")
	public String openRoom(Authentication authentication, Model model, @PathVariable("id") Long id){

		// 1) 메시지 불러오기
		List<MsgMessageDTO> msgList = messengerService.getMessages(id);

		// 2) 멤버 불러오기
		List<RoomMemberDTO> memberList = roomMemberQueryService.getMembers(id);

		// 3) 방 정보 불러오기
		MsgRoom room = msgRoomRepository.findById(id).get();
		String groupName = roomNameGenerator.create(id, authentication.getName(), memberList);

		// 5) 방 이름 모델에 담기
		model.addAttribute("roomId", id);
		model.addAttribute("msgList", msgList);
		model.addAttribute("groupYn", room.getGroupYn());
		model.addAttribute("groupName", groupName);
		model.addAttribute("members", memberList);
		return "/messenger/chat";
	}

	// ==========================================================================
	// 메신저 팝업 채팅방 - 새로운 방 생성
	@ResponseBody
	@PostMapping(value="/chat")
	public ResponseEntity<?> createRoom(@RequestBody RoomCreateRequest roomCreateRequest, Authentication auth) throws IOException {
		MsgRoomDTO dto = messengerService.createRoom(roomCreateRequest, auth.getName());
		// =============================> 파일 추가해야함... 잊지말것..
		// =============================> 파일 추가해야함... 잊지말것..
		// =============================> 파일 추가해야함... 잊지말것..
		// =============================> 파일 추가해야함... 잊지말것..
		// =============================> 파일 추가해야함... 잊지말것..
		// =============================> 파일 추가해야함... 잊지말것..
		// =============================> 파일 추가해야함... 잊지말것..
		return ResponseEntity.ok(dto);
	}

	// ==========================================================================
	// 메신저 팝업 채팅방 - 기존 방에 메시지 전송
	@PostMapping(value = "/chat/{id}",
				 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> sendMessage(Authentication authentication,
										 @PathVariable("id") Long id,
										 @RequestPart("message") MsgSendRequest dto,
										 @RequestPart(value = "files", required = false) List<MultipartFile> files)
		throws IOException{

		// roomId 지정
		dto.setRoomId(id);

		// DB 저장 + 파일 저장
		MessageSaveResult result = messengerService.saveMessage(dto, files);

		// STOMP 브로드캐스트
		chatService.broadcastMessage(result.getMessage(), result.getFiles());

		return ResponseEntity.ok().build();
	}

	// ==========================================================================
	// 채팅방 검색 기능
	@GetMapping("/rooms/search")
	public ResponseEntity<?> searchRooms(Authentication authentication, @RequestParam("keyword") String keyword) {
	    return ResponseEntity.ok(messengerService.searchRooms(authentication.getName(), keyword));
	}

	// ==========================================================================
	// 방 이름 수정
	@PatchMapping("/room/{roomId}/rename")
	public ResponseEntity<String> renameRoom(
			@PathVariable("roomId") Long roomId,
			@RequestBody Map<String, String> request
	) {
		log.info("방 이름 수정 컨트롤러....");
		String newName = request.get("groupName");
		messengerService.renameRoom(roomId, newName);

		return ResponseEntity.ok("updated");
	}


	
}
