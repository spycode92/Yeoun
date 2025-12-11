package com.yeoun.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ORDERS")
public class Orders {

    // 1) 수주ID
    @Id
    @Column(name = "ORDER_ID", length = 30, nullable = false)
    @Comment("수주(주문) 건을 식별하기 위한 고유 ID (ORD + YYYYMMDD + 순번)")
    private String orderId;

    // 2) 거래처ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENT_ID")
    private Client client;

    // 3) 담당자ID (내부 직원)
    @Column(name = "EMP_ID", length = 30, nullable = false)
    @Comment("수주를 등록하는 내부 담당자 ID")
    private String empId;

    // 4) 주문번호(외부)
    @Column(name = "ORDER_NUM", length = 30)
    @Comment("외부 시스템에서 전달되는 주문번호")
    private String orderNum;

    // 5) 수주일자
    @Column(name = "ORDER_DATE")
    @Comment("수주가 시스템에 등록된 일자")
    private LocalDate orderDate;

    // 6) 납기일자
    @Column(name = "DELIVERY_DATE")
    @Comment("고객이 요청한 납기일자")
    private LocalDate deliveryDate;

    // 7) 거래처 담당자명
    @Column(name = "MANAGER_NAME", length = 100)
    @Comment("주문 요청한 거래처의 담당자 이름")
    private String managerName;

    // 8) 거래처연락처
    @Column(name = "MANAGER_TEL", length = 50)
    @Comment("거래처 담당자 연락처")
    private String managerTel;

    // 9) 거래처이메일
    @Column(name = "MANAGER_EMAIL", length = 100)
    @Comment("거래처 담당자 이메일")
    private String managerEmail;

    // 10) 수주상태
    @Column(name = "ORDER_STATUS", length = 20, nullable = false)
    @Comment("수주 진행 상태 (요청/확정/취소/완료 등)")
    private String orderStatus;

    // 11) 배송지 우편번호
    @Column(name = "POSTCODE", length = 20)
    @Comment("배송지 우편번호")
    private String postcode;

    // 12) 배송지 주소
    @Column(name = "ADDR", length = 255)
    @Comment("배송지 기본 주소")
    private String addr;

    // 13) 배송지 상세주소
    @Column(name = "ADDR_DETAIL", length = 2000)
    @Comment("배송지 상세 주소")
    private String addrDetail;

    // 14) 고객 메모
    @Column(name = "ORDER_MEMO", length = 2000)
    @Comment("납품 관련 요청사항 및 고객 요청사항 기록")
    private String orderMemo;

    // 15) 등록일시
    @Column(name = "CREATED_AT")
    @Comment("최초 등록된 일시")
    private LocalDateTime createdAt;

    // 16) 등록자
    @Column(name = "CREATED_BY", length = 30)
    @Comment("최초 등록 사용자(EMP)")
    private String createdBy;
    


    /* ============================================
       기본값 자동 설정
    ============================================ */
    @PrePersist
    public void prePersist() {
        if (this.orderStatus == null) this.orderStatus = "REQUEST";  // 기본 상태
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.orderDate == null) this.orderDate = LocalDate.now();
    }
    
    // -----------------------------
    // 수주 상태값 변경
    public void changeStatus(String orderStatus) {
    	this.orderStatus = orderStatus;
    }
}
