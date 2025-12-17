package com.yeoun.order.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkOrderValidateRequest {

	private String lineId;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private String workerId;
	private List<String> workers;
}
