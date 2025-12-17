package com.yeoun.main.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeoun.approval.service.ApprovalDocService;
import com.yeoun.attendance.service.AttendanceService;

import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.main.dto.ScheduleDTO;
import com.yeoun.main.dto.ScheduleSharerDTO;
import com.yeoun.main.service.ScheduleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/main")
public class MainController {
	private final ScheduleService scheduleService;
	private final AttendanceService attendanceService;
	private final ApprovalDocService approvalDocService;

	private final SimpMessagingTemplate messagingTemplate;

	// 메인페이지 스케줄페이지
	@GetMapping("")
	public String schedule(@AuthenticationPrincipal LoginDTO loginUser, Model model) {

		if (loginUser != null) {
			model.addAttribute("currentUserId", loginUser.getEmpId());
			model.addAttribute("currentUserName", loginUser.getEmpName());
		}

		model.addAttribute("buttonEnabled", attendanceService.isAttendanceButtonEnabled(loginUser.getEmpId()));
		model.addAttribute("status", attendanceService.attendanceStatus(loginUser.getEmpId()));
		model.addAttribute("deptList", approvalDocService.getDept());
		return "/main/schedule";
	}

	@GetMapping("/schedule")
	public String scheduleList() {

		return "/main/schedule_list";
	}

	// 일정등록
	@PostMapping("/schedule")
	public ResponseEntity<Map<String, String>> createSchedule(
			@ModelAttribute("scheduleDTO") @Valid ScheduleDTO scheduleDTO,
			BindingResult bindingResult,
			@RequestParam(name = "sharedEmpList", required = false, defaultValue = "[]") String sharedEmpListJson) {
		// 리턴에 사용할 Map 객체 생성
		Map<String, String> msg = new HashMap<>();
		// 받아온 sharedEmpListJson를 파싱해서 저장할 ScheduleSharerDTO리스트 생성
		List<ScheduleSharerDTO> list = new ArrayList<ScheduleSharerDTO>();

		// sharedEmpListJson 형태를 DTO로 변환할 객체
		ObjectMapper mapper = new ObjectMapper();
		// sharedEmpListJson객체가 존재할때만 실행
		if (sharedEmpListJson != null || sharedEmpListJson != "") {
			try {
				list = mapper.readValue(sharedEmpListJson, new TypeReference<List<ScheduleSharerDTO>>() {
				});
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}

		// 일정등록 요청 데이터 검증
		if (bindingResult.hasErrors()) {
			msg.put("msg", "일정 등록에 실패했습니다. - BINDING ERROR");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
		}
		// 일정등록 요청 데이터 이상 없을때
		// 일정등록 요청
		try {
			scheduleService.createSchedule(scheduleDTO, list);
			msg.put("msg", "일정이 등록되었습니다.");
			return ResponseEntity.ok(msg);

		} catch (Exception e) { // 에러발생시 일정등록 실패 메세지전달
			msg.put("msg", "일정 등록에 실패했습니다 :" + e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
		}
	}

	// 일정수정
	@PatchMapping("/schedule")
	public ResponseEntity<Map<String, String>> modifySchedule(
			@ModelAttribute("scheduleDTO") @Valid ScheduleDTO scheduleDTO,
			BindingResult bindingResult,
			@RequestParam(name = "sharedEmpList", required = false, defaultValue = "[]") String sharedEmpListJson) {
		Map<String, String> msg = new HashMap<>();
		// 받아온 sharedEmpListJson를 파싱해서 저장할 ScheduleSharerDTO리스트 생성
		List<ScheduleSharerDTO> list = new ArrayList<ScheduleSharerDTO>();

		// sharedEmpListJson 형태를 DTO로 변환할 객체
		ObjectMapper mapper = new ObjectMapper();
		// sharedEmpListJson객체가 존재할때만 실행
		if (sharedEmpListJson != null || sharedEmpListJson != "") {
			try {
				list = mapper.readValue(sharedEmpListJson, new TypeReference<List<ScheduleSharerDTO>>() {
				});
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}

		// 일정수정 요청 데이터 검증
		if (bindingResult.hasErrors()) {
			msg.put("msg", "일정 수정에 실패했습니다.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
		}
		// 일정수정 요청 데이터 이상 없을때
		// 일정수정 요청
		try {
			scheduleService.modifySchedule(scheduleDTO, list);
			msg.put("msg", "일정이 수정되었습니다.");
			return ResponseEntity.ok(msg);

		} catch (Exception e) { // 에러발생시 일정등록 실패 메세지전달
			msg.put("msg", "일정 수정에 실패했습니다 :" + e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
		}
	}

	// 일정삭제
	@DeleteMapping("/schedule")
	public ResponseEntity<Map<String, String>> deleteSchedule(
			@ModelAttribute("scheduleDTO") @Valid ScheduleDTO scheduleDTO,
			BindingResult bindingResult, Authentication authentication) {
		Map<String, String> msg = new HashMap<>();
		// System.out.println(scheduleDTO);

		// 일정수정 요청 데이터 검증
		if (bindingResult.hasErrors()) {
			msg.put("msg", "일정 삭제에 실패했습니다.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
		}
		// 일정수정 요청 데이터 이상 없을때
		// 일정수정 요청
		try {
			scheduleService.deleteSchedule(scheduleDTO, authentication);
			msg.put("msg", "일정이 삭제되었습니다.");
			return ResponseEntity.ok(msg);

		} catch (Exception e) { // 에러발생시 일정등록 실패 메세지전달
			msg.put("msg", "일정 삭제에 실패했습니다 :" + e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
		}
	}

}
