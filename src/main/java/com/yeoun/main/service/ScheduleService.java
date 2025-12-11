package com.yeoun.main.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.common.dto.AlarmDTO;
import com.yeoun.common.service.AlarmService;
import com.yeoun.emp.dto.DeptDTO;
import com.yeoun.emp.entity.Dept;
import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.repository.DeptRepository;
import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.leave.dto.LeaveDTO;
import com.yeoun.leave.dto.LeaveHistoryDTO;
import com.yeoun.leave.entity.AnnualLeaveHistory;
import com.yeoun.leave.repository.LeaveHistoryRepository;
import com.yeoun.main.dto.ScheduleDTO;
import com.yeoun.main.dto.ScheduleSharerDTO;
import com.yeoun.main.entity.Schedule;
import com.yeoun.main.entity.ScheduleSharer;
import com.yeoun.main.mapper.ScheduleMapper;
import com.yeoun.main.repository.ScheduleRepository;
import com.yeoun.main.repository.ScheduleSharerRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleService {
	private final ScheduleRepository scheduleRepository;
	private final DeptRepository deptRepository;
	private final EmpRepository empRepository;
	private final LeaveHistoryRepository leaveHistoryRepository;
	private final ScheduleMapper scheduleMapper;
	private final ScheduleSharerRepository scheduleSharerRepository;
	private final AlarmService alarmService;
	// --------------------------------------------------
	
	//일정 등록모달 부서리스트 가져오기
	public List<DeptDTO> getDeptList() {
		List<Dept> deptList = deptRepository.findAll();
		
		return deptList.stream() //부서리스트를 엔터티 -> dto리스트로 변경후 리턴
				.map(dept -> DeptDTO.fromEntity(dept))
				.collect(Collectors.toList());
	}
	
	// 일정 등록로직
	@Transactional
	public void createSchedule(@Valid ScheduleDTO scheduleDTO, List<ScheduleSharerDTO> list) {
		Emp emp = empRepository.findById(scheduleDTO.getCreatedUser()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 직원입니다.111"));

		Schedule schedule = scheduleDTO.toEntity();
		schedule.setEmp(emp);
		String scheduleTitle =schedule.getScheduleTitle();
		// 여기서 Schedule테이블 정보 저장
		scheduleRepository.save(schedule);
		
		// Schedule테이블에 저장된 정보를 토대로 ScheduleSharer테이블에 정보저장
		for(ScheduleSharerDTO DTO : list) {
			// save할 객체 생성
			ScheduleSharer scheduleSharer = new ScheduleSharer();
			// sharer에 공유된 empId로 emp객체 찾기
			Emp sharerEmp = empRepository.findById(DTO.getEmpId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 직원입니다.111"));;
			
			// scheduleSharer엔티티에 schedule객체, emp 객체 추가 
			scheduleSharer.setSchedule(schedule);
			scheduleSharer.setSharedEmp(sharerEmp);
			// 엔티티 값 저장
			scheduleSharerRepository.save(scheduleSharer);
			
			// 공유일정 공유자들에게 알림 설정
			String alarmMessage = """
			        새로운 공유 일정이 등록되었습니다.['%s']
					""".formatted(scheduleTitle);
			
			AlarmDTO alarmDTO = AlarmDTO.builder()
					.empId(sharerEmp.getEmpId())
					.alarmMessage(alarmMessage)
					.alarmStatus("N")
					.alarmLink("/main")
					.build();
			alarmService.sendPersonalMessage(alarmDTO);
		}
		
		
	}
	
	// 일정 목록 조회로직
	public List<ScheduleDTO> getScheduleList(LocalDateTime startDate, LocalDateTime endDate, Authentication authentication) {
		String empId = authentication.getName();
		// empId로 로그인한 emp엔티티 정보 조회
		Emp emp = empRepository.findById(empId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 직원입니다.111"));
		String myDeptId = emp.getDept().getDeptId();
		String myDeptName = emp.getDept().getDeptName();
		
	    LocalDateTime startOfDay = startDate.with(java.time.LocalTime.MIN);//해당일자의 00시00분00초  
	    LocalDateTime endOfDay   = endDate.with(java.time.LocalTime.MAX);//해당일자의 23시59분59초
		// 일정목록 조회
	    List<Schedule> scheduleList = scheduleRepository.getIndividualSchedule(empId, startOfDay, endOfDay);
		
		return scheduleList.stream().map(ScheduleDTO::fromEntity).collect(Collectors.toList());
	}
	
	//단일 일정 조회
	public ScheduleDTO getSchedule(Long scheduleId) {
		Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 일정입니다."));
		
		return ScheduleDTO.fromEntity(schedule);
	}

	//일정 정보 수정
	@Transactional
	public void modifySchedule(@Valid ScheduleDTO scheduleDTO, List<ScheduleSharerDTO> list) {
		// 입력된 스케줄id로 기존 스케줄로우 정보 받아오기
		Schedule schedule = scheduleRepository.findById(scheduleDTO.getScheduleId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 일정입니다."));
		
		String scheduleTitle = schedule.getScheduleTitle();
		// changeSchedule 메서드 사용해서 수정된 정보 저장
		schedule.changeSchedule(scheduleDTO);
		
		// scheduleDTO의 scheduleType이 "share"일 경우 기존의 공유자리스트 제거후 다시 저장
		if("share".equals(scheduleDTO.getScheduleType())) {
			// 기존의 공유자목록 조회
			List<ScheduleSharer> sharers = scheduleSharerRepository.findBySchedule_ScheduleId(scheduleDTO.getScheduleId());
			// 이전 정보 삭제
			scheduleSharerRepository.deleteAll(sharers);
			// Schedule테이블에 저장된 정보를 토대로 ScheduleSharer테이블에 정보저장
			for(ScheduleSharerDTO DTO : list) {
				// save할 객체 생성
				ScheduleSharer scheduleSharer = new ScheduleSharer();
				// sharer에 공유된 empId로 emp객체 찾기
				Emp sharerEmp = empRepository.findById(DTO.getEmpId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 직원입니다."));;
				
				// scheduleSharer엔티티에 schedule객체, emp 객체 추가 
				scheduleSharer.setSchedule(schedule);
				scheduleSharer.setSharedEmp(sharerEmp);
				// 엔티티 값 저장
				scheduleSharerRepository.save(scheduleSharer);
				
				// 공유일정 공유자들에게 알림 설정
				String alarmMessage = """
				        새로운 공유 일정이 등록되었습니다.['%s']
						""".formatted(scheduleTitle);
				
				AlarmDTO alarmDTO = AlarmDTO.builder()
						.empId(sharerEmp.getEmpId())
						.alarmMessage(alarmMessage)
						.alarmStatus("N")
						.alarmLink("/main")
						.build();
				alarmService.sendPersonalMessage(alarmDTO);
			}
		}
	}
	
	//일정 정보 삭제
	@Transactional
	public void deleteSchedule(@Valid ScheduleDTO scheduleDTO, Authentication authentication) {
		Schedule schedule = scheduleRepository.findById(scheduleDTO.getScheduleId()).orElseThrow(() -> new EntityNotFoundException("존재하지않는 일정입니다!!"));
		
		scheduleRepository.delete(schedule);
	}
	
	//startDate, endDate의 연차정보 가져오기
	public List<LeaveHistoryDTO> getLeaveHistoryList(LocalDateTime startDateTime, LocalDateTime endDateTime,
			LoginDTO loginDTO) {
		
		String empId = loginDTO.getEmpId();
		String deptId = loginDTO.getDeptId();
		
		LocalDate startDate = startDateTime.toLocalDate();
		LocalDate endDate = endDateTime.toLocalDate();
		List<AnnualLeaveHistory> leaveHistoryList = leaveHistoryRepository.findLeaveHistorySchedule(startDate, endDate, empId, deptId);
		return leaveHistoryList.stream().map(LeaveHistoryDTO::fromEntity).collect(Collectors.toList());
	}

	public List<Map<String, Object>> getOrganizationList() {
		
		List<Map<String, Object>> organizationList = scheduleMapper.getOrganizationList();
		
		return organizationList;
	}


}

































