package com.yeoun.order.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import com.yeoun.process.entity.WorkOrderProcess;
import com.yeoun.process.repository.WorkOrderProcessRepository;
import com.yeoun.process.service.ProcessTimeCalculator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeoun.emp.entity.Emp;
import com.yeoun.equipment.entity.ProdLine;
import com.yeoun.masterData.entity.ProductMst;
import com.yeoun.masterData.entity.RouteHeader;
import com.yeoun.equipment.repository.ProdLineRepository;
import com.yeoun.leave.repository.LeaveHistoryRepository;
import com.yeoun.masterData.repository.ProductMstRepository;
import com.yeoun.order.mapper.OrderMapper;
import com.yeoun.outbound.service.OutboundService;
import com.yeoun.production.dto.ProductionPlanListDTO;
import com.yeoun.production.repository.ProductionPlanItemRepository;
import com.yeoun.production.repository.ProductionPlanRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Log4j2
public class OrderQueryService {
	
	private final OrderMapper orderMapper;
	private final ProdLineRepository prodLineRepository;
	private final ProductMstRepository productMstRepository;
	private final ProductionPlanRepository productionPlanRepository;
	private final WorkOrderRepository workOrderRepository;

	private final EmpRepository empRepository;
	private final WorkerProcessRepository workerProcessRepository;
	private final WorkOrderProcessRepository workOrderProcessRepository;
	private final LeaveHistoryRepository leaveHistoryRepository;
	
//	@Autowired
//	private ObjectMapper objectMapper;


	// =======================================================
	// 작업지시 목록 조회
	public List<WorkOrderListDTO> loadAllOrders (WorkOrderSearchDTO dto){
		//log.info("dto....... loadAll...." + dto);
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
			if (plan.getStatus().equals("CANCELLED")) continue;

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
		return prodLineRepository.findByUseYnOrderByLineIdAsc("Y");
	}
	
	// =======================================================
	// 작업자 조회
	public List<WorkerListDTO> loadAllWorkers() {
		
		LocalDate today = LocalDate.now();			// 휴가여부 판단용
		LocalDateTime now = LocalDateTime.now();	// 작업여부 판단용
		
		List<WorkerListDTO> list = orderMapper.selectWorkers();
		
		for (WorkerListDTO dto : list) {
			boolean isHoliday = leaveHistoryRepository.existsByEmp_EmpIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual
								(dto.getEmpId(), today, today);
			if (isHoliday) {
				dto.setStatus("AWAY");
				continue;
			}
			
			Integer isWorking = orderMapper.existWorkingSchedule(dto.getEmpId(), now);
			
			if (isWorking > 0) {
				dto.setStatus("WORK");
			} else {
				dto.setStatus("AVAIL");
			}
			
		}
		return list;
	}
	
	// =======================================================
	// 품질검사팀 조회

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
//		//log.info("workers size = {}", workers.size());
//		try {
//			log.info("workers JSON = {}", objectMapper.writeValueAsString(workers));
//		} catch (JsonProcessingException e) {
//			e.printStackTrace();
//		}
		
		List<WorkOrderProcess> processList = workOrderProcessRepository.findByWorkOrderOrderIdOrderByStepSeqAsc(id);
//		log.info("processList size = {}", processList.size());
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
//		infos.forEach(i ->
//		        log.info("WorkInfo => id={}, name={}, status={}, worker={}",
//		                i.getProcessId(), i.getProcessName(), i.getStatus(), i.getWorkerName())
//		);
		
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
				.outboundYn(order.getOutboundYn())
				.remark(order.getRemark())
				.build();

	}

	// =======================================================
	// 모든 스케줄 불러오기
	public List<WorkScheduleDTO> loadAllSchedules() {
		List<WorkScheduleDTO> list = orderMapper.selectAllSchedules();
		//log.info("here 1................. list..." + list.toString());
		for (WorkScheduleDTO schedule : list) {
			//log.info("here 1................. schedule..." + schedule.toString());
			List<WorkerProcessDTO> dtos = orderMapper.selectWorkersBySchedule(schedule.getScheduleId());
			List<String> workers = new ArrayList<>();
			for (WorkerProcessDTO dto : dtos) {
				//log.info("here 1................. dto..." + dto.getEmpId());
				workers.add(dto.getEmpId());
			}
			schedule.setWorkers(workers);
		}
		return list;
	}
  
	// 작업지시서 전체 조회
	public List<WorkOrderDTO> findAllWorkList() {
  		List<WorkOrder> workOrders = workOrderRepository.findByOutboundYn("N");
		// 상태가 "N"인게 없어서 "Y"로 작업 후 변경할 예정
//		List<WorkOrder> workOrders = workOrderRepository.findByOutboundYn("Y");
		
		return workOrders.stream()
				.map(WorkOrderDTO::fromEntity)
				.collect(Collectors.toList());
	}


}









