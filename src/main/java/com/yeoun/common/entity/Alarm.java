package com.yeoun.common.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Entity
@Table(name = "ALARM")
@SequenceGenerator(
		name = "ALARMS_SEQ_GENERATOR", // JPA 에서 사용하는 시퀀스 이름(DB의 시퀀스 이름이 아님!)
		sequenceName = "ALARM_SEQ", // 오라클에서 사용하는 시퀀스 이름
		initialValue = 1, 			// 초기값(오라클 시퀀스의 start with 1과 동일)
		allocationSize = 1          // 증가값(오라클 시퀀스의 increment by 값과 동일)
		)
@Getter
@Setter
@ToString
@EntityListeners(AuditingEntityListener.class)
public class Alarm {
	@Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ALARMS_SEQ_GENERATOR")
	@Column(name = "ALARM_ID", updatable = false)
	private Long alarmId;
	@Column(nullable = false)
	private String empId;
	@Column(nullable = false)
	private String alarmMessage;
	@Column(nullable = false)
	private String alarmStatus;
	@Column(nullable = true)
	private String alarmLink;
	@CreatedDate
	private LocalDateTime createdDate;
}
