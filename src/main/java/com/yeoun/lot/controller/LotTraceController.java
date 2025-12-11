package com.yeoun.lot.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.yeoun.lot.dto.LotMaterialNodeDTO;
import com.yeoun.lot.dto.LotProcessNodeDTO;
import com.yeoun.lot.dto.LotRootDTO;
import com.yeoun.lot.entity.LotMaster;
import com.yeoun.lot.service.LotTraceService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequestMapping("/lot")
@RequiredArgsConstructor
public class LotTraceController {

	private final LotTraceService lotTraceService;
	
	// LOT 추적 페이지
	@GetMapping("/trace")
	public String view(@RequestParam(name = "lotNo", required = false) String lotNo, 
					   @RequestParam(name = "stepSeq", required = false) Integer stepSeq,
					   Model model) {
		
		// 1. 왼쪽에 표시할 FIN LOT 목록
		List<LotRootDTO> lotList = lotTraceService.getFinishedLots(); 
		model.addAttribute("lotList", lotList);
		
		// 2) 기본 선택 LOT 처리
        //    - lotNo 파라미터가 없고 목록이 비어있지 않으면 첫 LOT 자동 선택
        if (lotNo == null && !lotList.isEmpty()) {
            lotNo = lotList.get(0).getLotNo();
        }

        // 3) 선택된 LOT 상세 조회 후 오른쪽 카드에 바인딩
        if (lotNo != null) {
        	
        	// LOT 상세
            LotMaster selected = lotTraceService.getLotDetail(lotNo);
            model.addAttribute("selectedLot", selected);
            
            // 1차: 공정 단계 리스트
            List<LotProcessNodeDTO> processNodes =
                    lotTraceService.getProcessNodesForLot(lotNo);
            model.addAttribute("processNodes", processNodes);
            
            // 2차: 자재 LOT 
            List<LotMaterialNodeDTO> materialNodes = 
            		lotTraceService.getMaterialNodesForLot(lotNo);
            model.addAttribute("materialNodes", materialNodes);
            
            if (stepSeq != null) {
                processNodes.stream()
                        .filter(p -> p.getStepSeq().equals(stepSeq))
                        .findFirst()
                        .ifPresent(p -> model.addAttribute("selectedProcess", p));
            }
        }
        
		return "/lot/trace";
	}
	

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
