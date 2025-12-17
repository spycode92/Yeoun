package com.yeoun.order.service;

import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.equipment.repository.ProdLineRepository;
import com.yeoun.masterData.repository.ProductMstRepository;
import com.yeoun.order.creator.ProcessListCreator;
import com.yeoun.order.creator.WorkerProcessCreator;
import com.yeoun.order.dto.WorkOrderRequest;
import com.yeoun.order.entity.WorkOrder;
import com.yeoun.order.entity.WorkSchedule;
import com.yeoun.order.entity.WorkerProcess;
import com.yeoun.order.repository.WorkOrderRepository;
import com.yeoun.order.repository.WorkScheduleRepository;
import com.yeoun.order.repository.WorkerProcessRepository;
import com.yeoun.order.support.ColorCodeGenerator;
import com.yeoun.order.support.WorkOrderIdGenerator;
import com.yeoun.outbound.service.OutboundService;
import com.yeoun.production.entity.ProductionPlan;
import com.yeoun.production.entity.ProductionPlanItem;
import com.yeoun.production.enums.ProductionStatus;
import com.yeoun.production.repository.ProductionPlanItemRepository;
import com.yeoun.production.repository.ProductionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class OrderCommandService {

    private final WorkOrderIdGenerator workOrderIdGenerator;
    private final ColorCodeGenerator colorCodeGenerator;
    private final WorkerProcessCreator workerProcessCreator;
    private final ProcessListCreator processListCreator;

    private final WorkOrderRepository workOrderRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final WorkerProcessRepository workerProcessRepository;

    private final EmpRepository empRepository;
    private final OutboundService outboundService;
    private final ProdLineRepository prodLineRepository;
    private final ProductMstRepository productMstRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final ProductionPlanItemRepository productionPlanItemRepository;



    // =======================================================
    // 작업지시 등록
    @Transactional
    public void createWorkOrder(WorkOrderRequest dto, String id) {

        log.info("create dto!!!!! :::::::: " + dto);

        // 1) 새 작업지시 번호 생성
        String orderId = workOrderIdGenerator.generate();

        // 2) 작업지시 등록
        WorkOrder order = WorkOrder.builder()
                .orderId(orderId)
                .planId(dto.getPlanId())
                .product(productMstRepository.findById(dto.getPrdId())
                        .orElseThrow(() -> new RuntimeException("품번을 찾을 수 없음!")))
                .planQty(dto.getPlanQty())
                .planStartDate(dto.getPlanStartDate())
                .planEndDate(dto.getPlanEndDate())
                .routeId(dto.getRouteId())
                .line(prodLineRepository.findById(dto.getLineId())
                        .orElseThrow(() -> new RuntimeException("라인을 찾을 수 없음!")))
                .status("CREATED")
                .createdEmp(empRepository.findByEmpId(id)
                        .orElseThrow(() -> new RuntimeException("작성자를 찾을 수 없음!")))
                .remark(dto.getRemark())
                .build();
        workOrderRepository.save(order);

        // 3) 생산계획 상태 변경
        ProductionPlan plan = productionPlanRepository.findById(dto.getPlanId())
                .orElseThrow(() -> new RuntimeException("생산 계획을 찾을 수 없음!"));
        plan.setStatus(ProductionStatus.IN_PROGRESS);

        List<ProductionPlanItem> planItems = productionPlanItemRepository.findByPlanId(dto.getPlanId());
        for (ProductionPlanItem item : planItems) {
            item.setStatus(ProductionStatus.IN_PROGRESS);
        }

        // 4) 작업스케줄 생성
        WorkSchedule schedule = WorkSchedule.builder()
                .work(workOrderRepository.findById(orderId)
                        .orElseThrow(() -> new RuntimeException("작업지시 번호를 찾을 수 없음!")))
                .line(order.getLine())
                .startDate(order.getPlanStartDate())
                .endDate(order.getPlanEndDate())
                .colorCode(colorCodeGenerator.generate(orderId))
                .build();
        workScheduleRepository.save(schedule);

        // 5) 방금 생성된 작업스케줄 번호 조회
        WorkSchedule newSchedule = workScheduleRepository.findTopByWork_OrderIdOrderByScheduleIdAsc(orderId)
                .orElseThrow(() -> new RuntimeException("일치하는 번호 없음!"));

        // 6) 작업자 저장
        workerProcessCreator.create(dto, newSchedule);

        // 7) 새 공정 생성
        processListCreator.create(order, id);

    }


    // =======================================================
    // 작업지시 확정 및 삭제
    @Transactional
    public void modifyOrderStatus(String id, String status) {
        WorkOrder order = workOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당하는 작업 번호가 없습니다."));
        order.setStatus(status);

        if (status.equals("CANCELED"))
            outboundService.canceledMaterialOutbound(id);

    }

    // =======================================================
    // 작업지시 수정
    @Transactional
    public void updateOrder(String id, Map<String, String> map) {

        WorkOrder order = workOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("작업지시가 없음"));
        order.setRemark(map.get("remark"));

        map.forEach((code, worker) -> {
            if ("remark".equals(code)) return;

            WorkerProcess wp = workerProcessRepository
                    .findFirstBySchedule_Work_OrderIdAndProcess_ProcessId(id, code)
                    .orElseThrow();

            Emp emp = empRepository.findByEmpId(worker)
                    .orElseThrow(() -> new RuntimeException("일치하는 작업자가 없음"));

            wp.setWorker(emp);
        });
    }
}
