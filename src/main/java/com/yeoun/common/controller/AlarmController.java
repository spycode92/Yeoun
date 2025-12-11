package com.yeoun.common.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.common.dto.AlarmDTO;
import com.yeoun.common.service.AlarmService;

import lombok.AllArgsConstructor;


@Controller
@RequestMapping("/alarm")
@AllArgsConstructor
public class AlarmController {
	private final AlarmService alarmService;
	
	@GetMapping("")
	public String alarm() {
		return "/alarm/alarm_list";
	}
	
	// 기간내 알림 목록
	@GetMapping("/list")
	public ResponseEntity<List<AlarmDTO>> getAlarmData(@AuthenticationPrincipal LoginDTO loginDTO, 
			@RequestParam(required = false, name = "startDate") String startDate,  
			@RequestParam(required = false, name = "endDate") String endDate) {
		String empId = loginDTO.getEmpId();
		
		List<AlarmDTO> alarmDTOList = alarmService.getAlarmData(empId, startDate, endDate);
		
		return ResponseEntity.ok(alarmDTOList);
	}
	
	// 알림 읽음처리 
	@PostMapping("/list")
	public ResponseEntity<Map<String,String>> updateAlarmStatus(@AuthenticationPrincipal LoginDTO loginDTO, 
			@RequestParam(required = false, name = "startDate") String startDate,  
			@RequestParam(required = false, name = "endDate") String endDate,
			@RequestParam(required = false, name = "alarmStatus") String alarmStatus) {
		String empId = loginDTO.getEmpId();
		
		alarmService.updateAlarmStatus(empId, startDate, endDate, alarmStatus);
		
		Map<String,String> result = new HashMap<String, String>();
		result.put("msg", "알림상태 업데이트 완료");
		
		
		return ResponseEntity.ok(result);
	}
	
	@GetMapping("/status")
	public ResponseEntity<List<AlarmDTO>> getAlarmStatus(@AuthenticationPrincipal LoginDTO loginDTO
			, @RequestParam(required = false, name = "alarmStatus") String alarmStatus) {
		String empId = loginDTO.getEmpId();
		
		List<AlarmDTO> alarmList = alarmService.getAlarmStatus(empId, alarmStatus);
		System.out.println(alarmList);
		return ResponseEntity.ok(alarmList);
	}
	
}
