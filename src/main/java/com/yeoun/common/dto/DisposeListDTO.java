package com.yeoun.common.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;

import com.yeoun.common.entity.Dispose;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class DisposeListDTO {
	private Long disposeId; // 폐기ID
	private String lotNo; //로트번호
	private String itemId; // 원자재/부자재, 완제품의 기준정보 고유값
	
	private String itemName; // 자재이름 조회용
	
	private String workType; // 작업종류 ( 입고, 재고, 생산 )
	private String empId; // 작업자
	
	private String empName; // 작업자이름 조회용
	
	private Long disposeAmount; // 폐기수량
	private String disposeReason; // 폐기이유
	private LocalDateTime createdDate; // 등록 일시
	
	// 검색에 필요한 정보
	private LocalDate startDate;
	private LocalDate endDate;
	private String searchKeyword;
	private String searchType;
	
	
}
