package com.yeoun.outbound.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OutboundOrderItemDTO {
	private Long outboundItemId; // 출고 품목 ID
	private String outboundId;
	private String matId;        // 원재료 ID
	private String matName;      // 원재료 이름
	private String prdId;        // 완제품 ID
	private String prdName;      // 완제품 이름
	private String matUnit;         // 단위
	private Long orderqQty;      // 원재료필요수량
	private Long shipmentQty;    // 출하요청수량
	private Long outboundQty;    // 출고수량
	private String lotNo;       // LOT 번호
	private BigDecimal matQty;      // 원재료 사용량
	private Long ivId;     // 재고번호
	private String locationId; // 창고위치
}
