package com.yeoun.lot.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
@Builder
public class LotEquipInfoDTO {
	
	private Long prodEquipId;       // 설비 고유번호
    private String equipCode;       // 설비 코드
    private String equipName;       // 설비명 (블렌딩 탱크1)
    private String status;          // 설비 상태
    private String stdName;         // 설비 영문명 (영문 표준명)
    private String koName;  		// 설비 한글명 (혼합탱크)
    
}
