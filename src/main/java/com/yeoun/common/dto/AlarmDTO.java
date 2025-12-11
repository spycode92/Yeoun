package com.yeoun.common.dto;

import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;

import com.yeoun.common.entity.Alarm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlarmDTO {
	private Long alarmId;
	private String empId;
	private String alarmMessage;
	private String alarmStatus;
	private String alarmLink;
	private LocalDateTime createdDate;
	
	// ----------------------------------------------------------
	private static ModelMapper modelMapper = new ModelMapper();
	
	public Alarm toEntity() {
		return modelMapper.map(this,  Alarm.class);
	}
	
	public static AlarmDTO fromEntity(Alarm alarm) {
		return modelMapper.map(alarm, AlarmDTO.class);
	}
}
