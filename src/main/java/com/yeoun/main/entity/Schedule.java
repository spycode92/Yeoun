package com.yeoun.main.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeoun.emp.entity.Emp;
import com.yeoun.main.dto.ScheduleDTO;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "schedule")
@SequenceGenerator(
		name = "SCHEDULES_SEQ_GENERATOR", // JPA 에서 사용하는 시퀀스 이름(DB의 시퀀스 이름이 아님!)
		sequenceName = "SCHEDULE_SEQ", // 오라클에서 사용하는 시퀀스 이름
		initialValue = 1, 			// 초기값(오라클 시퀀스의 start with 1과 동일)
		allocationSize = 1          // 증가값(오라클 시퀀스의 increment by 값과 동일)
		)
@Getter
@Setter
@ToString
@EntityListeners(AuditingEntityListener.class)
public class Schedule {
	@Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SCHEDULES_SEQ_GENERATOR")
	@Column(name = "SCHEDULE_ID", updatable = false)
	private Long scheduleId; // 일정ID

	@Column(nullable = false)
	private String scheduleTitle; // 일정 제목
	
	@Column(nullable = false)
	private String scheduleContent; // 일정 내용
	
	@Column(nullable = false)
	private String scheduleType; // 일정 종류
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "CREATED_USER", referencedColumnName = "EMP_ID", nullable = false)
	private Emp emp;
	
	@Column(nullable = false, length = 1)
	private String alldayYN = "N"; // 종일일정구분
	
	@Column(nullable = false)
	private LocalDateTime scheduleStart; // 일정 시작시간
	
	@Column(nullable = false)
	private LocalDateTime scheduleFinish; // 일정 마치는시간
	
	@CreatedDate
	private LocalDateTime createdDate; // 등록 일시
	
	@LastModifiedDate
	private LocalDateTime updatedDate; // 수정 일시
	
	@Column(length = 20, nullable = false)
    private String recurrenceType = "none";
	
	// ------------------------------------------------------------------------
	@OneToMany(mappedBy = "schedule", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ScheduleSharer> scheduleSharers;
	
	public void changeSchedule(ScheduleDTO scheduleDTO) {
		this.scheduleTitle = scheduleDTO.getScheduleTitle();
		this.scheduleContent = scheduleDTO.getScheduleContent();
		this.scheduleType = scheduleDTO.getScheduleType();
		this.alldayYN = scheduleDTO.getAlldayYN();
		this.scheduleStart = scheduleDTO.getScheduleStart();
		this.scheduleFinish = scheduleDTO.getScheduleFinish();
	}
}
