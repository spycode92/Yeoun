package com.yeoun.order.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderValidationResult {
	
	private boolean valid;
	private boolean lineAvailable;
	private boolean workerAvailable;
	private String message;
	private List<String> suggestedLines;
	private List<String> suggestedWorkers;

}
