package com.yeoun.inbound.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ReceiptDTO {
	private String clientName; // 거래처명
	private String inboundId; // 입고Id
	private LocalDateTime expectArrivalDate; // 예상입고일
	private String inboundStatus; // 상태
	private String materialId; // 발주Id;
	private String prodId; // 작업지서서Id
	private String orderEmpName; // 발주 담당자(원재료)
	private String materialEmpName; // 입고 담당자(완제품)
	private List<ReceiptItemDTO> items; // 입고 품목들
	
	// 작업지시서 등록자이름
	private String workOrderEmpName;
}
