package com.yeoun.attendance.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yeoun.attendance.dto.AttendanceDTO;
import com.yeoun.emp.entity.Emp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "ATTENDANCE")
@SequenceGenerator(
		name = "ATTENDANCE_SEQ_GENERATOR",
		sequenceName = "ATTENDANCE_SEQ", 
		initialValue = 1,
		allocationSize = 1
)
@Getter
@Setter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Attendance {
	@Id 
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ATTENDANCE_SEQ_GENERATOR")
	private Long attendanceId;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "EMP_ID", nullable = false)
	private Emp emp; // 출근한 사원 번호
	
	@Column(nullable = false)
	private LocalDate workDate; // 근무일자
	
	@JsonFormat(pattern = "HH:MM")
	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime workIn; // 출근시간
	
	@JsonFormat(pattern = "HH:MM")
	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime workOut; // 퇴근시간
	
	private Integer workDuration; // 근무시간(분단위)
	private String statusCode; // 근태상태
	private String remark; // 비고
	
	@Column(length = 7)
	private String createdUser; // 등록자(수기로 등록 시 사용)
	
	@CreatedDate
	private LocalDateTime createdDate; // 등록일자
	
	@Column(length = 7)
	private String updatedUser; // 수정자
	
	@LastModifiedDate
	private LocalDateTime updatedDate; // 수정일자
	
	// 수기로 출퇴근 등록
	public static Attendance createAttendance(AttendanceDTO attendanceDTO, Emp emp) {
		Attendance attendance = new Attendance();
		
		attendance.emp = emp;
		attendance.workDate = attendanceDTO.getWorkDate();
		attendance.workIn = attendanceDTO.getWorkIn();
		attendance.workOut = attendanceDTO.getWorkOut();
		attendance.statusCode = attendanceDTO.getStatusCode();
		attendance.createdUser = attendanceDTO.getCreatedUser();
		
		return attendance;
	}
	
	// 자동 출근 (출퇴근 버튼 찍었을 경우)
	public static Attendance createForWorkIn(Emp emp, LocalDate date, LocalTime now, 
			LocalTime standardIn, int lateLimit, List<AccessLog> accessLogs) {
		Attendance attendance = new Attendance();

		attendance.emp = emp;
		attendance.workDate = date;
		
		AccessLog outWorkLog = null;
		
		// list로 받아온 accesslog에서 accesstype의 outwork 값만 필터링
		if (accessLogs != null && !accessLogs.isEmpty()) {
			outWorkLog = accessLogs.stream()
					.filter(log -> log.getAccessType() != null && 
							log.getAccessType().equalsIgnoreCase("OUTWORK"))
					.max(Comparator.comparing(AccessLog::getOutTime))
					.orElse(null);
		}
		
		if (outWorkLog != null) {
			LocalTime outworkTime  = outWorkLog.getOutTime();
			
			if (outworkTime != null && !outworkTime.isAfter(standardIn.plusMinutes(lateLimit))) {
				
				// 정상 출근
				attendance.statusCode = "WORKIN";
				attendance.workIn = outworkTime;
			} else {
				// 지각
				attendance.statusCode = "LATE";
				attendance.workIn = (outworkTime != null) ? outworkTime : now;
			}
			
			attendance.remark = outWorkLog.getReason();
		} else {
			// 외근 없는 일반 출근 처리
			attendance.statusCode = now.isAfter(standardIn.plusMinutes(lateLimit)) ? "LATE" : "WORKIN";
			
			attendance.workIn = now;
		}
		
		return attendance;
	}
	
	// 퇴근 처리
	public void recordWorkOut(LocalTime now) {
		LocalTime  outTime = now;
		
	    if (outTime != null) {
	        this.workOut = outTime;
	    }
	}
	
	// 근태 수정 로직
	public void modifyAttendance(LocalTime workIn, LocalTime workOut, String statusCode, String updateUserEmpId) {
		if (workIn != null) {
			this.workIn = workIn;
		}

		if (workOut != null) {
			this.workOut = workOut;
		}
		
		this.statusCode = statusCode;
		this.updatedUser = updateUserEmpId;
	}
	
	// 퇴근 중복 방지 체크
	public boolean isAlreadyOut() {
		return this.workOut != null;
	}

	// 외근 등록 후 데이터 변경
	public void markAsInByOutwork(LocalTime outTime, WorkPolicy workPolicy, String reason) {
		// 출근 시간이 없을 경우에만 외근 시작 시간을 출근 시간으로 변경
		if (this.workIn == null) {
			this.workIn = outTime;
			
			LocalTime standardIn = LocalTime.parse(workPolicy.getInTime());
			int lateLimit = workPolicy.getLateLimit();
			this.statusCode = (outTime.isAfter(standardIn.plusMinutes(lateLimit))) ? "LATE" : "WORKIN";
		}
		
		this.remark = reason;
	}
	
	// 근무시간 변경
	public void adjustWorkDuration(int minutes) {
	    this.workDuration = Math.max(minutes, 0);
	}
	
	// 상태 변경
	public void changeStatus(String status) {
		this.statusCode = status;
	}
}












