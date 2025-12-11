package com.yeoun.order.repository;

import com.yeoun.order.entity.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {

    // 일치하는 작업지시 번호 중 가장 최근 스케줄 조회
    Optional<WorkSchedule> findTopByWork_OrderIdOrderByScheduleIdAsc(String id);
}
