package com.yeoun.leave.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeoun.attendance.entity.WorkPolicy;
import com.yeoun.emp.entity.Dept;
import com.yeoun.emp.entity.Emp;
import com.yeoun.leave.dto.LeaveChangeRequestDTO;

import groovy.transform.ToString;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ANNUAL_LEAVE")
@SequenceGenerator(
		name = "ANNUAL_LEAVE_SEQ_GENERATOR",
		sequenceName = "ANNUAL_LEAVE_SEQ", 
		initialValue = 70,
		allocationSize = 1
)
@Getter
@Setter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AnnualLeave {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "ANNUAL_LEAVE_SEQ_GENERATOR")
	private Long leaveId;
	
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "EMP_ID", nullable = false)
	private Emp emp; // 직원 Id
	
	private LocalDate periodStart; // 입사일 기준일 경우 산정 시작일
	private LocalDate periodEnd; // 입사일 기준일 경우 산정 종료일
	private int useYear; // 회계연도 기준일 경우 기준연도
	
	@Column(nullable = false)
	private double totalDays; // 사원에게 부여된 총 연차
	
	private double usedDays; // 사용한 연차
	private double remainDays; // 남은 연차
	private String updatedUser; // 수기로 연차 수정한 직원
	private LocalDateTime updatedDate; // 수정된 날짜
	private String reason; // 수정한 이유
	
	@OneToMany(mappedBy = "annualLeave", cascade = CascadeType.ALL)
	private List<AnnualLeaveHistory> histories = new ArrayList<>();
	
	@Builder
	public AnnualLeave(Emp emp, LocalDate periodStart, LocalDate periodEnd, int totalDays) {
		this.emp = emp;
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
		this.useYear = periodStart.getYear();
		this.totalDays = totalDays;
		this.usedDays = 0;
		this.remainDays = totalDays;
	}
	
	// 연차 기준 정책에 따라 총 연차 계산
	public void updateTotaldays(String workPolicy) {
		LocalDate hireDate = emp.getHireDate();
		LocalDate today = LocalDate.now();
		
		if ("JOIN".equals(workPolicy)) { // 입사일 기준
			this.totalDays = calculateJoinBasisAnnual(hireDate, today);
		} else { // 회계연도 기준
			this.totalDays = calculateFiscalBasisAnnual(hireDate, today);
		}
		
		// 잔여연차 계산
		this.remainDays = totalDays - usedDays;
	}
	
	// 입사일 기준 계산
	private int calculateJoinBasisAnnual(LocalDate hireDate, LocalDate today) {
		// 입사일부터 오늘 날짜까지의 연도 차이 계산
		long year = ChronoUnit.YEARS.between(hireDate, today);
		
		if (year < 1) { // 근속 연수 1년 미만이면, 입사월부터 매달 1일씩 연차 발생
			long month = ChronoUnit.MONTHS.between(hireDate, today);
			return (int) Math.min(month, 11); // 법적 최대치(11) 초과하지 않도록 제한
		} else {
			int extra = (int) ((year - 1) / 2); // 2년마다 1일씩 추가
			return Math.min(15 + extra, 25); // 법적으로 근속 연수에 따라서 최소한 25일까지 부여
		}
	}
	
	// 회계연도 기준 계산
	private int calculateFiscalBasisAnnual(LocalDate hireDate, LocalDate today) {
		int year = today.getYear();
		LocalDate fiscalEnd = LocalDate.of(year, 12, 31);
		
		long totalYears = ChronoUnit.YEARS.between(hireDate, today);

	    // 입사 1년 이상인 경우 최소 15일
	    if (totalYears >= 1) {
	        int extra = (int) ((totalYears - 1) / 2);
	        return Math.min(15 + extra, 25);
	    }

	    // 1년 미만이면 회계연도 기준으로 월별 산정
	    long monthsWorked = ChronoUnit.MONTHS.between(hireDate, fiscalEnd);
	    return (int) Math.min(monthsWorked, 11);
	}
	
	// 연차 사용했을 경우
	public void useAnnual(double useDays) {
		// 사용 일수가 음수일 경우
		if (useDays <= 0) {
			 throw new IllegalArgumentException("사용 일수는 1일 이상이어야 합니다.");
		}
		
		// 기존에 사용한 연차와 총 연차와 비교해서 초과할 경우
		if (this.usedDays + useDays > this.totalDays) {
			throw new IllegalArgumentException("사용 가능한 연차를 초과했습니다.");
		}
		
		// 기존 사용한 연차에 더하기
		this.usedDays += useDays;
		
		// 총 연차에서 사용한 연차 빼기
		this.remainDays = this.totalDays - this.usedDays;
	}
	
	// 연차 수정했을 경우
	public void modifyAnnual(String userId, LeaveChangeRequestDTO leaveChangeRequestDTO) {
		
		// 증감을 알 수 있는 타입
		String type = leaveChangeRequestDTO.getChangeType();
		// 변경된 연차 갯수
		int changeDays = leaveChangeRequestDTO.getChangeDays();
		
		// 차감일 경우 총 연차에서 변경된 연차 갯수 비교해서 부족한 경우 에러 처리
		if ("decrease".equals(type)) {
			if (this.totalDays - changeDays < this.usedDays) {
				throw new IllegalArgumentException("차감할 수 있는 연차가 부족합니다.");
			}
		}
		
		// 증감에 따라 총 연차 수정
		if ("increase".equals(type)) {
			this.totalDays += changeDays;
			this.remainDays = this.totalDays - this.usedDays;
		} else {
			this.totalDays -= changeDays;
			this.remainDays = this.totalDays - this.usedDays;
		}
		
		this.updatedUser = userId;
		this.updatedDate = LocalDateTime.now();
		this.reason = leaveChangeRequestDTO.getReason();
	}
	
	// 매년 1월 1일 연차 업데이트
	public void updateAnnual(LocalDate newStart, LocalDate newEnd, int currentYear, String policy) {
		this.periodStart = newStart;
		this.periodEnd = newEnd;
		this.useYear = currentYear;
		this.updateTotaldays(policy);
		this.usedDays = 0;
		this.remainDays = this.totalDays;
	}
}
