package com.yeoun.lot.entity;

import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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

@Entity
@Table(name = "LOT_RELATIONSHIP")
@SequenceGenerator(
		name = "LOT_RELATIONSHIP_SEQ_GENERATOR",
		sequenceName = "LOT_RELATIONSHIP_SEQ",
		initialValue = 1,
		allocationSize = 1
		)
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LotRelationship {
	
	// 관계 ID
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "LOT_RELATIONSHIP_SEQ_GENERATOR")
	@Column(name = "REL_ID")
	private Long relId;
	
	// 부모 LOT 번호 (완제품 / 상위 LOT)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OUTPUT_LOT_NO", nullable = false)
    private LotMaster outputLot;

    // 자식 LOT 번호 (원자재 / 하위 LOT)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INPUT_LOT_NO", nullable = false)
    private LotMaster inputLot;

    // 사용수량
    @Column(name = "USED_QTY")
    private Integer usedQty;

    // 등록일시
    @CreatedDate
    @Column(name = "CREATED_DATE", nullable = false)
    private LocalDate createdDate;
    
}
