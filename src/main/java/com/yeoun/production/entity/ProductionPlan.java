package com.yeoun.production.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeoun.emp.entity.Emp;
import com.yeoun.production.enums.ProductionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCTION_PLAN")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ProductionPlan {

    @Id
    @Column(name = "PLAN_ID", length = 20)
    @Comment("생산계획 고유식별자 (PLAN + YYYYMMDD + 시퀀스)")
    private String planId;

    @Column(name = "PRD_ID", length = 20, nullable = false)
    @Comment("제품 ID (PLAN은 제품 기준)")
    private String prdId;

    @Column(name = "PLAN_QTY", nullable = false)
    @Comment("총 생산 계획수량")
    private Integer planQty;

    @Column(name = "PLAN_DATE", nullable = false)
    @Comment("계획일자")
    private LocalDate planDate;

    @Column(name = "DUE_DATE", nullable = false)
    @Comment("납기일자")
    private LocalDate dueDate;    

    @Column(name = "PLAN_MEMO", length = 1000)
    @Comment("계획 메모")
    private String planMemo;

    @CreatedDate
    @Column(name = "CREATED_AT")
    @Comment("생성 시각")
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY")
    private String createdBy;

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "CREATED_BY",
        referencedColumnName = "EMP_ID",
        insertable = false,
        updatable = false
    )
    private Emp createdByEmp;


    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    @Comment("수정 시각")
    private LocalDateTime updatedAt;
    
    @Column(name = "UPDATED_BY" , length = 30)
    @Comment("수정자 ID")
    private String updatedBy;

    
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 20, nullable = false)
    private ProductionStatus status; 
    //PLANNING- 계획 단계 (생산 전)  IN_PROGRESS-  생산 진행중  DONE -생산완료

}
