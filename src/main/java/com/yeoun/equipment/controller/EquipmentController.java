package com.yeoun.equipment.controller;

import com.yeoun.equipment.dto.*;
import com.yeoun.equipment.entity.Equipment;
import com.yeoun.equipment.entity.ProdLine;
import com.yeoun.equipment.repository.EquipmentRepository;
import com.yeoun.equipment.repository.ProdLineRepository;
import com.yeoun.equipment.service.EquipmentService;

import com.yeoun.process.dto.WorkOrderProcessDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/equipment")
@RequiredArgsConstructor
@Log4j2
public class EquipmentController {

    private final EquipmentService equipmentService;
    private final EquipmentRepository equipmentRepository;
    private final ProdLineRepository prodLineRepository;

    // ===================================================
    // 설비, 라인 기준정보 페이지
    @GetMapping("/master")
    public String master(EquipmentTypeCreateRequest equipReq,
                         LineCreateRequest lineReq,
                         Model model) {
    	model.addAttribute("equipmentTypeCreateRequest", equipReq);
    	model.addAttribute("lineCreateRequest", lineReq);
        return "masterData/equipment_line";
    }

    // ===================================================
    // 설비 기준정보 불러오기
    @GetMapping("/equip/data")
    @ResponseBody
    public List<EquipmentDTO> equipData() {
        List<EquipmentDTO> list = equipmentService.loadAllEquipmentTypes();
        log.info("list :::::::: " + list);
        return list;
    }
    
    // ===================================================
    // 설비 마스터 등록하기
    @PostMapping("/equipType")
    public ResponseEntity<?> createEquipType(@Valid @RequestBody EquipmentTypeCreateRequest req,
    		BindingResult bindingResult) {
    	
    	if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            bindingResult.getFieldError().getDefaultMessage()
                    )
            );
    	}
    	
    	equipmentService.createEquipmentType(req);
    	return ResponseEntity.ok().build();
    }
    
    // ===================================================
    // 설비 마스터 수정하기
    @PatchMapping("/equipType/{id}")
    public ResponseEntity<?> updateEquipType(@PathVariable String id,
                @Valid @RequestBody EquipmentTypeCreateRequest req) {
        equipmentService.modifyEquipmentType(req);
        return ResponseEntity.ok().build();
    }
    
    // ===================================================
    // 설비 마스터 삭제(비활성화)하기
    @PatchMapping("/equipType/{id}/inActive")
    public ResponseEntity<?> deleteEquipType(@PathVariable String id,
                                             @RequestBody EquipmentTypeCreateRequest req) {
        equipmentService.modifyYnEquipmentType(id, req.getUseYn());
        return ResponseEntity.ok().build();
    }

    // ===================================================
    // 라인 기준정보 불러오기
    @GetMapping("/line/data")
    @ResponseBody
    public List<ProdLineDTO> lineData() {
        List<ProdLineDTO> list = equipmentService.loadAllLines();
        log.info("list :::::::: " + list);
        return list;
    }
    
    
    // ===================================================
    // 라인 등록하기
    @PostMapping("/line")
    public ResponseEntity<?> createLine(
            @Valid @RequestBody LineCreateRequest req,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            bindingResult.getFieldError().getDefaultMessage()
                    )
            );
        }

        equipmentService.createLine(req);

        return ResponseEntity.ok().build();
    }
    
    // ===================================================
    // 라인 수정하기
    @PatchMapping("/line/{id}")
    public ResponseEntity<?> updateLine(@PathVariable String id,
               @Valid @RequestBody LineCreateRequest req) {
        equipmentService.modifyLine(req);
        return ResponseEntity.ok().build();
    }
    
    // ===================================================
    // 라인 삭제(비활성화)하기
    @PatchMapping("/line/{id}/inActive")
    public ResponseEntity<?> deleteLine(@PathVariable String id,
               @RequestBody LineCreateRequest req) {
        equipmentService.modifyYnLine(id, req.getUseYn());
        return ResponseEntity.ok().build();
    }
    
    // ===================================================
    // 설비 목록
    @GetMapping("/list")
    public String list(Model model, EquipmentCreateRequest req) {

        // 모든 설비
        List<EquipmentDTO> equipments = equipmentRepository.findAll().stream()
                .map(EquipmentDTO::fromEntity)
                .toList();

        // 사용중 설비
        List<EquipmentDTO> useEquipments = equipmentRepository.findByUseYn("Y").stream()
                .map(EquipmentDTO::fromEntity)
                .toList();

        // 모든 라인
        List<ProdLineDTO> lines = prodLineRepository.findAll().stream()
                .map(ProdLineDTO::fromEntity)
                .toList();

        // 사용중 라인
        List<ProdLineDTO> useLines = prodLineRepository.findByUseYn("Y").stream()
                .map(ProdLineDTO::fromEntity)
                .toList();


        model.addAttribute("equipTypes", equipments);
        model.addAttribute("useEquipTypes", useEquipments);
        model.addAttribute("lines", lines);
        model.addAttribute("useLines", useLines);
        model.addAttribute("req", req);
    	return "equipment/list";
    }

    // ===================================================
    // 설비 목록
    @GetMapping("/list/data")
    @ResponseBody
    public List<ProdEquipDTO> equipmentListData(EquipmentSearchDTO dto) {
        List<ProdEquipDTO> list = equipmentService.loadAllEquipments(dto);
        return list;
    }
    
    // ===================================================
    // 보유 설비 등록하기
    @PostMapping("/equip")
    public ResponseEntity<?> createEquipment(@Valid @RequestBody EquipmentCreateRequest req,
                                BindingResult bindingResult) {
        equipmentService.createEquipment(req);
        return ResponseEntity.ok().build();
    }
    
    // ===================================================
    // 설비별 작업 이력 조회
    @GetMapping("/equip/{id}/history")
    @ResponseBody
    public List<WorkOrderProcessDTO> loadHistory(@PathVariable Long id) {
        List<WorkOrderProcessDTO> list = equipmentService.loadAllEquipmentHistory(id);
        log.info("list ::::::::: " + list);
        return list;
    }
    
    // ===================================================
    // 설비 비가동 이력
    @GetMapping("/history")
    public String history(Model model) {
        // 모든 설비
        List<EquipmentDTO> equipments = equipmentRepository.findAll().stream()
                .map(EquipmentDTO::fromEntity)
                .toList();

        // 모든 라인
        List<ProdLineDTO> lines = prodLineRepository.findAll().stream()
                .map(ProdLineDTO::fromEntity)
                .toList();

        model.addAttribute("equipTypes", equipments);
        model.addAttribute("lines", lines);
    	return "/equipment/history";
    }
    
    // ===================================================
    // 설비 비가동 이력
    @GetMapping("/history/data")
    @ResponseBody
    public List<EquipDowntimeDTO> historyData(HistorySearchDTO dto) {
    	return equipmentService.loadAllEquipDowntimeHistory(dto);
    }
    


}
