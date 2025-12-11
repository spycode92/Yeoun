package com.yeoun.lot.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeoun.masterData.entity.MaterialMst;
import com.yeoun.masterData.entity.ProductMst;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * LOT 마스터
 * - LOT 단위 생산/원자재 관리를 위함 (각 LOT의 현재 상태 및 기본 정보 관리)
 */
@Entity
@Table(name = "LOT_MASTER")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LotMaster {
	
	// LOT 번호
	// 형식: [LOT유형]-[제품코드5자리]-[YYYYMMDD]-[라인]-[시퀀스3자리]
	@Id @Column(name = "LOT_NO", length = 50, nullable = false)
	private String lotNo;
	
	// LOT 유형
	@Column(name = "LOT_TYPE", length = 10, nullable = false)
	private String lotType;
	
	// 작업지시번호
	@Column(name = "ORDER_ID", length = 16)
	private String orderId;
	
	// 제품 ID
	@Column(name = "PRD_ID", nullable = false)
	private String prdId;
	
	// 완제품(Product) 조인 - prdId가 제품 코드일 때만 매칭됨
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PRD_ID", insertable = false, updatable = false)
	@NotFound(action = NotFoundAction.IGNORE)
	private ProductMst product;

	// 원자재(Material) 조인 - prdId가 원자재 코드일 때만 매칭됨
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PRD_ID", insertable = false, updatable = false)
	@NotFound(action = NotFoundAction.IGNORE)
	private MaterialMst material;
	
	// 현재수량
	@Column(name = "QUANTITY", nullable = true)
	private Integer quantity;
	
	// 현재상태
	@Column(name = "CURRENT_STATUS", length = 50, nullable = false)
	private String currentStatus;
	
	// 현재위치 유형
	@Column(name = "CURRENT_LOC_TYPE", length = 50)
	private String currentLocType;
	
	// 현재위치 ID
	@Column(name = "CURRENT_LOC_ID", nullable = true)
	private String currentLocId;
	
	// 상태 변경 일시
	@Column(name = "STATUS_CHANGE_DATE", nullable = true)
	private LocalDateTime statusChangeDate;
	
	// LOT 생성일시
	@Column(name = "CREATED_DATE")
	@CreatedDate
	private LocalDateTime createdDate;
	
	// =======================================================
	// 표준 이름 반환 메서드
	public String getDisplayName() {

	    // 완제품이면
	    if (product != null) {
	        return product.getPrdName();
	    }

	    // 원자재 LOT이면
	    if (material != null) {
	        return material.getMatName();
	    }

	    // 둘 다 없으면 코드라도
	    return prdId;
	}


}
