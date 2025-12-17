package com.yeoun.order.entity;

import java.time.LocalDateTime;

import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeoun.equipment.entity.ProdLine;

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

@Entity
@Table(name = "WORK_SCHEDULE")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkSchedule {
	
	// 스케줄 고유 시퀀스
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SCHEDULE_SEQ")
    @SequenceGenerator(
      name = "SCHEDULE_SEQ",
      sequenceName = "SCHEDULE_SEQ",
      allocationSize = 1
    )
	private Long scheduleId;
	
	// 작업지시 번호
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "WORK_ID", nullable = false)
	private WorkOrder work;
    
    // 라인 번호
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LINE_ID", nullable = false)
    private ProdLine line;
    
    // 생산 시작 예정시간
    @Column(nullable = false)
    private LocalDateTime startDate;
    
    // 생산 완료 예정시간
    @Column(nullable = false)
    private LocalDateTime endDate;

    // 일정표 표시 색상
    @Column(nullable = false)
    private String colorCode;
    
    // 비고
    @Column
    private String remark;
    
}





