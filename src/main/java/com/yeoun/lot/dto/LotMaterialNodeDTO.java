package com.yeoun.lot.dto;

import lombok.Getter;
import lombok.Setter;

// LOT 추적에 쓰이는 자재 정보
@Getter
@Setter
public class LotMaterialNodeDTO {
	
	private String lotNo;			// 자재 LOT번호 (LOT_RELATIONSHIP.INPUT_LOT_NO)
	private String displayName;		// 자재 이름 (코드 + 자재명)
	private Integer usedQty;		// 사용 수량
	private String unit;			// 단위 (g, EA 등)
	
	public LotMaterialNodeDTO(String lotNo, String displayName, Integer usedQty, String unit) {
		this.lotNo = lotNo;
		this.displayName = displayName;
		this.usedQty = usedQty;
		this.unit = unit;
	}
	
	

}
