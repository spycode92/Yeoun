package com.yeoun.leave.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.yeoun.approval.entity.ApprovalDoc;
import com.yeoun.approval.repository.ApprovalDocRepository;
import com.yeoun.attendance.entity.WorkPolicy;
import com.yeoun.attendance.repository.WorkPolicyRepository;
import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.leave.dto.LeaveChangeRequestDTO;
import com.yeoun.leave.dto.LeaveDTO;
import com.yeoun.leave.dto.LeaveHistoryDTO;
import com.yeoun.leave.entity.AnnualLeave;
import com.yeoun.leave.entity.AnnualLeaveHistory;
import com.yeoun.leave.repository.LeaveHistoryRepository;
import com.yeoun.leave.repository.LeaveRepository;
import com.yeoun.pay.entity.PayrollPayslip;
import com.yeoun.pay.repository.PayrollPayslipRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class LeaveService {
	
	private final EmpRepository empRepository;
	private final LeaveRepository leaveRepository;
	private final LeaveHistoryRepository historyRepository;
	private final WorkPolicyRepository workPolicyRepository;
	private final PayrollPayslipRepository payslipRepo;
	private final ApprovalDocRepository approvalDocRepository;

	// 직원 연차 생성
	@Transactional
	public void createAnnualLeaveForEmp(String empId) {
		Emp emp = empRepository.findById(empId)
				.orElseThrow(() -> new NoSuchElementException("사원을 찾을 수 없습니다."));
		
		// 연차 생성 되었는지 확인
		if (leaveRepository.existsByEmp(emp)) {
			throw new IllegalStateException("이미 연차 데이터가 존재합니다.");
		}
		
		// 근무정책 조회
		WorkPolicy workPolicy = workPolicyRepository.findFirstByOrderByPolicyIdAsc()
				.orElseThrow(() -> new NoSuchElementException("등록된 근무정책이 없습니다."));
		
		LocalDate periodStart = emp.getHireDate();
		LocalDate periodend = emp.getHireDate().plusYears(1).minusDays(1);
		
		AnnualLeave annualLeave = AnnualLeave.builder()
				.emp(emp)
				.periodStart(periodStart)
				.periodEnd(periodend)
				.build();
		
		// 정책 기준으로 연차 계산
		annualLeave.updateTotaldays(workPolicy.getAnnualBasis());
		
		leaveRepository.save(annualLeave);
	}

	// 개인 연차 조회
	public LeaveDTO getAnnualLeave(String empId) {
		// 연차 정보 가져오기 
		AnnualLeave annualLeave = leaveRepository.findByEmp_EmpId(empId)
				.orElseThrow(() -> new NoSuchElementException("해당 사원의 연차 정보를 찾을 수 없습니다."));
		
		// --------------------------------------------------------
		// 연차 수당 제외로 주석 처리
//		LocalDate now = LocalDate.now();
		
		// 현재 날짜 기준으로 yyyymm 생성
//		String currentDate = String.format("%d%02d", now.getYear(), now.getMonthValue());
		
//		PayrollPayslip payslip = payslipRepo
//	                .findByPayYymmAndEmpId(currentDate, empId)
//	                .orElseThrow(() -> new IllegalArgumentException("명세서 없음"));
		 
		// 연차 수당 추계액 계산
//		leaveDTO.calculateAnnualLeaveAllowance(payslip.getBaseAmt(), payslip.getAlwAmt());
		// --------------------------------------------------------
		
		LeaveDTO leaveDTO = LeaveDTO.fromEntity(annualLeave);
		
		return leaveDTO;
	}
	
	// 개인 연차 현황(리스트)
	public List<LeaveHistoryDTO> getMyLeaveList(String empId, LocalDate startOfYear, LocalDate endOfYear) {
		return historyRepository.findAnnualLeaveInYear(empId, startOfYear, endOfYear)
				.stream()
				.map(LeaveHistoryDTO::fromEntity)
				.collect(Collectors.toList());
	}
	
	// 관리자용 연차 현황 (리스트)
	public List<LeaveDTO> getAllLeaveList(String empId) {
		List<AnnualLeave> list = leaveRepository.findAllWithEmpInfo(empId);
		
		return list.stream()
				.map(LeaveDTO::fromEntity)
				.collect(Collectors.toList());
	}
	// 연차 개별 조회
	public LeaveDTO getLeaveDetail(Long leaveId) {
		AnnualLeave annualLeave = leaveRepository.findById(leaveId)
				.orElseThrow(() -> new NoSuchElementException("조회된 연차가 없습니다."));
		
		return LeaveDTO.fromEntity(annualLeave);
	}

	// 연차 수정
	@Transactional
	public void modifyLeave(LoginDTO loginDTO, LeaveChangeRequestDTO leaveChangeRequestDTO, Long leaveId) {
		AnnualLeave annualLeave = leaveRepository.findById(leaveId)
				.orElseThrow(() -> new NoSuchElementException("조회된 연차가 없습니다."));
		
		annualLeave.modifyAnnual(loginDTO.getEmpId(), leaveChangeRequestDTO);
	}
	
	// 개인 연차 사용 등록
	@Transactional
	public void createAnnualLeave(Long approvalId) {
		// 결재 문서 조회
		ApprovalDoc approvalDoc = approvalDocRepository.findById(approvalId)
				.orElseThrow(() -> new NoSuchElementException("결재 문서를 찾을 수 없습니다."));
		
		Emp emp = empRepository.findById(approvalDoc.getEmpId())
				.orElseThrow(() -> new NoSuchElementException("사원을 찾을 수 없습니다."));
		
		AnnualLeave annualLeave = leaveRepository.findByEmp_empId(approvalDoc.getEmpId())
				.orElseThrow(() -> new NoSuchElementException("연차 테이블을 찾을 수 없습니다."));
		
		// 연차 사용 시작일과 종료 일자 계산해서 사용한 일수 구함
		double usedDays = (double) (ChronoUnit.DAYS.between(approvalDoc.getStartDate(), approvalDoc.getEndDate()) + 1);
		
		if ("반차".equals(approvalDoc.getLeaveType())) {
			usedDays = 0.5;
		}
		
		String leaveTypeCode = toLeaveCode(approvalDoc.getLeaveType());
		
		// 연차 사용 기록 등록 
		LeaveHistoryDTO  leaveHistoryDTO = LeaveHistoryDTO.builder()
				.emp_id(emp.getEmpId())
				.emp_name(emp.getEmpName())
				.dept_id(emp.getDept().getDeptId())
				.leaveType(leaveTypeCode)
				.startDate(approvalDoc.getStartDate())
				.endDate(approvalDoc.getEndDate())
				.usedDays(usedDays)
				.reason(approvalDoc.getReason())
				.approvalId(approvalDoc.getApprovalId())
				.build();
		
		AnnualLeaveHistory annualLeaveHistory = leaveHistoryDTO.toEntity();
		annualLeaveHistory.setAnnualLeave(annualLeave);
		annualLeaveHistory.setDept(emp.getDept());
		
		historyRepository.save(annualLeaveHistory);
		
		// 사용한 연차 반영
		annualLeave.useAnnual(usedDays);
	}
	
	private static final Map<String, String> LEAVE_TYPE_MAP = Map.of(
		"연차", "ANNUAL",
		"반차", "HALF",
		"병가", "SICK"
	);
	
	private String toLeaveCode(String type) {
		return LEAVE_TYPE_MAP.getOrDefault(type, "UNKNOWN");
	}
			
	// 연차 재계산 (비동기)
	@Async
	@Transactional
	public void recalculateAllAnnualLeaves(WorkPolicy workPolicy) {
		try {
			// 전체 직원의 연차 가져오기
			List<AnnualLeave> annualLeaves = leaveRepository.findAll();
			
			// 변경된 연차 기준에 맞게 업데이트
			for (AnnualLeave leave : annualLeaves) {
				leave.updateTotaldays(workPolicy.getAnnualBasis());
			}
		} catch (Exception e) {
			log.error("연차 계산 중 오류 발생 : " + e.getMessage());
		}
	}

	// 매년 1월 1일에 연차 산정 시작/종료일, 해당연도, 총 연차, 사용 연차, 잔여 연차 수정
	@Transactional
	public void updateAllAnnualLeaves() {
		List<AnnualLeave> leaveList = leaveRepository.findAll();
		
		// 근무정책 조회
		WorkPolicy workPolicy = workPolicyRepository.findFirstByOrderByPolicyIdAsc()
				.orElseThrow(() -> new NoSuchElementException("등록된 근무정책이 없습니다."));
		
		LocalDate now = LocalDate.now();
		// 회계연도에 사용될 현재 연도
		int currentYear = now.getYear();
		
		for (AnnualLeave leave : leaveList) {
			Emp emp = leave.getEmp();
			LocalDate hireDate = emp.getHireDate();
			
			// 연차 산정 시작일과 종료일 수정
			LocalDate newStart = (hireDate.getMonthValue() == 1 && hireDate.getDayOfMonth() == 1) 
					? hireDate // 입사일이 1월 1일이면 그대로 사용
					: LocalDate.of(currentYear, hireDate.getMonth(), hireDate.getDayOfMonth());
			
			LocalDate newEnd = newStart.plusYears(1).minusDays(1);
			
			// 연차 업데이트
			leave.updateAnnual(newStart, newEnd, currentYear, workPolicy.getAnnualBasis());
		}
	}
}
