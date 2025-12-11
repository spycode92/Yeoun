package com.yeoun.order.dto;

import java.util.List;

import com.yeoun.order.dto.WorkOrderDetailDTO.WorkInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class WorkersDTO {
	public String workerId;
	public String workerName;
	public String dept;
}
