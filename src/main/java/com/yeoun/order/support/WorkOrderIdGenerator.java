package com.yeoun.order.support;

import com.yeoun.order.entity.WorkOrder;
import com.yeoun.order.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WorkOrderIdGenerator {

    private final WorkOrderRepository workOrderRepository;

    // =======================================================
    // 작업지시 번호 생성
    public String generate(){
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "WO-" + today;

        Optional<WorkOrder> lastOrder = workOrderRepository.findTopByOrderIdStartingWithOrderByOrderIdDesc(prefix);

        if (lastOrder.isEmpty()){
            return prefix + "-0001";
        }

        String lastId = lastOrder.get().getOrderId();

        int nextSeq = Integer.parseInt(lastId.substring(prefix.length() + 1));
        nextSeq++;

        return String.format("%s-%04d", prefix, nextSeq);
    }
}
