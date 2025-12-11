package com.yeoun.common.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "DISPOSE")
@SequenceGenerator(
		name = "DISPOSE_SEQ_GENERATOR", // JPA 에서 사용하는 시퀀스 이름(DB의 시퀀스 이름이 아님!)
		sequenceName = "DISPOSE_SEQ", // 오라클에서 사용하는 시퀀스 이름
		initialValue = 1, 			// 초기값(오라클 시퀀스의 start with 1과 동일)
		allocationSize = 1          // 증가값(오라클 시퀀스의 increment by 값과 동일)
		)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Dispose {
	@Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "DISPOSE_SEQ_GENERATOR")
	@Column(name = "DISPOSE_ID", updatable = false)
	private Long disposeId; // 폐기ID
	
	@Column(nullable = false)
	private String lotNo; //로트번호
	
	@Column(nullable = false)
	private String itemId; // 원자재/부자재, 완제품의 기준정보 고유값
	
	@Column(nullable = false)
	private String workType; // 작업종류 ( 입고, 출고, 재고, 생산 )
	
	@Column(nullable = false)
	private String empId; // 작업자
	
	@Column(nullable = false)
	private Long disposeAmount; // 폐기수량
	
	@Column(nullable = true)
	private String disposeReason; // 폐기이유
	
	@CreatedDate
	private LocalDateTime createdDate; // 등록 일시
	
}
