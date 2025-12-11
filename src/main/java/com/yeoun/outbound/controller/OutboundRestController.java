package com.yeoun.outbound.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yeoun.outbound.dto.OutboundOrderDTO;
import com.yeoun.outbound.service.OutboundService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Log4j2
public class OutboundRestController {
	private final OutboundService outboundService;
	
	@GetMapping("/shipment/list")
	public ResponseEntity<List<OutboundOrderDTO>> shipmentList() {
		
		List<OutboundOrderDTO> shipmnetList = outboundService.getShipmentList();
		
		return ResponseEntity.ok(shipmnetList);
	}
}
