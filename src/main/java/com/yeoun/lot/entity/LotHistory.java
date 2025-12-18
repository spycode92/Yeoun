package com.yeoun.lot.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeoun.emp.entity.Emp;
import com.yeoun.masterData.entity.ProcessMst;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * LOT 이력
 * - LOT의 모든 공정 및 상태 변경 이력 관리 (시간순 누적)
 */
@Entity
@Table(name = "LOT_HISTORY")
@SequenceGenerator(
		name = "LOT_HISTORY_SEQ_GENERATOR",
		sequenceName = "LOT_HISTORY_SEQ",
		initialValue = 1,
		allocationSize = 1
		)
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LotHistory {
	
	// 이력ID
	@Id 
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "LOT_HISTORY_SEQ_GENERATOR")
	@Column(name = "HIST_ID", nullable = false)
	private Long histId;
	
	// LOT 번호
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LOT_NO", nullable = false)
    private LotMaster lot;  
	
	// 작업지시번호
	@Column(name = "ORDER_ID", length = 16)
	private String orderId;
	
	// 공정코드
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROCESS_ID")
    private ProcessMst process;
	
	// 이벤트 유형
	@Column(name = "EVENT_TYPE", length = 20)
    private String eventType;
	
	// 상태
	@Column(name = "STATUS", length = 50)
    private String status;
	
	// 위치 유형
	@Column(name = "LOCATION_TYPE", length = 50)
    private String locationType;
	
	// 위치 ID
	@Column(name = "LOCATION_ID")
    private String locationId;
	
	// 전체수량
	@Column(name = "QUANTITY")
    private Integer quantity;
	
	// 양품수량
	@Column(name = "GOOD_QTY")
	private Integer goodQty;
	
	// 불량수량
	@Column(name = "DEFECT_QTY")
    private Integer defectQty;
	
	// 시작시간
	@Column(name = "START_TIME")
    private LocalDateTime startTime;
	
	// 종료시간
	@Column(name = "END_TIME")
    private LocalDateTime endTime;
	
	// 작업자
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WORKER_ID")
    private Emp worker;
	
	// 등록일시
	@CreatedDate
    @Column(name = "CREATED_DATE", nullable = false)
    private LocalDateTime createdDate;

}
