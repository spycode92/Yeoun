package com.yeoun.order.support;

import com.yeoun.masterData.entity.ProductMst;
import com.yeoun.masterData.entity.RouteHeader;
import com.yeoun.masterData.entity.RouteStep;
import com.yeoun.masterData.repository.ProductMstRepository;
import com.yeoun.masterData.repository.RouteHeaderRepository;
import com.yeoun.masterData.repository.RouteStepRepository;
import com.yeoun.order.entity.WorkOrder;
import com.yeoun.process.service.ProcessTimeCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WorkOrderTimeCalculator {

    private final ProductMstRepository productMstRepository;
    private final RouteHeaderRepository routeHeaderRepository;
    private final RouteStepRepository routeStepRepository;

    // =======================================================
    // 작업지시 예상 시간 조회
    @Transactional(readOnly = true)
    public long calcExpectedMinutes(String prdId, Integer planQty, String routeId) {

        // 1) 제품 조회
        ProductMst product = productMstRepository.findById(prdId)
                .orElseThrow(() -> new RuntimeException("품번 없음: " + prdId));

        // 2) 라우트 헤더 조회
        RouteHeader routeHeader = routeHeaderRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("라우트 없음: " + routeId));

        // 3) 라우트 스텝 조회
        List<RouteStep> routeSteps =
                routeStepRepository.findByRouteHeaderOrderByStepSeqAsc(routeHeader);

        // 4) 계산용 임시 WorkOrder
        WorkOrder tmp = WorkOrder.builder()
                .product(product)
                .planQty(planQty)
                .build();

        // 5) 전체 공정 예상시간 계산
        return ProcessTimeCalculator
                .calcExpectedMinutesForWorkOrderFromRoute(tmp, routeSteps);
    }
}
