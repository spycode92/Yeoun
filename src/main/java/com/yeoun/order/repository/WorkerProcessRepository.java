package com.yeoun.order.repository;

import com.yeoun.order.entity.WorkerProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkerProcessRepository extends JpaRepository<WorkerProcess, Long> {

    List<WorkerProcess> findAllBySchedule_Work_OrderId(String orderId);


}
