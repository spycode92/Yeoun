package com.yeoun.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import com.yeoun.masterData.entity.ProductMst;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ORDER_ITEM")
public class OrderItem {

    // 1) 수주 상세 ID (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_ITEM_ID", nullable = false)
    @Comment("수주 상세 데이터의 고유 식별 번호 (PK/자동증가)")
    private Long orderItemId;

    // 2) 수주 ID (FK → ORDER.ORDER_ID)
    @Column(name = "ORDER_ID", length = 30, nullable = false)
    @Comment("수주 마스터 정보의 수주 식별자 (FK)")
    private String orderId;

    // 3) 제품 ID (FK → PRODUCT_MST.PRD_ID)
    @Column(name = "PRD_ID", length = 30, nullable = false)
    @Comment("주문한 상품의 ID (PRODUCT 테이블 참조)")
    private String prdId;

    // 4) 주문수량
    @Column(name = "ORDER_QTY", precision = 18, scale = 2, nullable = false)
    @Comment("주문한 제품 수량")
    private BigDecimal orderQty;

    // 5) 단가
    @Column(name = "UNIT_PRICE", precision = 18, scale = 2, nullable = false)
    @Comment("제품 1개당 가격 (주문 시점 가격 적용)")
    private BigDecimal unitPrice;

    // 6) 총 금액 (단가 × 수량)
    @Column(name = "TOTAL_PRICE", precision = 18, scale = 2, nullable = false)
    @Comment("수주 금액 (단가 × 주문수량)")
    private BigDecimal totalPrice;

    // 7) 상세 상태
    @Column(name = "ITEM_STATUS", length = 50)
    @Comment("품목 관련 요청사항 또는 상태 정보 (예: 주문, 준비중, 출하완료 등)")
    private String itemStatus;

    // 8) 상세 메모
    @Column(name = "ITEM_MEMO", length = 2000)
    @Comment("품목 관련 요청사항 또는 메모 (옵션, 요구사항 등)")
    private String itemMemo;

    // 9) 등록일시
    @Column(name = "CREATED_AT")
    @Comment("데이터 생성 일시")
    private LocalDateTime createdAt;

    // 10) 수정일시
    @Column(name = "UPDATED_AT")
    @Comment("데이터 수정 일시")
    private LocalDateTime updatedAt;
    
    
    @ManyToOne
    @JoinColumn(name="ORDER_ID", insertable=false, updatable=false)
    private Orders order;

    @ManyToOne
    @JoinColumn(name="PRD_ID", insertable=false, updatable=false)
    private ProductMst product;

    
 
    // =============================
    // 기본값 자동 처리
    // =============================
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();

        if (this.totalPrice == null && this.orderQty != null && this.unitPrice != null) {
            this.totalPrice = this.unitPrice.multiply(this.orderQty);
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();

        if (this.orderQty != null && this.unitPrice != null) {
            this.totalPrice = this.unitPrice.multiply(this.orderQty);
        }
    }
}
