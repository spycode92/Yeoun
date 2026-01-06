package com.yeoun.common.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.yeoun.common.dto.DisposeDTO;
import com.yeoun.common.dto.DisposeListDTO;
import com.yeoun.common.service.DisposeService;

import lombok.RequiredArgsConstructor;


@Controller
@RequestMapping("/dispose")
@RequiredArgsConstructor
public class DisposeController {
	private final DisposeService disposeService;
	
	@GetMapping("")
	public String getDispose() {
		return "/dispose/disposeList";
	}
	
	@PostMapping("/list")
	public ResponseEntity<List<DisposeListDTO>> getDisposeList(@RequestBody DisposeListDTO requestBody) {
		
		System.out.println(">>>>>>>>>>>>>>>>>>>>" + requestBody);
		
		List<DisposeListDTO> disposeList = disposeService.getDisposeList(requestBody);
		
		return ResponseEntity.ok(disposeList);
	}
	
}
