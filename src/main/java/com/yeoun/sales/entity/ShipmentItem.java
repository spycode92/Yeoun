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
@Table(name = "SHIPMENT_ITEM")
public class ShipmentItem {

    // 1) 출하 상세 ID (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SHIPMENT_ITEM_ID", nullable = false)
    @Comment("출하 상세 PK (자동 번호)")
    private Long shipmentItemId;

    // 2) 출하 ID (FK → SHIPMENT.SHIPMENT_ID)
    @Column(name = "SHIPMENT_ID", length = 30, nullable = false)
    @Comment("출하 헤더 ID")
    private String shipmentId;

    // 3) 제품 ID
    @Column(name = "PRD_ID", length = 30, nullable = false)
    @Comment("해당 출하에 포함된 제품 ID")
    private String prdId;

    // 4) 출하 수량
    @Column(name = "LOT_QTY", precision = 18, scale = 2, nullable = false)
    @Comment("제품 출하 수량")
    private BigDecimal lotQty;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
