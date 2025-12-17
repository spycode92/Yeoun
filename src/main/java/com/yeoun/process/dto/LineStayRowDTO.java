package com.yeoun.process.dto;

import java.util.List;

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
public class LineStayRowDTO {
	
	private String activeOrderId;   // 라인 대표 작업지시번호
	
	private String lineId;
    private String lineName;            // 고체/액체/여분 등
    private List<StayCellDTO> steps;    // size = 6

}
