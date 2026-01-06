package com.yeoun.order.creator;

import com.yeoun.masterData.entity.RouteStep;
import com.yeoun.masterData.repository.RouteHeaderRepository;
import com.yeoun.masterData.repository.RouteStepRepository;
import com.yeoun.order.entity.WorkOrder;
import com.yeoun.process.entity.WorkOrderProcess;
import com.yeoun.process.repository.WorkOrderProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProcessListCreator {

    private final RouteStepRepository routeStepRepository;
    private final RouteHeaderRepository routeHeaderRepository;
    private final WorkOrderProcessRepository workOrderProcessRepository;

    // =======================================================
    // 새 공정 생성 로직
    @Transactional
    public void create (WorkOrder order, String id) {

        String routeId = order.getRouteId();


        // 1) ROUTE_STEP 전체 조회
        List<RouteStep> routeSteps =
                routeStepRepository.findByRouteHeaderOrderByStepSeqAsc(
                        routeHeaderRepository.findById(routeId)
                                .orElseThrow(() -> new RuntimeException("해당 품목의 라우팅 정보가 없음! : " + routeId))
                );


        // 2) 데이터를 기반으로 WORK_ORDER_PROCESS 생성
        for (RouteStep routeStep : routeSteps) {
            WorkOrderProcess wop = new WorkOrderProcess();
            wop.setWopId("WOP-" + order.getOrderId() + "-" + String.format("%02d", routeStep.getStepSeq()));
            wop.setWorkOrder(order);
            wop.setRouteStep(routeStep);
            wop.setProcess(routeStep.getProcess());
            wop.setStepSeq(routeStep.getStepSeq());
            wop.setStatus("READY");
            wop.setCreatedId(id);

            workOrderProcessRepository.save(wop);
        }

    }
}
