package com.yeoun.equipment.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HistorySearchDTO {
	String equipment;
	String line;
	String reason;
	LocalDateTime start;
	LocalDateTime end;
}
