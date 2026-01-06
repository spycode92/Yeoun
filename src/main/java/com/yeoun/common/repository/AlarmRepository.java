package com.yeoun.common.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeoun.common.dto.AlarmDTO;
import com.yeoun.common.entity.Alarm;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {
    // 정렬 추가 (최신순)
    List<Alarm> findAllByEmpIdAndCreatedDateBetweenOrderByCreatedDateDesc(
        String empId, 
        LocalDateTime startDateTime, 
        LocalDateTime endDateTime
    );
    
    // 알림읽음상태별 조회
	List<Alarm> findAllByEmpIdAndAlarmStatus(String empId, String alarmStatus);
}
