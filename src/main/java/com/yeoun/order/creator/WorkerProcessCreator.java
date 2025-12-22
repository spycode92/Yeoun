package com.yeoun.order.creator;

import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.masterData.repository.ProcessMstRepository;
import com.yeoun.order.dto.WorkOrderRequest;
import com.yeoun.order.entity.WorkSchedule;
import com.yeoun.order.entity.WorkerProcess;
import com.yeoun.order.repository.WorkerProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WorkerProcessCreator {

    private final EmpRepository empRepository;
    private final ProcessMstRepository processMstRepository;
    private final WorkerProcessRepository workerProcessRepository;

    // =======================================================
    // 공정별 작업자 할당
    @Transactional
    public void create(WorkOrderRequest dto, WorkSchedule schedule) {
        Map<String, String> processWorkerMap = Map.of(
                "PRC-BLD", dto.getPrcBld(),
                "PRC-FLT", dto.getPrcFlt(),
                "PRC-FIL", dto.getPrcFil(),
                "PRC-CAP", dto.getPrcCap(),
                "PRC-QC", dto.getPrcQc(),
                "PRC-LBL", dto.getPrcLbl()
        );

        for (Map.Entry<String, String> entry : processWorkerMap.entrySet()) {

            String processId = entry.getKey();    // ex: "PRC_BLD"
            String workerId  = entry.getValue();  // ex: dto.getPrcBld()

            WorkerProcess wp = WorkerProcess.builder()
                    .schedule(schedule)
                    .worker(empRepository.findByEmpId(workerId)
                            .orElseThrow(() -> new RuntimeException(processId + "작업자 불일치")))
                    .process(processMstRepository.findByProcessId(processId)
                            .orElseThrow(() -> new RuntimeException(processId + "공정 불일치")))
                    .build();

            workerProcessRepository.save(wp);
        }
    }
}
