package com.yeoun.equipment.service;

import com.yeoun.equipment.dto.*;
import com.yeoun.equipment.entity.ProdEquip;
import com.yeoun.equipment.entity.ProdLine;
import com.yeoun.equipment.mapper.EquipmentMapper;
import com.yeoun.equipment.repository.ProdEquipRepository;
import com.yeoun.equipment.entity.Equipment;
import com.yeoun.equipment.repository.EquipmentRepository;
import com.yeoun.equipment.repository.ProdLineRepository;

import com.yeoun.process.dto.WorkOrderProcessDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final ProdLineRepository prodLineRepository;
    private final ProdEquipRepository prodEquipRepository;
	private final EquipmentMapper equipmentMapper;

	// ===================================================
    // 설비 마스터 목록
    public List<EquipmentDTO> loadAllEquipmentTypes() {
        return equipmentRepository.findAll().stream()
                .map(EquipmentDTO::fromEntity)
                .toList();
    }

    // ===================================================
    // 라인 목록
    public List<ProdLineDTO> loadAllLines() {
        return prodLineRepository.findAll(Sort.by(Sort.Direction.ASC, "lineId")).stream()
                .map(ProdLineDTO::fromEntity)
                .toList();
    }
    
    // ===================================================
    // 보유 설비 목록
	public List<ProdEquipDTO> loadAllEquipments(EquipmentSearchDTO dto) {
		List<ProdEquip> equips = prodEquipRepository.loadProdEquip(
				dto.getEquipment(),
				dto.getLine(),
				dto.getStatus());
		List<ProdEquipDTO> list = new ArrayList<>();
		for (ProdEquip equip : equips) {
			ProdEquipDTO equipDTO = ProdEquipDTO.builder()
							.equipId(equip.getEquipId())
							.equipTypeId(equip.getEquipment().getEquipId())
							.equipName(equip.getEquipName())
							.lineId(equip.getLine().getLineId())
							.status(equip.getStatus())
							.createdDate(equip.getCreatedDate())
							.updatedDate(equip.getUpdatedDate())
							.remark(equip.getRemark())
							.build();
			
			list.add(equipDTO);
		}
		return list;
	}

    // ===================================================
    // 설비 마스터 등록
	public void createEquipmentType(EquipmentTypeCreateRequest req) {
		
		EquipmentDTO dto = EquipmentDTO.builder()
				.equipId(req.getEquipId())
				.koName(req.getKoName())
				.equipName(req.getEquipName())
				.useYn("Y")
				.remark(req.getRemark())
				.build();
		
		Equipment equipment = dto.toEntity();
		equipmentRepository.save(equipment);
		
	}

	// ===================================================
	// 설비 마스터 수정
	@Transactional
	public void modifyEquipmentType(EquipmentTypeCreateRequest req){
		Equipment equipment = equipmentRepository.findById(req.getEquipId())
				.orElseThrow(() -> new RuntimeException("해당하는 설비가 없습니다."));

		equipment.setKoName(req.getKoName());
		equipment.setEquipName(req.getEquipName());
		equipment.setRemark(req.getRemark());
		equipment.setUpdatedDate(LocalDateTime.now());
	}

	// ===================================================
	// 설비 마스터 활성화&비활성화
	@Transactional
	public void modifyYnEquipmentType(String id, String useYn) {
		Equipment equipment = equipmentRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("해당하는 설비가 없습니다."));
		equipment.setUseYn(useYn);
	}
	
	// ===================================================
	// 라인 마스터 등록
	public void createLine(LineCreateRequest req) {
		ProdLineDTO dto = ProdLineDTO.builder()
				.lineId(generateNextLineId())
				.lineName(req.getLineName())
				.status("STOP")
				.remark(req.getRemark())
				.useYn("Y")
				.build();

		ProdLine prodLine = dto.toEntity();
		prodLineRepository.save(prodLine);
	}

	// ===================================================
	// 라인 ID 생성
	public String generateNextLineId() {
		Integer max = prodLineRepository.findMaxLineNumber();
		int next = (max == null) ? 1 : max + 1;
		return String.format("PL-%02d", next);
	}


	// ===================================================
	// 라인 마스터 수정
	@Transactional
	public void modifyLine(LineCreateRequest req) {
		ProdLine line = prodLineRepository.findById(req.getLineId())
				.orElseThrow(() -> new RuntimeException("해당하는 라인이 없습니다."));

		line.setLineName(req.getLineName());
		line.setRemark(req.getRemark());
	}

	// ===================================================
	// 라인 마스터 활성화&비활성화
	@Transactional
	public void modifyYnLine(String id, String useYn) {
		ProdLine line = prodLineRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("해당하는 라인이 없습니다."));
		line.setUseYn(useYn);
	}

	// ===================================================
	// 설비 등록
	public void createEquipment(EquipmentCreateRequest req) {

		ProdEquip equipment = new ProdEquip();

		equipment.setEquipment(equipmentRepository.findById(req.getEquipType())
				.orElseThrow(() -> new RuntimeException("해당하는 설비 타입이 없습니다.")));
		equipment.setLine(prodLineRepository.findById(req.getLineId())
				.orElseThrow(() -> new RuntimeException("해당하는 라인이 없습니다.")));
		equipment.setStatus("RUN");
		equipment.setEquipName(req.getEquipName());

		prodEquipRepository.save(equipment);
	}


	public List<WorkOrderProcessDTO> loadAllEquipmentHistory(Long id, HistorySearchDTO dto) {

		if (dto == null) {
			dto = new HistorySearchDTO();
		}

		if (id == null) {
			dto.setType("ALL");
			return equipmentMapper.selectProcessByLineAndStep(dto);
		}

		dto.setType("DIV");

		ProdEquip equipment = prodEquipRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("해당 설비가 없습니다."));
		dto.setLine(equipment.getLine().getLineId());
		int step = switch (equipment.getEquipment().getEquipId())
		{
			case "BLENDING_TANK", "MIXER_AGITATOR" -> 1;
			case "FILTER_HOUSING" 				   -> 2;
			case "FILLING_SEMI"					   -> 3;
			case "CAPPING_SEMI"					   -> 4;
			case "LABELING_AUTO", "PACKING_SEMI"   -> 6;
			default -> throw new IllegalArgumentException("설비 타입 오류");
		};
		dto.setStep(step);

		return equipmentMapper.selectProcessByLineAndStep(dto);
	}

	public List<EquipDowntimeDTO> loadAllEquipDowntimeHistory(HistorySearchDTO dto) {
		return equipmentMapper.selectDowntimeHistories(dto);
	}
	
	// 가동중 설비 목록
	public Integer selectRunningEquipmentCount () {
		return equipmentMapper.countRunningEquipments();
	}
	
}








