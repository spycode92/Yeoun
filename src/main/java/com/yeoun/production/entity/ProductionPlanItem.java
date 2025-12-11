package com.yeoun.production.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeoun.masterData.entity.ProductMst;
import com.yeoun.production.enums.BomStatus;
import com.yeoun.production.enums.ProductionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCTION_PLAN_ITEM")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ProductionPlanItem {

    @Id
    @Column(name = "PLAN_ITEM_ID", length = 20, nullable=false)
    private String planItemId;

    @Column(name = "PLAN_ID", length = 20, nullable = false)
    private String planId;

    @Column(name = "ORDER_ITEM_ID", length = 30, nullable = false)
    @Comment("어떤 수주 상세에서 온 항목인지")
    private Long orderItemId;

    @Column(name = "PRD_ID", length = 20, nullable = false)
    private String prdId;

    @Column(name = "ORDER_QTY", precision = 10, scale = 2, nullable = false)
    @Comment("해당 수주 항목의 주문 수량")
    private BigDecimal orderQty;

    @Column(name = "PLAN_QTY", precision = 10, scale = 2, nullable = false)
    @Comment("실제 생산 계획 수량 (기본은 ORDER_QTY와 동일)")
    private BigDecimal planQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "BOM_STATUS", nullable = false, length = 10)
    private BomStatus bomStatus = BomStatus.WAIT;

    @Column(name = "PARTIAL_QTY")
    @Comment("부분 생산 가능 수량(BOM_STATUS가 PARTIAL일 때만 값)")
    private Double partialQty;

    @Column(name = "ITEM_MEMO", length = 300)
    private String itemMemo;

    @CreatedDate
    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY", length = 30)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 20, nullable = false)
    private ProductionStatus status;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRD_ID", insertable = false, updatable = false)
    private ProductMst product;


}
