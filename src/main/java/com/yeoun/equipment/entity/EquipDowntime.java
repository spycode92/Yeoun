package com.yeoun.equipment.entity;

import java.time.LocalDateTime;

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
import lombok.Setter;

@Entity
@Table(name = "EQUIP_DOWNTIME")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class EquipDowntime {
	
	// 비가동 설비 고유 시퀀스
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EQUIP_DOWN_SEQ")
    @SequenceGenerator(
      name = "EQUIP_DOWN_SEQ",
      sequenceName = "EQUIP_DOWN_SEQ",
      allocationSize = 1
    )
	private Long downId;
    
    // 비가동 설비
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "EQUIP_ID", nullable = false)
    private ProdEquip equipment;
    
    // 비가동 사유
    @Column(nullable = false)
    private String downReason;
    
    // 비가동 시작 시간
    @Column(nullable = false)
    private LocalDateTime startTime;
    
    // 비가동 종료 시간
    @Column
    private LocalDateTime endTime;
    
    // 비고 및 메모
    @Column
    private String remark;

}





