package com.yeoun.order.repository;

import com.yeoun.order.entity.WorkerProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerProcessRepository extends JpaRepository<WorkerProcess, Long> {

    List<WorkerProcess> findAllBySchedule_Work_OrderId(String orderId);
    
    // 작업지시번호 + 공정ID 기준으로 담당자 1명 조회
    Optional<WorkerProcess> findFirstBySchedule_Work_OrderIdAndProcess_ProcessId(String orderId, String processId);


}
