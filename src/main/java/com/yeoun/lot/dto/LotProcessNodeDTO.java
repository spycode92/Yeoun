package com.yeoun.lot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// LOT 계층 트리의 1차 노드 - 공정 단계
@Getter
@AllArgsConstructor
public class LotProcessNodeDTO {
	
	private Integer stepSeq;	// 공정 순번
	private String processId;	// 공정 Id
	private String processName;	// 공정명
	private String status;		// LOT 기준 공정 상태
	private String orderId;		// 작업지시번호

}
