package com.yeoun.lot.dto;

import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;

import com.yeoun.emp.entity.Emp;
import com.yeoun.lot.entity.LotHistory;
import com.yeoun.lot.entity.LotMaster;
import com.yeoun.masterData.entity.ProcessMst;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class LotHistoryDTO {
	// 이력ID
	private Long histId;
	// LOT 번호
	private String lotNo; 
	// 작업지시번호
	private String orderId;
	// 공정코드
	private String processId;
	// 이벤트 유형
	private String eventType;
	// 상태
	private String status;
	// 위치 유형
	private String locationType;
	// 위치 ID
	private String locationId;
	// 전체수량
	private Integer quantity;
	// 양품수량
	private Integer goodQty;
	// 불량수량
	private Integer defectQty;
	// 시작시간
	private LocalDateTime startTime;
	// 종료시간
	private LocalDateTime endTime;
	// 작업자
	private String workedId;
	
	@Builder
	public LotHistoryDTO(Long histId, String lotNo, String orderId, String processId, String eventType, String status,
			String locationType, String locationId, Integer quantity, Integer goodQty, Integer defectQty,
			LocalDateTime startTime, LocalDateTime endTime, String workedId) {
		this.histId = histId;
		this.lotNo = lotNo;
		this.orderId = orderId;
		this.processId = processId;
		this.eventType = eventType;
		this.status = status;
		this.locationType = locationType;
		this.locationId = locationId;
		this.quantity = quantity;
		this.goodQty = goodQty;
		this.defectQty = defectQty;
		this.startTime = startTime;
		this.endTime = endTime;
		this.workedId = workedId;
	}
	
	// ---------------------------------------------------
	// DTO <-> Entity 변환
	private static ModelMapper modelMapper = new ModelMapper();
	
	// 엔티티 타입으로 변환
	public LotHistory toEntity() {
	    LotHistory history = new LotHistory();

	    // 기본 필드 매핑
	    history.setOrderId(this.orderId);
	    history.setEventType(this.eventType);
	    history.setStatus(this.status);
	    history.setLocationType(this.locationType);
	    history.setLocationId(this.locationId);
	    history.setQuantity(this.quantity);
	    history.setGoodQty(this.goodQty);
	    history.setDefectQty(this.defectQty);
	    history.setStartTime(this.startTime);
	    history.setEndTime(this.endTime);
		
		return history;
	}
	
	// DTO 타입으로 변환
	public static LotHistoryDTO fromEntity(LotHistory history) {
		LotHistoryDTO lotHistoryDTO = modelMapper.map(history, LotHistoryDTO.class);
		
		lotHistoryDTO.setLotNo(history.getLot().getLotNo());
		lotHistoryDTO.setProcessId(history.getProcess().getProcessId());
		lotHistoryDTO.setWorkedId(history.getWorker().getEmpId());
		
		return lotHistoryDTO;
	}
}
