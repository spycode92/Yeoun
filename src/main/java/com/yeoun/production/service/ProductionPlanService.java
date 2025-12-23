package com.yeoun.production.service;

import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.inventory.repository.InventoryRepository;
import com.yeoun.production.dto.*;
import com.yeoun.production.entity.ProductionPlan;
import com.yeoun.production.entity.ProductionPlanItem;
import com.yeoun.production.enums.BomStatus;
import com.yeoun.production.enums.ProductionStatus;
import com.yeoun.production.repository.ProductionPlanItemRepository;
import com.yeoun.production.repository.ProductionPlanRepository;
import com.yeoun.sales.dto.OrderItemDTO;
import com.yeoun.sales.dto.OrderPlanSuggestDTO;
import com.yeoun.sales.entity.OrderItem;
import com.yeoun.sales.repository.OrderItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class ProductionPlanService {

    private final ProductionPlanRepository planRepo;
    private final ProductionPlanItemRepository itemRepo;
    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final EmpRepository employeeRepository;

    /* =============================================================
    공통 함수: OrderItem → OrderItemDTO 변환
 ============================================================= */
 private OrderItemDTO convertToOrderItemDTO(OrderItem oi) {

     // ⭐ EMP_ID → 직원명 조회 (없으면 "미지정")
     String empName = employeeRepository.findById(oi.getOrder().getEmpId())
             .map(emp -> emp.getEmpName())
             .orElse("미지정");

     return new OrderItemDTO(
    		    oi.getOrderItemId(),
    		    oi.getOrderId(),
    		    oi.getPrdId(),
    		    oi.getProduct().getPrdName(),
    		    oi.getOrderQty(),   // ✅ BigDecimal 그대로

    		    oi.getOrder().getClient().getClientName(),
    		    oi.getOrder().getClient().getManagerName(),
    		    oi.getOrder().getClient().getManagerTel(),
    		    oi.getOrder().getClient().getManagerEmail(),

    		    oi.getOrder().getOrderDate(),
    		    oi.getOrder().getDeliveryDate(),

    		    empName
    		);
 }


    /* ================================
        생산계획 ID 생성
    ================================ */
    private String generatePlanId() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PLD" + today + "-";

        String last = planRepo.findLastPlanId(prefix);
        int seq = (last == null) ? 1 : Integer.parseInt(last.substring(last.lastIndexOf("-") + 1)) + 1;

        return prefix + String.format("%03d", seq);
    }

    /* ================================
        생산계획 상세 ID 생성
    ================================ */
    private String generatePlanItemId() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PIM" + today + "-";

        String last = itemRepo.findLastPlanItemId(prefix);
        int seq = (last == null) ? 1 : Integer.parseInt(last.substring(last.lastIndexOf("-") + 1)) + 1;

        return prefix + String.format("%03d", seq);
    }


    /* ================================
    생산계획 생성 (수동, 모달 선택 기반)
 ================================ */
@Transactional
public String createPlan(List<PlanCreateItemDTO> items, String createdBy, String memo) {

    if (items == null || items.isEmpty()) {
        throw new IllegalArgumentException("생산계획 생성 실패: 선택된 수주 항목이 없습니다.");
    }

    String prdId = null;
    int totalPlanQty = 0;

    List<Long> orderItemIdList = new ArrayList<>();

    for (PlanCreateItemDTO dto : items) {

        OrderItem oi = orderItemRepository.findById(dto.getOrderItemId())
                .orElseThrow(() -> new IllegalArgumentException("OrderItem 없음: " + dto.getOrderItemId()));

        // 제품 통일성 체크
        if (prdId == null) prdId = oi.getPrdId();
        else if (!prdId.equals(oi.getPrdId()))
            throw new IllegalArgumentException("생산계획은 동일 제품만 묶어서 생성할 수 있습니다.");

        // 프론트에서 선택한 qty 사용
        int qty = dto.getQty();
        if (qty <= 0) throw new IllegalArgumentException("잘못된 생산 수량: " + qty);

        totalPlanQty += qty;
        orderItemIdList.add(oi.getOrderItemId());
    }

    // ================================
    // PLAN MASTER 생성
    // ================================
    String planId = generatePlanId();

    ProductionPlan plan = ProductionPlan.builder()
            .planId(planId)
            .prdId(prdId)
            .planQty(totalPlanQty)
            .planDate(LocalDate.now())
            .dueDate(LocalDate.now().plusDays(7))
            .status(ProductionStatus.PLANNING)
            .planMemo(memo)
            .createdBy(createdBy)
            .build();

    planRepo.save(plan);


    // ================================
    // PLAN DETAIL 생성
    // ================================
    for (PlanCreateItemDTO dto : items) {

        OrderItem oi = orderItemRepository.findById(dto.getOrderItemId())
                .orElseThrow(() -> new IllegalArgumentException("OrderItem 없음: " + dto.getOrderItemId()));

        int qty = dto.getQty(); // ⭐ 선택된 qty 그대로 사용

        ProductionPlanItem detail = ProductionPlanItem.builder()
                .planItemId(generatePlanItemId())
                .planId(planId)
                .prdId(prdId)
                .orderItemId(oi.getOrderItemId())
                .orderQty(oi.getOrderQty())   // 원 수주 수량은 기록만
                .planQty(BigDecimal.valueOf(qty)) // ⭐ 실제 생산계획 수량
                .bomStatus(BomStatus.WAIT)
                .status(ProductionStatus.PLANNING)
                .itemMemo("")
                .createdBy(createdBy)
                .build();

        itemRepo.save(detail);
    }


    // ================================
    // ORDER ITEM 상태 변경
    // ================================
    orderItemIdList.forEach(orderItemRepository::updateStatusToPlanned);

    return planId;
}

    /* ================================
       생산계획 목록 조회
    ================================ */
    public List<ProductionPlanListDTO> getPlanList() {
        return planRepo.findPlanList();
    }

    /* ================================
       생산 추천 목록 생성
    ================================ */
    public List<OrderPlanSuggestDTO> getPlanSuggestions(String group) {

        List<Map<String, Object>> groups = orderItemRepository.findConfirmedGrouped(group);

        List<Map<String, Object>> stockList = inventoryRepository.findCurrentStockGrouped();

        Map<String, Integer> stockMap = new HashMap<>();
        for (Map<String, Object> s : stockList) {
            stockMap.put(
                    (String) s.get("prdId"),
                    ((BigDecimal) s.get("currentStock")).intValue()
            );
        }

        List<OrderPlanSuggestDTO> results = new ArrayList<>();

        for (Map<String, Object> g : groups) {

            String prdId = (String) g.get("prdId");
            String prdName = (String) g.get("prdName");

            int totalOrderQty = ((BigDecimal) g.get("totalOrderQty")).intValue();
            int orderCount = ((Number) g.get("orderCount")).intValue();
            LocalDate earliestDelivery = (LocalDate) g.get("earliestDeliveryDate");

            int currentStock = stockMap.getOrDefault(prdId, 0);
            int shortageQty = Math.max(totalOrderQty - currentStock, 0);

            // ================================
            // 🔍 LOGGING
            // ================================
            log.info("============ 🔎 생산 추천 계산 ============");
            log.info("제품ID = {}, 제품명 = {}", prdId, prdName);
            log.info("총 주문수량(totalOrderQty) = {}", totalOrderQty);
            log.info("현재 재고(currentStock) = {}", currentStock);
            log.info("수주 건수(orderCount) = {}", orderCount);

            // 1) 수주 상세 목록 조회
            List<Map<String, Object>> items = orderItemRepository.findItemsByProduct(prdId);

            // 2) 정확한 BOM 기반 원자재 부족 계산
            boolean bomShortage = checkBomShortage(prdId, totalOrderQty);
            String bomStatus = bomShortage ? "부족" : "정상";

            // ================================
            // 🔍 BOM 결과 LOGGING
            // ================================
            log.info("BOM 부족여부(bomShortage) = {}", bomShortage);
            log.info("BOM 상태(bomStatus) = {}", bomStatus);

            // 3) DTO 변환
            List<OrderPlanSuggestDTO.OrderItemInfo> orderItems = items.stream()
                .map(i -> new OrderPlanSuggestDTO.OrderItemInfo(
                        ((Number) i.get("ORDER_ITEM_ID")).longValue(),
                        (String) i.get("ORDER_ID"),
                        ((Number) i.get("ORDER_QTY")).intValue(),
                        (String) i.get("dueDate"),
                        (String) i.get("CLIENT_NAME"),
                        (String) i.get("MANAGER_NAME"),
                        (String) i.get("MANAGER_TEL"),
                        (String) i.get("MANAGER_EMAIL"),
                        (String) i.get("PRD_NAME")
                ))
                .toList();

            results.add(
                OrderPlanSuggestDTO.builder()
                    .prdId(prdId)
                    .prdName(prdName)
                    .totalOrderQty(totalOrderQty)
                    .currentStock(currentStock)
                    .shortageQty(shortageQty)
                    .needProduction(shortageQty > 0 ? "YES" : "NO")
                    .orderCount(orderCount)
                    .earliestDeliveryDate(
                            earliestDelivery != null ? earliestDelivery.toString() : "-"
                    )
                    .bomStatus(bomStatus)
                    .orderItems(orderItems)
                    .build()
            );
        }

        return results;
    }


    /* ============================
        자동 추천 기반 생산계획 생성
    ============================ */
    @Transactional
    public String createAutoPlan(List<Map<String, Object>> requestList, String createdBy, String memo) {

        if (requestList == null || requestList.isEmpty()) {
            throw new IllegalArgumentException("자동 생산계획 생성 실패: 요청 데이터 없음");
        }

        StringBuilder resultMsg = new StringBuilder();

        for (Map<String, Object> req : requestList) {

            String prdId = (String) req.get("prdId");
            Integer planQty = (Integer) req.get("planQty");

            if (prdId == null || planQty == null)
                throw new IllegalArgumentException("잘못된 요청 데이터입니다.");

            String planId = generatePlanId();

            ProductionPlan plan = ProductionPlan.builder()
                    .planId(planId)
                    .prdId(prdId)
                    .planQty(planQty)
                    .planDate(LocalDate.now())
                    .dueDate(LocalDate.now().plusDays(7))
                    .status(ProductionStatus.PLANNING)
                    .planMemo(memo)
                    .createdBy(createdBy)
                    .build();

            planRepo.save(plan);

            // 상세 정보 저장
            List<Map<String, Object>> orderItems =
                    (List<Map<String, Object>>) req.get("orderItems");

            if (orderItems != null) {
                for (Map<String, Object> item : orderItems) {

                    Long orderItemId = Long.valueOf(item.get("orderItemId").toString());

                    OrderItem oi = orderItemRepository.findById(orderItemId)
                            .orElseThrow(() -> new IllegalArgumentException("OrderItem 찾을 수 없음: " + orderItemId));

                    ProductionPlanItem detail = ProductionPlanItem.builder()
                            .planItemId(generatePlanItemId())
                            .planId(planId)
                            .prdId(prdId)
                            .orderItemId(oi.getOrderItemId())
                            .orderQty(oi.getOrderQty())
                            .planQty(oi.getOrderQty())
                            .bomStatus(BomStatus.WAIT)
                            .status(ProductionStatus.PLANNING)
                            .createdBy(createdBy)
                            .build();

                    itemRepo.save(detail);
                }
            }

            resultMsg.append(planId).append(" 생성완료, ");
        }

        return resultMsg.toString();
    }


    /* ============================
        생산계획 상세보기 모달
    ============================ */
    @Transactional(readOnly = true)
    public PlanDetailDTO getPlanDetailForModal(String planId) {

        ProductionPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("생산계획 없음: " + planId));

        List<ProductionPlanItem> planItems = itemRepo.findByPlanId(planId);

        // 제품별 병합
        Map<String, ProductionPlanItemDTO> merged = new HashMap<>();

        for (ProductionPlanItem item : planItems) {

            String prdId = item.getPrdId();
            int qty = item.getPlanQty().intValue();

            merged.merge(prdId,
                    new ProductionPlanItemDTO(
                            item.getPlanItemId(),
                            item.getPrdId(),
                            item.getProduct().getPrdName(),
                            qty,
                            item.getBomStatus().name(),
                            item.getStatus().name()
                    ),
                    (oldVal, newVal) -> {
                        oldVal.setPlanQty(oldVal.getPlanQty() + qty);
                        return oldVal;
                    });
        }

        // 수주 매핑
        Map<String, List<OrderItemDTO>> orderItemMap = new HashMap<>();

        for (ProductionPlanItem item : planItems) {

            Long orderItemId = Long.valueOf(item.getOrderItemId());
            OrderItem oi = orderItemRepository.findById(orderItemId).orElse(null);

            if (oi != null) {
                OrderItemDTO dto = convertToOrderItemDTO(oi);
                orderItemMap.computeIfAbsent(item.getPrdId(), k -> new ArrayList<>()).add(dto);
            }
        }

        String itemName = planItems.isEmpty()
                ? ""
                : planItems.get(0).getProduct().getPrdName();

     // ⭐ 생성자 이름 조회 (empId → empName)
        String createdByName = employeeRepository
                .findById(plan.getCreatedBy())
                .map(emp -> emp.getEmpName())
                .orElse("미지정");
        
        return new PlanDetailDTO(
                plan.getPlanId(),
                plan.getCreatedAt().toString(),
                itemName,
                plan.getPlanQty(),
                plan.getStatus().name(),
                plan.getPlanMemo(),
                createdByName,
                new ArrayList<>(merged.values()),
                orderItemMap
        );
    }


    /* ============================
        공통 조회 API
    ============================ */
    public List<OrderItemDTO> getOrderItemsByProduct(String prdId) {

      //  List<OrderItem> list = orderItemRepository.findByPrdId(prdId);
        List<OrderItem> list = orderItemRepository.findByPrdIdAndItemStatus(prdId, "CONFIRMED");

        List<OrderItemDTO> dtoList = new ArrayList<>();

        for (OrderItem oi : list) {
            dtoList.add(convertToOrderItemDTO(oi));  // ⭐ 공통 함수 사용
        }

        return dtoList;
    }
      
 
    /**
     * 특정 제품의 원자재 부족 여부 계산 (정확 버전)
     * @param prdId         제품ID
     * @param totalOrderQty 이번 추천에서 생산해야 하는 총 수량
     */
    private boolean checkBomShortage(String prdId, int totalOrderQty) {

        // 주문이 0개면 굳이 원자재 검사할 필요 없음
        if (totalOrderQty <= 0) {
            log.info("🔎 prdId={} : totalOrderQty=0 → BOM 검사 스킵 (부족 아님으로 처리)", prdId);
            return false;
        }

        // ✔ 제품 BOM 조회
        List<Map<String, Object>> bomList = planRepo.findBomItems(prdId);

        log.info("🔍 prdId={} 의 BOM 개수 = {}", prdId, bomList.size());
        log.info("🔍 prdId={} 의 BOM = {}", prdId, bomList);

        for (Map<String, Object> bom : bomList) {

            String matId = (String) bom.get("matId");
            BigDecimal matQty = (BigDecimal) bom.get("matQty");

            if (matQty == null) {
                log.warn("⚠ MAT_QTY null → 0으로 처리. prdId={}, matId={}", prdId, matId);
                matQty = BigDecimal.ZERO;
            }

            // ▶ 필요한 총 원자재 수량 : (1개 생산에 필요한 수량 × 주문 총 수량)
            BigDecimal required = matQty.multiply(BigDecimal.valueOf(totalOrderQty));

            log.info("  --------------------------------------------------");
            log.info("  🧮 원자재 검사 시작 → matId={}", matId);
            log.info("   • 1개 생산당 필요수량(matQty) = {}", matQty);
            log.info("   • 전체 주문수량(totalOrderQty) = {}", totalOrderQty);
            log.info("   • 전체 주문에 필요한 총 원자재(required) = {}", required);

            // ▶ 현재 재고 조회
            Map<String, Object> stock = inventoryRepository.findMaterialStock(matId);

            // ⚠ 재고 데이터 자체가 없으면 → 바로 부족
            if (stock == null) {
                log.warn("❌ 재고 테이블에 데이터 없음 → 부족 처리 :: matId={}", matId);
                return true;
            }

            log.info("   • 재고조회 결과(stock raw) = {}", stock);

            BigDecimal current = new BigDecimal(stock.get("ivAmount").toString());
            log.info("   • 현재 재고(current) = {}", current);

            // ⚠ 재고가 필요한 수량보다 적으면 즉시 부족
            if (current.compareTo(required) < 0) {
                log.warn("❌ 원자재 부족 발생!");
                log.warn("   - matId={} ", matId);
                log.warn("   - 필요한 required={} ", required);
                log.warn("   - 현재 current={} ", current);
                return true;
            }

            log.info("✅ 원자재 충분함 → matId={} (required={} / current={})",
                    matId, required, current);
        }

        log.info("✅ prdId={} : 모든 원자재 충분 → BOM 정상", prdId);
        return false;
    }
    
    /* ============================
    		생산계획 취소
  		============================ */    
    
    @Transactional
    public void cancelProductionPlan(String planId, String empId) {

        // 1️⃣ 생산계획 조회
        ProductionPlan plan = planRepo.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("생산계획이 존재하지 않습니다."));

        // 2️⃣ 상태 체크 (검토대기만 가능)
        if (plan.getStatus() != ProductionStatus.PLANNING) {
            throw new IllegalStateException(
                "검토대기 상태의 생산계획만 취소할 수 있습니다."
            );
        }

        // 3️⃣ 생산계획 상태 취소
        plan.setStatus(ProductionStatus.CANCELLED);
        plan.setUpdatedBy(empId);

        // 4️⃣ 생산계획 상세 조회
        List<ProductionPlanItem> planItems =
            itemRepo.findByPlanId(planId);

        for (ProductionPlanItem item : planItems) {

            // 4-1️⃣ 생산계획 ITEM 상태 취소
            item.setStatus(ProductionStatus.CANCELLED);            

            // 4-2️⃣ 연결된 주문상세 상태 복구
            Long orderItemId = item.getOrderItemId();
            if (orderItemId != null) {
                orderItemRepository
                    .updateItemStatusToConfirmedByOrderItemId(orderItemId);
            
            }
        }
    }

}
