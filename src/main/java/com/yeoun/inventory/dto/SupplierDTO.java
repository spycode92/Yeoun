package com.yeoun.inventory.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SupplierDTO {
	private String clientId;      // 거래처Id
	private String clientName;    // 거래처명
	private String managerName;   // 담당자이름
	private String statusCode;    // 상태코드
	private LocalDate dueDate;    // 납기일
	private List<SupplierItemDTO> supplierItemList; // 공급품목
}
