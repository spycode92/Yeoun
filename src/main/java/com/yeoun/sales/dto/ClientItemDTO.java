package com.yeoun.sales.dto;

import java.math.BigDecimal;

import groovy.transform.builder.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientItemDTO {

    private Long itemId;
    private String materialId;

    private String materialName;  //원재료명
    
    private String matUnit;  // 🔥 제품/BOM 단위
    private String unit;     // 🔥 공급단위(협력사 지정)
    private BigDecimal orderUnit; // 발주단위

    private BigDecimal unitPrice; //단가
    private BigDecimal moq; //최소발주수량
    private String supplyAvailable; //공급가능여부
    private BigDecimal leadDays; //리드타임
   
    private String matType;       // ⭐ 자재유형(원재료/부자재/포장재)

    // 🔥 JPQL에서 사용하는 생성자 추가
    public ClientItemDTO(
            Long itemId,
            String materialId,
            String materialName,
            String unit,
            String matUnit,            
            BigDecimal orderUnit, 
            BigDecimal unitPrice,
            BigDecimal moq,
            String supplyAvailable,
            String matType
    ) {
        this.itemId = itemId;
        this.materialId = materialId;
        this.materialName = materialName;
        this.matUnit = matUnit;
        this.unit = unit;
        this.orderUnit = orderUnit;
        this.unitPrice = unitPrice;
        this.moq = moq;
        this.supplyAvailable = supplyAvailable;
        this.matType = matType;
    }
}
