package com.yeoun.process.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yeoun.process.dto.WorkOrderProcessDTO;
import com.yeoun.process.dto.WorkOrderProcessDetailDTO;
import com.yeoun.process.dto.WorkOrderProcessStepDTO;
import com.yeoun.process.service.WorkOrderProcessService;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Controller
@RequestMapping("/process")
@RequiredArgsConstructor
public class WorkOrderProcessController {
	
	private final WorkOrderProcessService workOrderProcessService;
	
	// 공정 현황 페이지
	@GetMapping("/status")
	public String processStatus() {
		return "/process/process_status";
	}
	
	// 공정 현황 목록 데이터
	@GetMapping("/status/data")
	@ResponseBody
	public List<WorkOrderProcessDTO> getWorkOrdersForGrid(@RequestParam(name = "workDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
														  @RequestParam(name = "searchProcess", required = false) String processId,
														  @RequestParam(name = "searchHStatus", required = false) String status,
														  @RequestParam(name = "searchKeyword", required = false) String keyword) {
		return workOrderProcessService.getWorkOrderListForStatus(workDate, processId, status, keyword);
	}
	
	// 공정 현황 상세 모달용 데이터
	@GetMapping("/status/detail/{orderId}")
	@ResponseBody
	public WorkOrderProcessDetailDTO getWorkOrderDetail(@PathVariable("orderId") String orderId) {
		return workOrderProcessService.getWorkOrderProcessDetail(orderId);
	}
	
	// -----------------------------
    // 공정 단계 시작/종료/메모
    // -----------------------------
    @PostMapping("/status/step/start")
    @ResponseBody
    public Map<String, Object> startStep(@RequestBody StepRequest req) {
        try {
        	workOrderProcessService.startStep(req.getOrderId(), req.getStepSeq());
        	
        	// 공정 시작 처리 후, 최신 상세 다시 조회
        	WorkOrderProcessDetailDTO detail =
                    workOrderProcessService.getWorkOrderProcessDetail(req.getOrderId());
        	
            return Map.of(
                    "success", true,
                    "message", "공정을 시작 처리했습니다.",
                    "detail", detail
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", e.getMessage()
            );
        }
    }

    @PostMapping("/status/step/finish")
    @ResponseBody
    public Map<String, Object> finishStep(@RequestBody StepRequest req) {
        try {
        	workOrderProcessService.finishStep(req.getOrderId(), req.getStepSeq());
        	
        	// 공정 종료 처리 후, 최신 상세 다시 조회
        	WorkOrderProcessDetailDTO detail =
                    workOrderProcessService.getWorkOrderProcessDetail(req.getOrderId());
        	
            return Map.of(
                    "success", true,
                    "message", "공정을 종료 처리했습니다.",
                    "detail", detail
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", e.getMessage()
            );
        }
    }

    @PostMapping("/status/step/memo")
    @ResponseBody
    public Map<String, Object> updateMemo(@RequestBody MemoRequest req) {
        try {
        	workOrderProcessService.updateStepMemo(
                    req.getOrderId(), req.getStepSeq(), req.getMemo());
        	
        	// 메모 저장 후, 최신 상세 다시 조회
            WorkOrderProcessDetailDTO detail =
                    workOrderProcessService.getWorkOrderProcessDetail(req.getOrderId());
        	
            return Map.of(
                    "success", true,
                    "message", "메모를 저장했습니다.",
                    "detail", detail
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", e.getMessage()
            );
        }
    }

    // 요청 JSON 바디용 DTO
    @Getter @Setter
    public static class StepRequest {
        private String orderId;
        private Integer stepSeq;
    }

    @Getter @Setter
    public static class MemoRequest {
        private String orderId;
        private Integer stepSeq;
        private String memo;
    }
    
}
