package com.yeoun.order.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.yeoun.equipment.repository.ProdLineRepository;
import com.yeoun.leave.repository.LeaveHistoryRepository;
import com.yeoun.order.dto.WorkerListDTO;
import org.springframework.stereotype.Service;

import com.yeoun.order.dto.WorkOrderValidateRequest;
import com.yeoun.order.dto.WorkOrderValidationResult;
import com.yeoun.order.mapper.OrderMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class OrderValidationService {
	
	private final OrderMapper orderMapper;
	private final ProdLineRepository prodLineRepository;
	private final LeaveHistoryRepository leaveHistoryRepository;

	// 작업지시 등록 전 유효성 검증
	public WorkOrderValidationResult validateAll (WorkOrderValidateRequest req) {

		// 1) 작업자 중복 (입력 무결성)
		if (!validateWorkerDuplicate(req)){
			return WorkOrderValidationResult.builder()
					.valid(false)
					.message("동일한 작업자를 여러 공정에 배정할 수 없습니다.")
					.build();
		}

		// 2) 작업시간 규칙
		if (!validateWorkTime(req)) {
			return WorkOrderValidationResult.builder()
					.valid(false)
					.message("작업 시간은 09:00 ~ 18:00 사이여야 합니다.")
					.build();
		}

		// 3) 자원 충돌 검증
		boolean lineAval	= validateLineAvailability(req);
		boolean workerAval  = validateWorkerAvailability(req);

		WorkOrderValidationResult result = WorkOrderValidationResult.builder()
				.valid(lineAval && workerAval)
				.lineAvailable(lineAval)
				.workerAvailable(workerAval)
				.build();

		// 걸린 경우 추천
		if (!lineAval) {
			result.setMessage("선택한 시간대에 해당 라인에서 진행하는 작업이 있습니다.");
			result.setSuggestedLines(suggestAvailableLines(req));
		} else if (!workerAval) {
			result.setMessage("선택한 시간대에 해당 작업자가 진행하는 작업이 있습니다.");
			result.setSuggestedWorkers(suggestAvailableWorkers(req));
		}

		return result;
	}



	// =============================== VALIDATION ===================================

	// 작업지시 등록 전 유효성 검증(시간)
	public boolean validateWorkTime (WorkOrderValidateRequest req) {
		LocalTime start = req.getStartTime().toLocalTime();
		LocalTime end 	= req.getEndTime().toLocalTime();

		LocalTime open	= LocalTime.of(9, 0);
		LocalTime close = LocalTime.of(18, 0);

		return !start.isBefore(open)
			&& !end.isAfter(close)
			&& start.isBefore(end);
	}

	// 작업지시 등록 전 유효성 검증(작업자 중복 배정)
	public boolean validateWorkerDuplicate(WorkOrderValidateRequest req) {
		if (req.getWorkers() == null || req.getWorkers().isEmpty()) {
			return true;
		}

		return req.getWorkers().size()
				== new HashSet<>(req.getWorkers()).size();
	}

	// 작업지시 등록 전 유효성 검증(라인)
	public boolean validateLineAvailability (WorkOrderValidateRequest req) {
		
		if (req.getLineId() == null || req.getLineId().isEmpty()) {
			return true;
		}
		
		int count = orderMapper.countLineOverlap(req);
		
		if (count > 0) {
			return false;
		}
		
		return true;
	}

	// 작업지시 등록 전 유효성 검증(작업자)
	public boolean validateWorkerAvailability (WorkOrderValidateRequest req) {

		if (req.getWorkers() == null || req.getWorkers().isEmpty()) {
			return true;
		}

		for (String workerId : req.getWorkers()) {
			req.setWorkerId(workerId);
			int count = orderMapper.countWorkerOverlap(req);

			if (count > 0) {
				return false;
			}
		}

		return true;
	}

	// =============================== SUGGEST ===================================

	// 작업 가능 라인 제안
	private List<String> suggestAvailableLines(WorkOrderValidateRequest req) {
		return prodLineRepository.findByUseYnOrderByLineIdAsc("Y").stream()
				.map(line -> line.getLineId())
				.filter(lineId -> {
					req.setLineId(lineId);
					return orderMapper.countLineOverlap(req) == 0;
				})
				.toList();
	}

	// 작업 가능 작업자 제안
	private List<String> suggestAvailableWorkers(WorkOrderValidateRequest req) {

		List<WorkerListDTO> workers = orderMapper.selectWorkers();
		List<String> workerIds = new ArrayList<>();

		for (WorkerListDTO worker : workers) {
			boolean isHoliday = leaveHistoryRepository
					.existsByEmp_EmpIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual
							(worker.getEmpId(), req.getStartTime().toLocalDate(), req.getEndTime().toLocalDate());

			Integer hasWork = orderMapper.countWorkerOverlap(req);

			if (!isHoliday && hasWork == 0) {
				workerIds.add(worker.getEmpId());
			}
		}

		return workerIds;
	}
	
	

}
