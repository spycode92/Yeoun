package com.yeoun.masterData.dto;

import java.time.LocalDateTime;

import com.yeoun.masterData.entity.ProductMst;

import jakarta.validation.constraints.NotBlank;
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
public class RouteHeaderDTO<updatedDate> {
	
	// 라우트ID
	@NotBlank(message = "라우트 ID는 필수 입력값입니다.")
	private String routeId;
	
	// 제품코드
	@NotBlank(message = "제품 코드는 필수 입력값입니다.")
	private ProductMst product;
	
	// 라우트명
	@NotBlank(message = "라우트 명은 필수 입력값입니다.")
	private String routeName;
	
	// 라우트 설명
	@NotBlank(message = "라우트 설명은 필수 입력값입니다.")
	private String description;
	
	// 사용 여부
	private String useYn;
	
	// 최초 등록자
	private String createdId;
	
	// 최초 등록일
	private LocalDateTime createdDate;
	
	// 수정자
	private String updatedId;
	
	// 수정일
	private LocalDateTime updatedDate;

}
