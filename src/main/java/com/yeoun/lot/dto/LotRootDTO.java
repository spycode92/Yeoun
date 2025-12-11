package com.yeoun.lot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;


// LOT 추적 화면 왼쪽 "완제품 LOT 목록"
@Getter
@AllArgsConstructor
public class LotRootDTO {
	
	private String lotNo;			// LOT 번호
	private String displayName;		// 화면에 표시할 제품명
	private String status;			// LOT 진행 상태

}
