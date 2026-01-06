package com.yeoun.lot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LotUsedProductNodeDTO {
	
	private String lotNo;        // 완제품 LOT 번호 (output lot)
    private String displayName;  // 완제품 표시명
    private Integer usedQty;     // 이 자재가 투입된 수량 (관계 테이블 used_qty)
    private String unit;         // 자재 단위 (g/EA 등) - 화면에 같이 보여주면 좋음
    private String status;       // 선택: 완제품 LOT 상태 라벨(원하면)

}
