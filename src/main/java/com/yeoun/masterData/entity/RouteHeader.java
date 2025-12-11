package com.yeoun.masterData.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ROUTE_HEADER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class RouteHeader implements Serializable{
	
	// 라우트ID
	@Id
	@Column(name = "ROUTE_ID", length = 20)
	private String routeId;
	
	// 제품코드
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PRD_ID", nullable = false)
	private ProductMst product;
	
	// 라우트명
	@Column(name = "ROUTE_NAME", length = 100, nullable = false)
	private String routeName;
	
	// 라우트 설명
	@Column(name = "DESCRIPTION", length = 400)
	private String description;
	
	// 사용 여부
	@Column(name = "USE_YN", length = 1, nullable = false)
	private String useYn;
	
	// 최초 등록자
	@Column(name = "CREATED_ID", length = 7, nullable = false)
	private String createdId;
	
	// 최초 등록일
	@CreatedDate
	@Column(name = "CREATED_DATE")
	private LocalDateTime createdDate;
	
	// 수정자
	@Column(name = "UPDATED_ID", length = 7)
	private String updatedId;
	
	// 수정일
	@Column(name = "UPDATED_DATE")
	private LocalDateTime updatedDate;

}
