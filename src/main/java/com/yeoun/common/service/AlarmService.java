package com.yeoun.common.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.yeoun.common.dto.AlarmDTO;
import com.yeoun.common.e_num.AlarmDestination;
import com.yeoun.common.entity.Alarm;
import com.yeoun.common.repository.AlarmRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlarmService {
    private final SimpMessagingTemplate messagingTemplate;
    private final AlarmRepository alarmRepository;
    
    // 페이지 새로고침 뱃지 알림
    public void sendAlarmMessage(AlarmDestination dest, String message) {
        messagingTemplate.convertAndSend(dest.getDestination(), message);
    }
    
    // 개인 알림(1명)
    @Transactional
    public void sendPersonalMessage(AlarmDTO alarmDTO) {
    	// 받은 alarmDTO를 alarm엔티티로 변경후 저장
    	Alarm alarm = alarmDTO.toEntity(); 
    	alarmRepository.save(alarm);
    	
    	// 알림을 보낸 대상에게 새알림 도착 메세지 발송
    	messagingTemplate.convertAndSendToUser(alarmDTO.getEmpId(), "/alarm", alarmDTO.getAlarmMessage());
    }
    
    // 로그인유저의 알림정보 가져오기
	public List<AlarmDTO> getAlarmData(String empId, String startDate, String endDate) {
	    LocalDateTime startDateTime;
	    LocalDateTime endDateTime;
	    // 둘 다 없는 경우 → 전체 기간 조회
	    if ((startDate == null || startDate.isBlank()) &&
	        (endDate == null || endDate.isBlank())) {

	        startDateTime = LocalDateTime.of(1970, 1, 1, 0, 0);
	        endDateTime   = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

	    } else {
	        LocalDate start = (startDate == null || startDate.isBlank())
	                ? LocalDate.of(1970, 1, 1)
	                : LocalDate.parse(startDate);

	        LocalDate end = (endDate == null || endDate.isBlank())
	                ? LocalDate.of(9999, 12, 31)
	                : LocalDate.parse(endDate);

	        startDateTime = start.atStartOfDay();
	        endDateTime   = end.plusDays(1).atStartOfDay();
	    }
        
        // Repository 호출
        List<Alarm> alarms = alarmRepository
            .findAllByEmpIdAndCreatedDateBetweenOrderByCreatedDateDesc(
                empId, startDateTime, endDateTime
            );
        
        // DTO 변환
        return alarms.stream()
            .map(AlarmDTO::fromEntity)
            .toList();
    
	}
	
	// 알림 읽음처리
	@Transactional
	public void updateAlarmStatus(String empId, String startDate, String endDate, String alarmStatus) {
        // String -> LocalDateTime 변환
        LocalDateTime startDateTime = LocalDate.parse(startDate)
            .atStartOfDay();  
        
        LocalDateTime endDateTime = LocalDate.parse(endDate)
            .plusDays(1)      
            .atStartOfDay();
        
		List<Alarm> alarms = alarmRepository.findAllByEmpIdAndCreatedDateBetweenOrderByCreatedDateDesc(empId, startDateTime, endDateTime);
		
	    for (Alarm alarm : alarms) {
	        alarm.setAlarmStatus(alarmStatus);   // "Y" 읽음 처리
	    }
		
	}
	
	//읽지않은 알림 불러오기
	public List<AlarmDTO> getAlarmStatus(String empId, String alarmStatus) {
		if(alarmStatus == null || alarmStatus.isEmpty()) {
			alarmStatus = "N";
		}
		return alarmRepository.findAllByEmpIdAndAlarmStatus(empId, alarmStatus).stream()
				.map(AlarmDTO::fromEntity).toList();
	}
}
