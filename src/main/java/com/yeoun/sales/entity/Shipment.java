package com.yeoun.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import com.yeoun.sales.enums.ShipmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "SHIPMENT")
public class Shipment {

    // 1) 출하ID (PK)
    @Id   
    @Column(name = "SHIPMENT_ID", nullable = false)
    @Comment("출하 이력을 구분하기 위한 고유 식별자 (SHP + YYYYMMDD + 4자리 Sequence)")
    private String shipmentId;

    // 2) 수주ID (ORDER FK)
    @Column(name = "ORDER_ID", nullable = false)
    @Comment("수주 마스터의 식별자 (FK)")
    private String orderId;

    // 3) 출하일자
    @Column(name = "SHIPMENT_DATE")
    @Comment("출하 처리 일자")
    private LocalDate shipmentDate;    

    // 6) 출하상태
    @Enumerated(EnumType.STRING)
    @Column(name = "SHIPMENT_STATUS")
    private ShipmentStatus shipmentStatus;


    // 7) 담당자ID (EMP FK)
    @Column(name = "EMP_ID", length = 20, nullable = false)
    @Comment("출하 처리 담당자 (EMP_ID)")
    private String empId;

    // 8) 고객사명
    @Column(name = "CLIENT_NAME", length = 200)
    @Comment("조회 편의용 캐싱된 거래처명 정보")
    private String clientName;

    // 9) 운송장번호
    @Column(name = "TRACKING_NUMBER", length = 50)
    @Comment("택배사 운송장번호")
    private String trackingNumber;

    // 10) 출하 관련 메모
    @Column(name = "MEMO", length = 2000)
    @Comment("출하 관련 요청사항 및 기록")
    private String memo;

    // 11) 등록일시
    @Column(name = "CREATED_AT")
    @Comment("데이터 생성 일시")
    private LocalDateTime createdAt;

    // 12) 수정일시
    @Column(name = "UPDATED_AT")
    @Comment("데이터 수정 일시")
    private LocalDateTime updatedAt;

    // 13) 거래처ID (CLIENT FK)
    @Column(name = "CLIENT_ID", length = 30, nullable = false)
    @Comment("수주 대상 거래처 ID")
    private String clientId;



    // =============================
    // 기본값 처리
    // =============================

		@PrePersist
		public void prePersist() {
		    // 기본 상태값
		    if (this.shipmentStatus == null) 
		        this.shipmentStatus = ShipmentStatus.WAITING;
		
		    if (this.shipmentDate == null) 
		        this.shipmentDate = LocalDate.now();
		
		    if (this.createdAt == null) 
		        this.createdAt = LocalDateTime.now();
		}
		
		@PreUpdate
		public void preUpdate() {
		    this.updatedAt = LocalDateTime.now();
		}
		
		// 상태 변경 편의 메서드
		public void changeStatus(ShipmentStatus status) {
		    this.shipmentStatus = status;
		}
}
