package com.yeoun.lot.dto;

import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;

import com.yeoun.lot.entity.LotMaster;
import com.yeoun.masterData.entity.ProductMst;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LotMasterDTO {
	// LOT 번호
	// 형식: [LOT유형]-[제품코드5자리]-[YYYYMMDD]-[라인]-[시퀀스3자리]
	private String lotNo;
	// LOT 유형
	private String lotType;
	// 작업지시번호
	private String orderId;
	// 제품 ID
	private String prdId;
	// 현재수량
	private Integer quantity;
	// 현재상태
	private String currentStatus;
	// 현재위치 유형
	private String currentLocType;
	// 현재위치 ID
	private String currentLocId;
	// 상태 변경 일시
	private LocalDateTime statusChangeDate;
	// LOT 생성일시
	private LocalDateTime createdDate;
	
	@Builder
	public LotMasterDTO(String lotNo, String lotType, String orderId, String prdId, Integer quantity,
			String currentStatus, String currentLocType, String currentLocId, LocalDateTime statusChangeDate,
			LocalDateTime createdDate) {
		this.lotNo = lotNo;
		this.lotType = lotType;
		this.orderId = orderId;
		this.prdId = prdId;
		this.quantity = quantity;
		this.currentStatus = currentStatus;
		this.currentLocType = currentLocType;
		this.currentLocId = currentLocId;
		this.statusChangeDate = statusChangeDate;
		this.createdDate = createdDate;
	}
	
	// ---------------------------------------------------
	// DTO <-> Entity 변환
	private static ModelMapper modelMapper = new ModelMapper();
	
	// 엔터티 타입으로 변환
	public LotMaster toEntity() {
//		LotMaster lotMaster = modelMapper.map(this, LotMaster.class);
//		
//		if (this.getPrdId() != null) {
//			ProductMst productMst = new ProductMst();
//			productMst.setPrdId(this.getPrdId());
//		}
//		
//		return lotMaster;
		return modelMapper.map(this, LotMaster.class);
	}
	
	// DTO 타입으로 변환
	public static LotMasterDTO fromEntity(LotMaster lotMaster) {
//		LotMasterDTO masterDTO = modelMapper.map(lotMaster, LotMasterDTO.class);
		return modelMapper.map(lotMaster, LotMasterDTO.class);
	}
}
