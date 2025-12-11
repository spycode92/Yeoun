package com.yeoun.order.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeoun.masterData.entity.Equipment;
import com.yeoun.masterData.entity.ProdLine;

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
@Table(name = "PROD_EQUIP")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class ProdEquip {

	// 설비 고유 시퀀스
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EQUIP_SEQ")
    @SequenceGenerator(
      name = "EQUIP_SEQ",
      sequenceName = "EQUIP_SEQ",
      allocationSize = 1
    )
	private Long equipId;
    
    // 설비 코드 (기준정보 타입)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "EQUIP_CODE", nullable = false)
    private Equipment equipment;
    
    // 부를 수 있는 설비명
    @Column(nullable = false)
    private String equipName;
    
    // 설비 소속 라인 (기준정보)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LINE_ID", nullable = false)
    private ProdLine line;
    
    // 설비 상태	=> RUN/STOP/BREAKDOWN/MAINTENANCE
    @Column(nullable = false)
    private String status;
    
    // 등록 일시
    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdDate;
    
    // 수정 일시
    @Column
    private LocalDateTime updatedDate;
    
    // 비고
    @Column
    private String remark;
	
	
}
