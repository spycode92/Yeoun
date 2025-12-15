package com.yeoun.masterData.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.modelmapper.ModelMapper;

import com.yeoun.masterData.entity.ProductMst;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProductMstDTO {
		
	@NotBlank(message = "제품ID는 필수 입력값입니다.")
	private String prdId; //제품id
	
	@NotBlank(message = "제품명은 필수 입력값입니다.")
	private String prdName; //제품명
	
	@NotBlank(message = "제품유형은 필수 입력값입니다.")
	private String prdCat; //제품유형
	
	@NotBlank(message = "단위는 필수 입력값입니다.")
	private String prdUnit; //단위
	
	@NotBlank(message = "제품유형은 필수 입력값입니다.")
	private String prdStatus; //상태활성/비활성/단종/시즌한정/품절 ACTIVE,INACTIVE,DISCONTINUED,SEASONAL,OUT_OF_STOCK
	
	@NotBlank(message = "유효일자는 필수 입력값입니다.")
	private Long effectiveDate; //유효일자
	
	@NotNull(message = "가격은 필수 입력값입니다.")
	private BigDecimal unitPrice;
	
	private String prdSpec; //제품상세설명
	
	private String createId; //생성자 id
	
	private LocalDate createDate; //생성일시
	
	private String updateId; //수정자 id
	
	private LocalDate updateDate; //수정일시
	
	private String itemName; // 추가상품 타입
	
    private static final ModelMapper modelMapper = new ModelMapper();

    public static ProductMstDTO fromEntity(ProductMst entity) {
        if (entity == null) return null;
        return modelMapper.map(entity, ProductMstDTO.class);
    }

    public ProductMst toEntity() {
        return modelMapper.map(this, ProductMst.class);
    }
	
}