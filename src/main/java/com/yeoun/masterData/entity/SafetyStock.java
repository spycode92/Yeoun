package com.yeoun.masterData.entity;


import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "SAFETY_STOCK")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class) 
public class SafetyStock {

	@Id
	@Column(name="ITEM_ID", length = 50)
	private String itemId; // 품목코드(완제품/원재료)
	
	@Column(name="ITEM_TYPE", length = 100)
	private String itemType; //품목코드의 종류를 구분
			
	@Column(name="ITEM_NAME", length = 50)
	private String itemName; //품목명
	
	@Column(name="VOLUME", length = 20)
	private Long volume; //용량
	
	@Column(name="ITEM_UNIT")
	private String itemUnit; //단위
	
	@Column(name="POLICY_TYPE", length = 255)
	private String policyType; //정책방식
	
	@Column(name="POLICY_DAYS")
	private Long policyDays; //정책일수
	
	@Column(name="SAFETY_STOCK_QTY_DAILY")
	private Long safetyStockQtyDaily; //일별안전재고 수량
	
	@Column(name="SAFETY_STOCK_QTY")
	private Long safetyStockQty; //안전재고 수량
	
	@Column(name="STATUS")
	private String status; //상태
	
	@Column(name="REMARK")
	private String remark; //비고

}
