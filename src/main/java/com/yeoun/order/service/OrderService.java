package com.yeoun.order.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.masterData.entity.RouteStep;
import com.yeoun.masterData.repository.*;
import com.yeoun.order.dto.*;
import com.yeoun.order.entity.WorkOrder;
import com.yeoun.order.entity.WorkSchedule;
import com.yeoun.order.entity.WorkerProcess;
import com.yeoun.order.repository.WorkOrderRepository;
import com.yeoun.order.repository.WorkScheduleRepository;
import com.yeoun.order.repository.WorkerProcessRepository;
import com.yeoun.process.dto.WorkOrderProcessStepDTO;
import com.yeoun.process.entity.WorkOrderProcess;
import com.yeoun.process.repository.WorkOrderProcessRepository;
import com.yeoun.production.entity.ProductionPlan;
import com.yeoun.production.entity.ProductionPlanItem;
import com.yeoun.production.enums.ProductionStatus;
import com.yeoun.order.dto.WorkOrderSearchDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeoun.emp.dto.EmpListDTO;
import com.yeoun.masterData.entity.ProdLine;
import com.yeoun.masterData.entity.ProductMst;

import com.yeoun.masterData.repository.BomMstRepository;
import com.yeoun.masterData.repository.ProdLineRepository;
import com.yeoun.masterData.repository.ProductMstRepository;
import com.yeoun.order.dto.WorkOrderDTO;
import com.yeoun.order.dto.WorkOrderListDTO;
import com.yeoun.order.mapper.OrderMapper;
import com.yeoun.order.repository.WorkOrderRepository;
import com.yeoun.outbound.dto.OutboundOrderDTO;
import com.yeoun.production.dto.ProductionPlanListDTO;
import com.yeoun.production.repository.ProductionPlanItemRepository;
import com.yeoun.production.repository.ProductionPlanRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Log4j2
public class OrderService {
	
	private final OrderMapper orderMapper;
	private final ProdLineRepository prodLineRepository;
	private final ProductMstRepository productMstRepository;
	private final ProductionPlanRepository productionPlanRepository;
	private final ProductionPlanItemRepository productionPlanItemRepository;
	private final WorkOrderRepository workOrderRepository;

	private final EmpRepository empRepository;
	private final WorkScheduleRepository workScheduleRepository;
	private final ProcessMstRepository processMstRepository;
	private final WorkerProcessRepository workerProcessRepository;
	private final RouteStepRepository routeStepRepository;
	private final RouteHeaderRepository routeHeaderRepository;
	private final WorkOrderProcessRepository workOrderProcessRepository;
	
	@Autowired
	private ObjectMapper objectMapper;


	// =======================================================
	// 작업지시 목록 조회
	public List<WorkOrderListDTO> loadAllOrders (WorkOrderSearchDTO dto){
		log.info("dto....... loadAll...." + dto);
		return orderMapper.selectOrderList(dto);
	}
	
	// =======================================================
	// 생산계획 조회
	public List<ProductionPlanViewDTO> loadAllPlans () {

		List<ProductionPlanViewDTO> list = new ArrayList<>();
		List<ProductionPlanListDTO> plans = productionPlanRepository.findPlanList();
		for (ProductionPlanListDTO plan : plans) {
			
			// 1) 생산 완료 계획은 제외하기
			if (plan.getStatus().equals("DONE")) continue;

			int total = plan.getTotalQty().intValue();
			int created = workOrderRepository.sumWorkOrderQty(plan.getPlanId());
			int remain = total-created;

			// 2) qty가 충족된 계획은 제외하기
			if (remain <= 0) continue;

			ProductionPlanViewDTO dto = ProductionPlanViewDTO.builder()
					.planId(plan.getPlanId())
					.createdAt(plan.getCreatedAt())
					.itemName(plan.getItemName())
					.totalQty(total)
					.createdQty(created)
					.remainingQty(remain)
					.status(plan.getStatus())
					.build();

			list.add(dto);
		}
		return list; 
	}
	
	// =======================================================
	// 품목 조회
	public List<ProductMst> loadAllProducts () {
		return productMstRepository.findAll();
	}
	
	// =======================================================
	// 라인 조회
	public List<ProdLine> loadAllLines() {
		return prodLineRepository.findAll();
	}
	
	// =======================================================
	// 작업자 조회
	public List<EmpListDTO> loadAllWorkers() {
		return orderMapper.selectWorkers();
	}

	// =======================================================
	// 작업지시 등록
	@Transactional
	public void createWorkOrder(WorkOrderRequest dto, String id) {

		log.info("create dto!!!!! :::::::: " + dto);

		// 1) 새 작업지시 번호 생성
		String orderId = generateOrderId();

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
				.colorCode(generateColorCode(orderId))
				.build();
		workScheduleRepository.save(schedule);

		// 5) 방금 생성된 작업스케줄 번호 조회
		WorkSchedule newSchedule = workScheduleRepository.findTopByWork_OrderIdOrderByScheduleIdAsc(orderId)
				.orElseThrow(() -> new RuntimeException("일치하는 번호 없음!"));

		// 6) 작업자 저장
		createWorkerProcessList(dto, newSchedule);

		// 7) 새 공정 생성
		createWorkOrderProcessList(order, id);

	}

	// =======================================================
	// 작업지시 번호 생성
	public String generateOrderId(){
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

	// =======================================================
	// 컬러코드 자동 생성
	public String generateColorCode(String workId) {
		int hash = workId.hashCode();

		int h = Math.floorMod(hash, 360);
		int s = 30 + Math.floorMod(hash >>> 3, 25);
		int l = 65 + Math.floorMod(hash >>> 5, 15);

		double sat = s / 100.0;
		double lig = l / 100.0;

		double c = (1 - Math.abs(2 * lig - 1)) * sat;
		double x = c * (1 - Math.abs((h / 60.0) % 2 - 1));
		double m = lig - c / 2.0;

		double r = 0, g = 0, b = 0;

		if (h < 60) { r = c; g = x; }
		else if (h < 120) { r = x; g = c; }
		else if (h < 180) { g = c; b = x; }
		else if (h < 240) { g = x; b = c; }
		else if (h < 300) { r = x; b = c; }
		else { r = c; b = x; }

		int R = (int) Math.round((r + m) * 255);
		int G = (int) Math.round((g + m) * 255);
		int B = (int) Math.round((b + m) * 255);

		return String.format("#%02X%02X%02X", R, G, B);
	}
	// =======================================================
	// 공정별 작업자 할당
	@Transactional
	public void createWorkerProcessList(WorkOrderRequest dto, WorkSchedule schedule) {
		Map<String, String> processWorkerMap = Map.of(
				"PRC-BLD", dto.getPrcBld(),
				"PRC-FLT", dto.getPrcFlt(),
				"PRC-FIL", dto.getPrcFil(),
				"PRC-CAP", dto.getPrcCap(),
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

	// =======================================================
	// 새 공정 생성 로직
	@Transactional
	public void createWorkOrderProcessList (WorkOrder order, String id) {

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

	// =======================================================
	// 작업지시 상세 조회
	public WorkOrderDetailDTO getDetailWorkOrder(String id) {

		// 작업지시 정보 조회
		WorkOrder order = workOrderRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("작업지시 번호를 조회할 수 없음!"));

		// 시간 포맷
		LocalDateTime start = order.getPlanStartDate();
		LocalDateTime end = order.getPlanEndDate();

		String planDate = start.toLocalDate().toString();
		String planTime = start.toLocalTime() + "~" + end.toLocalTime();

		// ================= 작업자 목록 생성 ==================
		List<WorkOrderDetailDTO.WorkInfo> infos = new ArrayList<>();
		List<WorkerProcess> workers = workerProcessRepository.findAllBySchedule_Work_OrderId(id);
		log.info("workers size = {}", workers.size());
		try {
			log.info("workers JSON = {}", objectMapper.writeValueAsString(workers));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		
		List<WorkOrderProcess> processList = workOrderProcessRepository.findByWorkOrderOrderIdOrderByStepSeqAsc(id);
		log.info("processList size = {}", processList.size());
		processList.forEach(p -> log.info("process = {}", p));
		
		// 1) WorkerProcess를 processId 기준으로 빠르게 조회할 Map 만들기
		Map<String, String> workerMap = workers.stream()
		        .collect(Collectors.toMap(
		                wp -> wp.getProcess().getProcessId(),     // key: 공정ID
		                wp -> wp.getWorker().getEmpId(),          // value: 작업자ID
		                (a, b) -> a                               // 중복 발생 시 첫 값 유지
		        ));
		
		// 2) 공정 리스트 순회하며 WorkInfo 생성
		for (WorkOrderProcess proc : processList) {

		    String processId = proc.getProcess().getProcessId();
		    String processName = proc.getProcess().getProcessName();
		    String status = proc.getStatus();  // COMPLETED / IN_PROGRESS / PENDING
		    String workerId = workerMap.get(processId); // 작업자 없으면 자동으로 null
		    String workerName = "";
		    if (workerId != null && !workerId.isBlank()) {
		    	workerName = empRepository.findByEmpId(workerId)
		              .orElseThrow(() -> new RuntimeException("작업자 없음 : " + workerId))
		              .getEmpName();
		    } else {
		    	workerName = null;
		    }

		    infos.add(
		            WorkOrderDetailDTO.WorkInfo.builder()
		                    .processId(processId)
		                    .processName(processName)
		                    .status(status)
		                    .workerId(workerId)
		                    .workerName(workerName)
		                    .build()
		    );
		}

		// 결과 로깅 (테스트용)
		infos.forEach(i ->
		        log.info("WorkInfo => id={}, name={}, status={}, worker={}",
		                i.getProcessId(), i.getProcessName(), i.getStatus(), i.getWorkerName())
		);
		
		// DTO 변환 후 반환
		return WorkOrderDetailDTO.builder()
				.orderId(id)
				.prdId(order.getProduct().getPrdId())
				.prdName(order.getProduct().getPrdName())
				.status(order.getStatus())
				.planQty(order.getPlanQty())
				.planDate(planDate)
				.planTime(planTime)
				.lineName(order.getLine().getLineName())
				.routeId(order.getRouteId())
				.infos(infos)
				.remark(order.getRemark())
				.build();

	}
	
	// =======================================================
	// 작업지시 확정
	@Transactional
	public void modifyOrderStatus(String id, String status) {
		WorkOrder order = workOrderRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("해당하는 작업 번호가 없습니다."));
		order.setStatus(status);
	}
	
	// =======================================================
	// 작업지시 수정
	
	
  
	// 작업지시서 전체 조회
	public List<WorkOrderDTO> findAllWorkList() {
  		List<WorkOrder> workOrders = workOrderRepository.findByOutboundYn("N");
		// 상태가 "N"인게 없어서 "Y"로 작업 후 변경할 예정
//		List<WorkOrder> workOrders = workOrderRepository.findByOutboundYn("Y");
		
		return workOrders.stream()
				.map(WorkOrderDTO::fromEntity)
				.collect(Collectors.toList());
	}

	public void selectAllWorkers() {
		log.info("테스트.... ::: " + orderMapper.selectMaterials("BG030", 100));
		
	}
	


}









