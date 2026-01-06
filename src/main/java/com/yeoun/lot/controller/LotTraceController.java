package com.yeoun.lot.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yeoun.lot.dto.LotMaterialDetailDTO;
import com.yeoun.lot.dto.LotMaterialNodeDTO;
import com.yeoun.lot.dto.LotProcessDetailDTO;
import com.yeoun.lot.dto.LotProcessNodeDTO;
import com.yeoun.lot.dto.LotRootDTO;
import com.yeoun.lot.dto.LotRootDetailDTO;
import com.yeoun.lot.dto.LotUsedProductNodeDTO;
import com.yeoun.lot.service.LotTraceService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequestMapping("/lot")
@RequiredArgsConstructor
public class LotTraceController {

	private final LotTraceService lotTraceService;
	
	// LOT 추적 페이지
//	@GetMapping("/trace")
//	public String view(@RequestParam(name = "lotNo", required = false) String lotNo, 
//					   @RequestParam(name = "stepSeq", required = false) Integer stepSeq,
//					   @RequestParam(name = "keyword", required = false) String keyword,
//					   @RequestParam(name="status", required=false) String status,
//	                   @RequestParam(name="type", required=false) String type,
//					   Model model) {
//		
//		// 1) 왼쪽에 표시할 WIP + FIN LOT 목록
//		List<LotRootDTO> lotList = lotTraceService.getFinishedLots(keyword, status, type);
//		model.addAttribute("lotList", lotList);
//		
//		// 2) 검색어 유지
//	    model.addAttribute("keyword", keyword);
//	    model.addAttribute("status", status);
//	    model.addAttribute("type", type);
//		
//		return "/lot/trace";
//	}
	
	@GetMapping("/trace")
	public String view(@RequestParam(name = "mode", required = false, defaultValue = "PRODUCT") String mode,
					   @RequestParam(name = "keyword", required = false) String keyword,
					   @RequestParam(name="status", required=false) String status,
					   @RequestParam(name="type", required=false) String type,
					   @RequestParam(name = "page", defaultValue = "1") int page,
				       @RequestParam(name = "size", defaultValue = "20") int size,
					   Model model) {
		
		// mode 정규화
        String m = (mode == null) ? "PRODUCT" : mode.trim().toUpperCase();
		
        List<LotRootDTO> allList;
        if ("MATERIAL".equals(m)) {
            // 자재 ROOT (RAW/SUB/PKG)
        	allList = lotTraceService.getMaterialRootLots(keyword, status, type);
        } else {
            // 기존 완제품(WIP/FIN)
        	allList = lotTraceService.getFinishedLots(keyword, status, type);
            m = "PRODUCT";
        }
        
        int totalCount = allList.size();
        int fromIndex = Math.min((page - 1) * size, totalCount);
        int toIndex   = Math.min(fromIndex + size, totalCount);
        
        List<LotRootDTO> lotList = allList.subList(fromIndex, toIndex);
        int totalPages = (int) Math.ceil((double) totalCount / size);

        model.addAttribute("mode", m);
        model.addAttribute("lotList", lotList);
        
        model.addAttribute("page", page);                 
        model.addAttribute("size", size);                
        model.addAttribute("totalPages", totalPages);     
        model.addAttribute("totalCount", totalCount);     

        // 검색어 유지
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("type", type);

        return "/lot/trace";
	}
	
	
	// LOT 상세 + 공정 + 자재 LOT를 한 번에 가져와서
	// 오른쪽 카드(body) fragment 로만 렌더링
	@GetMapping("/trace/detail")
	public String getLotDetailFragment(@RequestParam("lotNo") String lotNo,
	                                   Model model) {

	    // LOT 기본 정보
		LotRootDetailDTO rootDetail = lotTraceService.getRootLotDetail(lotNo);
	    model.addAttribute("rootDetail", rootDetail);

	    // 오른쪽 상세 카드용 fragment만 반환
	    return "/lot/trace_detail :: detail";
	}
	
	// =====================================================================
	// 공정 리스트 
	@GetMapping("/trace/process-list")
	@ResponseBody
	public List<LotProcessNodeDTO> getProcessList(@RequestParam("lotNo") String lotNo) {
		
		// LOT 기준 공정 단계 목록 조회 (기존 서비스 그대로 재사용)
	    return lotTraceService.getProcessNodesForLot(lotNo);
	}
	
	// 공정 상세 조회 (LOT 트리에서 공정 클릭 시 호출)
	@GetMapping("/trace/process-detail")
	@ResponseBody
	public LotProcessDetailDTO getProcessDetail(@RequestParam("orderId") String orderId,
								   				@RequestParam("stepSeq") Integer stepSeq) {
		return lotTraceService.getProcessDetail(orderId, stepSeq);
	}
	
	
	// =====================================================================
	// 자재 리스트 
	@GetMapping("/trace/material-list")
	@ResponseBody
	public List<LotMaterialNodeDTO> getMaterialList(@RequestParam("lotNo") String lotNo) {
		
		// LOT 기준 공정 단계 목록 조회 (기존 서비스 그대로 재사용)
	    return lotTraceService.getMaterialNodesForLot(lotNo);
	}
	
	// 자재 상세 조회 (LOT 트리에서 자재 클릭 시 호출)
	@GetMapping("/trace/material-detail")
	@ResponseBody
	public LotMaterialDetailDTO getMaterialDetail(@RequestParam("outputLotNo") String outputLotNo,
												  @RequestParam("inputLotNo") String inputLotNo) {
		return lotTraceService.getMaterialDetail(outputLotNo, inputLotNo);
	}
	
	// ===========================
    // MATERIAL 모드 - 신규 추가
    // ===========================

    // 자재 ROOT 상세(fragment) - 오른쪽 패널
	@GetMapping("/trace/material-root-detail")
	public String getMaterialRootDetailFragment(@RequestParam("lotNo") String lotNo, Model model) {
	    LotMaterialDetailDTO d = lotTraceService.getMaterialRootDetail(lotNo);
	    model.addAttribute("materialDetail", d);
	    return "/lot/trace_detail :: detail";
	}

    // 자재 LOT → 사용된 완제품 LOT 목록(Forward)
	@GetMapping("/trace/material/used-products")
	@ResponseBody
	public List<LotUsedProductNodeDTO> getUsedProducts(@RequestParam("inputLotNo") String inputLotNo) {
	    return lotTraceService.getUsedProductsByMaterialLot(inputLotNo);
	}
	
	
	
	
}
