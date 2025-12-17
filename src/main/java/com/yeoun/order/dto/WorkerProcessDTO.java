package com.yeoun.order.dto;

import java.time.LocalDateTime;
import java.util.List;

import groovy.transform.ToString;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class WorkerProcessDTO {
	
	private String scheduleId;
	private String empId;

}
