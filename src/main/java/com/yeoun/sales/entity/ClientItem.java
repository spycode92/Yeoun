package com.yeoun.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "CLIENT_ITEM")
public class ClientItem {

    // 1) 공급품목ID(PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ITEM_ID", nullable = false)
    @Comment("공급품목 데이터를 구분하는 고유 ID (PK)")
    private Long itemId;

    // 2) 거래처ID (FK)
    @Column(name = "CLIENT_ID", length = 20, nullable = false)
    @Comment("해당 품목을 공급하는 거래처 ID")
    private String clientId;

    // 3) 자재ID (FK)
    @Column(name = "MATERIAL_ID", length = 30, nullable = false)
    @Comment("공급 품목(자재) 식별 코드 (MATERIAL.MATERIAL_ID)")
    private String materialId;

    // 4) 공급 단가
    @Column(name = "UNIT_PRICE", precision = 18, scale = 2, nullable = false)
    @Comment("거래처로부터 구매 시 사용하는 단가")
    private BigDecimal unitPrice;

    // 5) 리드타임(일)
    @Column(name = "LEAD_DAYS", precision = 10, scale = 0, nullable = false)
    @Comment("발주 후 공급까지 걸리는 소요일수")
    private BigDecimal leadDays;

    // 6) 공급 단위
    @Column(name = "UNIT", length = 20, nullable = false)
    @Comment("해당 거래처로부터 구매하는 단위 (예: kg, PACK 등)")
    private String unit;

    // 7) 가용여부
    @Column(name = "SUPPLY_AVAILABLE", length = 1, nullable = false)
    @Comment("공급 가능 여부 (Y/N)")
    private String supplyAvailable;

    // 8) 최소발주수량(MOQ)
    @Column(name = "MIN_ORDER_QTY", precision = 18, scale = 4)
    @Comment("최소 발주 수량 (거래처 요구 MOQ)")
    private BigDecimal minOrderQty;

    // 9) 발주단위수량
    @Column(name = "ORDER_UNIT", precision = 18, scale = 4)
    @Comment("발주단위 수량 (최소수량 배수 적용)")
    private BigDecimal orderUnit;    

    // 10) 등록일시
    @Column(name = "CREATED_AT")
    @Comment("공급품목 최초 등록 일시")
    private LocalDateTime createdAt;

    // 11) 등록자 ID (EMP FK)
    @Column(name = "CREATED_BY", length = 20)
    @Comment("공급품목 최초 등록자 ID")
    private String createdBy;

    // 12) 수정일시
    @Column(name = "UPDATED_AT")
    @Comment("공급품목 최종 수정 일시")
    private LocalDateTime updatedAt;

    // 13) 수정자 ID (EMP FK)
    @Column(name = "UPDATED_BY", length = 20)
    @Comment("공급품목 최종 수정자 ID")
    private String updatedBy;


    // =============================
    // 기본값 처리
    // =============================
    @PrePersist
    public void prePersist() {
        if (this.supplyAvailable == null) this.supplyAvailable = "Y";  // DEFAULT Y
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
